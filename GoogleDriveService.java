package com.securefileshare.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class GoogleDriveService {
    
    private static GoogleDriveService instance;
    private Drive driveService;
    private static final long serialVersionUID = 1L;
    
    private static final String APPLICATION_NAME = "Secure File Sharing System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
        DriveScopes.DRIVE_FILE,
        DriveScopes.DRIVE_READONLY
    );
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    
    private boolean isInitialized = false;
    private String statusMessage = "Not initialized";
    
    private GoogleDriveService() {
        initialize();
    }
    
    public static synchronized GoogleDriveService getInstance() {
        if (instance == null) {
            instance = new GoogleDriveService();
        }
        return instance;
    }
    
    private synchronized void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            System.out.println("=== INITIALIZING GOOGLE DRIVE SERVICE ===");
            
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            
            System.out.println("Loading OAuth credentials...");
            Credential credential = authorize(HTTP_TRANSPORT);
            
            driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            
            testConnection();
            
            isInitialized = true;
            statusMessage = "Connected to Google Drive";
            System.out.println("✓ Google Drive service initialized successfully");
            
        } catch (FileNotFoundException e) {
            statusMessage = "Credentials file not found. Please add credentials.json to src/main/resources/";
            System.err.println("✗ GOOGLE DRIVE INITIALIZATION FAILED: " + statusMessage);
            System.err.println("To fix this, place your credentials.json file in: src/main/resources/");
            e.printStackTrace();
        } catch (Exception e) {
            statusMessage = "Google Drive initialization failed: " + e.getMessage();
            System.err.println("✗ GOOGLE DRIVE INITIALIZATION FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Credential authorize(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Try multiple possible locations for credentials.json
        InputStream in = null;
        String[] possiblePaths = {
            "/credentials.json",                    // Classpath root
            "credentials.json",                      // Relative path
            "/WEB-INF/classes/credentials.json",    // WEB-INF classes
            "WEB-INF/classes/credentials.json"       // Relative to WEB-INF
        };
        
        System.out.println("🔍 Searching for credentials.json...");
        for (String path : possiblePaths) {
            System.out.println("  - Trying classpath: " + path);
            in = GoogleDriveService.class.getResourceAsStream(path);
            if (in != null) {
                System.out.println("✅ Found credentials at classpath: " + path);
                break;
            }
        }
        
        // Try file system path as last resort
        if (in == null) {
            System.out.println("📁 Trying file system paths...");
            
            // Get the current working directory
            String userDir = System.getProperty("user.dir");
            System.out.println("📂 Current working directory: " + userDir);
            
            // Try src/main/resources/credentials.json (Maven style)
            java.io.File file1 = new java.io.File("src/main/resources/credentials.json");
            if (file1.exists()) {
                System.out.println("✅ Found credentials at: " + file1.getAbsolutePath());
                in = new FileInputStream(file1);
            } else {
                // Try src/main/webapp/WEB-INF/classes/credentials.json (your structure)
                java.io.File file2 = new java.io.File("src/main/webapp/WEB-INF/classes/credentials.json");
                if (file2.exists()) {
                    System.out.println("✅ Found credentials at: " + file2.getAbsolutePath());
                    in = new FileInputStream(file2);
                } else {
                    // Try webapp/WEB-INF/classes/credentials.json (alternative)
                    java.io.File file3 = new java.io.File("webapp/WEB-INF/classes/credentials.json");
                    if (file3.exists()) {
                        System.out.println("✅ Found credentials at: " + file3.getAbsolutePath());
                        in = new FileInputStream(file3);
                    } else {
                        // Try project root
                        java.io.File file4 = new java.io.File("credentials.json");
                        if (file4.exists()) {
                            System.out.println("✅ Found credentials at: " + file4.getAbsolutePath());
                            in = new FileInputStream(file4);
                        } else {
                            // Try target/classes (Maven build output)
                            java.io.File file5 = new java.io.File("target/classes/credentials.json");
                            if (file5.exists()) {
                                System.out.println("✅ Found credentials at: " + file5.getAbsolutePath());
                                in = new FileInputStream(file5);
                            } else {
                                // Try build/classes (Eclipse default output)
                                java.io.File file6 = new java.io.File("build/classes/credentials.json");
                                if (file6.exists()) {
                                    System.out.println("✅ Found credentials at: " + file6.getAbsolutePath());
                                    in = new FileInputStream(file6);
                                } else {
                                    throw new FileNotFoundException(
                                        "❌ Credentials file not found in any of the expected locations.\n" +
                                        "Please place credentials.json in one of these locations:\n" +
                                        "  - src/main/resources/credentials.json\n" +
                                        "  - src/main/webapp/WEB-INF/classes/credentials.json\n" +
                                        "  - Project root directory\n" +
                                        "Current working directory: " + userDir
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        
        // Create tokens directory if it doesn't exist
        java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
        if (!tokensDir.exists()) {
            System.out.println("📁 Creating tokens directory: " + tokensDir.getAbsolutePath());
            tokensDir.mkdirs();
        }
        
        // ⭐ IMPORTANT CHANGE: Commented out token storage for testing ⭐
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();
        
        System.out.println("🌐 Opening browser for OAuth authorization...");
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
    private void testConnection() throws IOException {
        System.out.println("🔌 Testing Google Drive connection...");
        
        com.google.api.services.drive.model.About about = driveService.about().get()
                .setFields("user, storageQuota")
                .execute();
        
        String userEmail = about.getUser().getEmailAddress();
        System.out.println("✓ Connected as: " + userEmail);
        
        // Display storage quota if available
        if (about.getStorageQuota() != null) {
            Long limit = about.getStorageQuota().getLimit();
            Long usage = about.getStorageQuota().getUsage();
            if (limit != null && usage != null) {
                double limitGB = limit / (1024.0 * 1024.0 * 1024.0);
                double usageGB = usage / (1024.0 * 1024.0 * 1024.0);
                System.out.printf("💾 Storage: %.2f GB used of %.2f GB%n", usageGB, limitGB);
            }
        }
    }
    
    /**
     * Check if Google Drive service is available
     */
    public boolean isServiceAvailable() {
        if (!isInitialized || driveService == null) {
            return false;
        }
        
        // Try a quick test to verify connection is still alive
        try {
            driveService.about().get().setFields("user").execute();
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ Google Drive connection lost: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get detailed status message for UI
     */
    public String getStatusMessage() {
        return statusMessage;
    }
    
    /**
     * Get the authenticated Drive service instance
     */
    public Drive getDriveService() {
        return driveService;
    }
    
    /**
     * Force reset tokens by deleting the tokens directory
     */
    public void resetTokens() {
        System.out.println("🔄 Attempting to reset Google Drive tokens...");
        try {
            java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
            if (tokensDir.exists()) {
                deleteDirectory(tokensDir);
                System.out.println("✅ Tokens directory deleted successfully");
            } else {
                System.out.println("ℹ️ No tokens directory found");
            }
            
            // Reset initialization flag to force re-authorization
            isInitialized = false;
            statusMessage = "Tokens reset - will re-authorize on next access";
            
        } catch (Exception e) {
            System.err.println("✗ Failed to reset tokens: " + e.getMessage());
        }
    }
    
    /**
     * Helper to delete directory recursively
     */
    private void deleteDirectory(java.io.File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("  Deleted file: " + file.getName());
                    }
                }
            }
        }
        boolean dirDeleted = dir.delete();
        if (dirDeleted) {
            System.out.println("  Deleted directory: " + dir.getName());
        }
    }
    
    /**
     * Get the absolute path of tokens directory
     */
    public String getTokensPath() {
        java.io.File tokensDir = new java.io.File(TOKENS_DIRECTORY_PATH);
        return tokensDir.getAbsolutePath();
    }
    
    /**
     * Upload a file to Google Drive
     */
    public UploadResult uploadFile(byte[] fileData, String originalFilename, int userId, 
                                  String description, boolean encrypted, 
                                  String encryptionKey, String iv) throws IOException {
        
        System.out.println("\n=== GOOGLE DRIVE FILE UPLOAD ===");
        System.out.println("File: " + originalFilename);
        System.out.println("Size: " + fileData.length + " bytes");
        System.out.println("User ID: " + userId);
        System.out.println("Encrypted: " + encrypted);
        
        if (!isServiceAvailable()) {
            throw new IOException("Google Drive service is not available. " + statusMessage);
        }
        
        java.io.File tempFile = null;
        
        try {
            // Create temporary file
            tempFile = java.io.File.createTempFile("gdrive_", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileData);
            }
            
            // Prepare file metadata
            File fileMetadata = new File();
            fileMetadata.setName(originalFilename);
            String fullDescription = description != null ? description : "Uploaded via Secure File Sharing System";
            if (encrypted) {
                fullDescription += " [AES-256 Encrypted]";
            }
            fileMetadata.setDescription(fullDescription);
            
            // Upload file
            FileContent mediaContent = new FileContent("application/octet-stream", tempFile);
            File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, size, webViewLink, createdTime")
                    .execute();
            
            System.out.println("✓ Upload successful!");
            System.out.println("File ID: " + uploadedFile.getId());
            System.out.println("View Link: " + uploadedFile.getWebViewLink());
            
            // Prepare result
            UploadResult result = new UploadResult();
            result.setSuccess(true);
            result.setCloudFileId(uploadedFile.getId());
            result.setWebdavPath("/gdrive/" + userId + "/" + originalFilename);
            result.setFilename(originalFilename);
            result.setOriginalFilename(originalFilename);
            result.setShareableUrl(uploadedFile.getWebViewLink());
            result.setEncrypted(encrypted);
            result.setUploadTime(new Date());
            
            return result;
            
        } catch (Exception e) {
            System.err.println("✗ GOOGLE DRIVE UPLOAD FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to upload file to Google Drive: " + e.getMessage(), e);
            
        } finally {
            // Clean up temp file
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    /**
     * Simplified upload method (without encryption details)
     */
    public UploadResult uploadFile(byte[] fileData, String originalFilename, int userId, 
                                  String description, boolean encrypted) throws IOException {
        return uploadFile(fileData, originalFilename, userId, description, encrypted, null, null);
    }
    
    /**
     * Download a file from Google Drive
     */
    public byte[] downloadFile(String fileId) throws IOException {
        System.out.println("\n=== GOOGLE DRIVE FILE DOWNLOAD ===");
        System.out.println("File ID: " + fileId);
        
        if (!isServiceAvailable()) {
            throw new IOException("Google Drive service is not available");
        }
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            
            byte[] fileData = outputStream.toByteArray();
            System.out.println("✓ Download successful: " + fileData.length + " bytes");
            
            return fileData;
            
        } catch (Exception e) {
            System.err.println("✗ DOWNLOAD FAILED: " + e.getMessage());
            throw new IOException("Failed to download file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete a file from Google Drive
     */
    public boolean deleteFile(String fileId) {
        System.out.println("\n=== GOOGLE DRIVE FILE DELETE ===");
        System.out.println("File ID: " + fileId);
        
        if (!isServiceAvailable()) {
            System.err.println("Google Drive service not available");
            return false;
        }
        
        try {
            driveService.files().delete(fileId).execute();
            System.out.println("✓ File deleted successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ DELETE FAILED: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * List files in Google Drive (for debugging)
     */
    public void listFiles() throws IOException {
        if (!isServiceAvailable()) {
            System.out.println("Google Drive service not available");
            return;
        }
        
        FileList result = driveService.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name, size, createdTime)")
                .execute();
        
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("📁 Files in Google Drive:");
            for (File file : files) {
                System.out.printf("  - %s (%s) %d bytes%n", 
                    file.getName(), file.getId(), file.getSize() != null ? file.getSize() : 0);
            }
        }
    }
    
    /**
     * Inner class for upload results
     */
    public static class UploadResult {
        private boolean success;
        private String cloudFileId;
        private String webdavPath;
        private String filename;
        private String originalFilename;
        private boolean encrypted;
        private String shareableUrl;
        private Date uploadTime;
        private String errorMessage;
        
        // Getters and Setters
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
        
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        
        public String getShareableUrl() { return shareableUrl; }
        public void setShareableUrl(String shareableUrl) { this.shareableUrl = shareableUrl; }
        
        public Date getUploadTime() { return uploadTime; }
        public void setUploadTime(Date uploadTime) { this.uploadTime = uploadTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}