package com.securefileshare.services;

import java.io.*;
import com.google.api.services.drive.Drive;

public class CloudStorageService {
    
    private static CloudStorageService instance;
    private GoogleDriveService googleDriveService;
    
    private CloudStorageService() {
        System.out.println("=== INITIALIZING CLOUD STORAGE SERVICE ===");
        
        try {
            googleDriveService = GoogleDriveService.getInstance();
            
            if (googleDriveService.isServiceAvailable()) {
                System.out.println("✓ Using Google Drive as cloud storage backend");
            } else {
                System.err.println("✗ Google Drive service is not available");
            }
            
        } catch (Exception e) {
            System.err.println("✗ CLOUD STORAGE INITIALIZATION WARNING!");
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    public static synchronized CloudStorageService getInstance() {
        if (instance == null) {
            instance = new CloudStorageService();
        }
        return instance;
    }
    
    public boolean isServiceAvailable() {
        return googleDriveService != null && googleDriveService.isServiceAvailable();
    }
    
    public String testConnection() {
        StringBuilder result = new StringBuilder();
        result.append("=== CLOUD STORAGE CONNECTION TEST ===\n\n");
        
        if (googleDriveService == null) {
            result.append("✗ Google Drive service not initialized\n");
        } else if (googleDriveService.isServiceAvailable()) {
            result.append("✓ Google Drive: AVAILABLE\n");
            result.append("\nService Details:\n");
            result.append("- Storage Provider: Google Drive\n");
            result.append("- Status: Operational\n");
            result.append("- Authentication: OAuth 2.0\n");
            result.append("- Maximum File Size: 5TB\n");
            result.append("- Free Storage: 15GB\n");
        } else {
            result.append("✗ Google Drive: UNAVAILABLE\n");
            result.append("\nTroubleshooting:\n");
            result.append("1. Check internet connection\n");
            result.append("2. Verify credentials.json exists in src/main/resources/\n");
            result.append("3. Ensure Google Drive API is enabled in Google Cloud Console\n");
            result.append("4. Check OAuth consent screen configuration\n");
        }
        
        return result.toString();
    }
    
    private Drive getDriveService() throws Exception {
        if (googleDriveService == null) {
            throw new Exception("Google Drive service not initialized");
        }
        return googleDriveService.getDriveService();
    }
    
    public UploadResult uploadFileWithEncryption(byte[] fileData, String filename, String folder,
                                                int userId, boolean encrypt, String encryptionPassword,
                                                EncryptionService.EncryptionResult encryptionResult) {
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== CLOUD STORAGE UPLOAD WITH ENCRYPTION ===");
        System.out.println("User ID: " + userId);
        System.out.println("Filename: " + filename);
        System.out.println("Encryption: " + (encrypt ? "ENABLED" : "DISABLED"));
        System.out.println("File Size: " + fileData.length + " bytes");
        System.out.println("Google Drive Available: " + (googleDriveService != null && googleDriveService.isServiceAvailable()));
        System.out.println("=".repeat(70));
        
        UploadResult result = new UploadResult();
        
        try {
            if (googleDriveService == null || !googleDriveService.isServiceAvailable()) {
                throw new IOException("Google Drive service is not available");
            }
            
            byte[] dataToUpload = fileData;
            
            if (encrypt && encryptionResult != null) {
                dataToUpload = encryptionResult.getEncryptedData();
                System.out.println("✓ File encrypted");
                System.out.println("Original size: " + fileData.length + " bytes");
                System.out.println("Encrypted size: " + dataToUpload.length + " bytes");
            }
            
            GoogleDriveService.UploadResult gdriveResult;
            
            if (encrypt && encryptionResult != null) {
                gdriveResult = googleDriveService.uploadFile(
                    dataToUpload, filename, userId, 
                    "Encrypted: " + encrypt + " | Password protected",
                    encrypt, 
                    encryptionResult.getEncryptionKey(), 
                    encryptionResult.getIv()
                );
            } else {
                gdriveResult = googleDriveService.uploadFile(
                    dataToUpload, filename, userId, 
                    "Uploaded via Secure File Sharing System",
                    encrypt
                );
            }
            
            result.setSuccess(gdriveResult.isSuccess());
            result.setCloudFileId(gdriveResult.getCloudFileId());
            result.setWebdavPath(gdriveResult.getWebdavPath());
            result.setOriginalSize(fileData.length);
            result.setFilename(gdriveResult.getFilename());
            result.setOriginalFilename(filename);
            result.setShareableUrl(gdriveResult.getShareableUrl());
            result.setEncrypted(encrypt);
            
            if (encrypt && encryptionResult != null) {
                result.setEncryptionInfo("key:" + encryptionResult.getEncryptionKey() + ":iv:" + encryptionResult.getIv());
            }
            
            if (!gdriveResult.isSuccess()) {
                result.setErrorMessage(gdriveResult.getErrorMessage());
            }
            
            System.out.println("✓ Upload successful!");
            System.out.println("File ID: " + gdriveResult.getCloudFileId());
            System.out.println("Shareable URL: " + gdriveResult.getShareableUrl());
            
        } catch (Exception e) {
            System.err.println("✗ UPLOAD FAILED!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            
            result.setSuccess(false);
            result.setErrorMessage("Google Drive upload failed: " + e.getMessage());
        }
        
        return result;
    }
    
    public byte[] downloadFile(String fileId) throws Exception {
        System.out.println("\n=== DOWNLOADING FROM GOOGLE DRIVE ===");
        System.out.println("File ID: " + fileId);
        
        if (!isServiceAvailable()) {
            throw new Exception("Google Drive service is not available");
        }
        
        try {
            Drive driveService = getDriveService();
            
            Drive.Files.Get getRequest = driveService.files().get(fileId);
            getRequest.setFields("id, name, size, mimeType");
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                getRequest.executeMediaAndDownloadTo(outputStream);
                byte[] fileData = outputStream.toByteArray();
                
                System.out.println("✓ Download successful!");
                System.out.println("File size: " + fileData.length + " bytes");
                System.out.println("File ID: " + fileId);
                
                return fileData;
            }
            
        } catch (Exception e) {
            System.err.println("✗ Download failed: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Failed to download file from Google Drive: " + e.getMessage(), e);
        }
    }
    
    public boolean deleteFile(String fileId) {
        System.out.println("\n=== DELETING FROM GOOGLE DRIVE ===");
        System.out.println("File ID: " + fileId);
        
        if (!isServiceAvailable()) {
            System.err.println("Google Drive service is not available");
            return false;
        }
        
        try {
            Drive driveService = getDriveService();
            
            driveService.files().delete(fileId).execute();
            
            System.out.println("✓ File deleted successfully from Google Drive");
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Failed to delete from Google Drive: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static class UploadResult {
        private boolean success;
        private String cloudFileId;
        private String webdavPath;
        private String filename;
        private String originalFilename;
        private long originalSize;
        private boolean encrypted;
        private String encryptionInfo;
        private String shareableUrl;
        private String errorMessage;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getCloudFileId() { return cloudFileId; }
        public void setCloudFileId(String cloudFileId) { this.cloudFileId = cloudFileId; }
        
        public String getWebdavPath() { return webdavPath; }
        public void setWebdavPath(String webdavPath) { this.webdavPath = webdavPath; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
        
        public long getOriginalSize() { return originalSize; }
        public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }
        
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        
        public String getEncryptionInfo() { return encryptionInfo; }
        public void setEncryptionInfo(String encryptionInfo) { this.encryptionInfo = encryptionInfo; }
        
        public String getShareableUrl() { return shareableUrl; }
        public void setShareableUrl(String shareableUrl) { this.shareableUrl = shareableUrl; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}