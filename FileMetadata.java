package com.securefileshare.models;

import java.sql.Timestamp;

public class FileMetadata {
    private int fileId;
    private int userId;
    private String originalFilename;
    private String storedFilename;
    private String fileHash;
    private long fileSize;
    private String fileType;
    private String description;
    
    // CloudMe specific fields
    private String cloudFileId;
    private String cloudmeFileId;
    private String cloudmeAccount;
    private String cloudmePath;
    private String cloudmeFolder;
    private Timestamp cloudmeModifiedDate;
    private String cloudmeSyncStatus;
    private String webdavPath;
    private String cloudStorage;
    
    // Encryption fields
    private String encryptionKey;
    private String encryptionKeyHash;
    private String iv;
    private String ivBase64;
    private String saltBase64;
    private int keyIterations;
    private String encryptionAlgorithm;
    private boolean encrypted;
    
    // File properties
    private boolean sensitive;
    private Timestamp accessExpiry;
    private Timestamp uploadDate;
    private Timestamp lastDownloadDate;
    private int accessCount;
    private int downloadCount;
    private int maxDownloads;
    
    // Upload metadata
    private int retentionDays;
    private String tags;
    private String uploadMethod;
    private int chunkCount;
    private int chunkSize;
    private String uploadStatus;
    
    // File integrity
    private String sha256Hash;
    private String sha256EncryptedHash;
    
    // Compression
    private double compressionRatio;
    
    // Sharing
    private boolean shared;
    private String shareToken;
    private Timestamp shareExpiry;
    private String sharePasswordHash;
    private int shareMaxDownloads;
    
    // For backward compatibility - alias methods
    public String getCloudPath() {
        return cloudmePath != null ? cloudmePath : webdavPath;
    }
    
    public void setCloudPath(String cloudPath) {
        this.cloudmePath = cloudPath;
        this.webdavPath = cloudPath;
    }
    
    public String getProcessedHash() {
        return sha256EncryptedHash;
    }
    
    public void setProcessedHash(String processedHash) {
        this.sha256EncryptedHash = processedHash;
    }
    
    public String getStoragePath() {
        return webdavPath;
    }
    
    public void setStoragePath(String storagePath) {
        this.webdavPath = storagePath;
        this.cloudmePath = storagePath;
    }
    
    public String getSalt() {
        return saltBase64;
    }
    
    public void setSalt(String salt) {
        this.saltBase64 = salt;
    }
    
    public long getProcessedSize() {
        return fileSize;
    }
    
    public void setProcessedSize(long processedSize) {
        // Not storing separately
    }
    
    public String getHashAlgorithm() {
        return "SHA-256";
    }
    
    public void setHashAlgorithm(String hashAlgorithm) {
        // Default SHA-256
    }
    
    // Getters and Setters
    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCloudFileId() { return cloudFileId; }
    public void setCloudFileId(String cloudFileId) { this.cloudFileId = cloudFileId; }
    
    public String getCloudmeFileId() { return cloudmeFileId; }
    public void setCloudmeFileId(String cloudmeFileId) { this.cloudmeFileId = cloudmeFileId; }
    
    public String getCloudmeAccount() { return cloudmeAccount; }
    public void setCloudmeAccount(String cloudmeAccount) { this.cloudmeAccount = cloudmeAccount; }
    
    public String getCloudmePath() { return cloudmePath; }
    public void setCloudmePath(String cloudmePath) { this.cloudmePath = cloudmePath; }
    
    public String getCloudmeFolder() { return cloudmeFolder; }
    public void setCloudmeFolder(String cloudmeFolder) { this.cloudmeFolder = cloudmeFolder; }
    
    public Timestamp getCloudmeModifiedDate() { return cloudmeModifiedDate; }
    public void setCloudmeModifiedDate(Timestamp cloudmeModifiedDate) { this.cloudmeModifiedDate = cloudmeModifiedDate; }
    
    public String getCloudmeSyncStatus() { return cloudmeSyncStatus; }
    public void setCloudmeSyncStatus(String cloudmeSyncStatus) { this.cloudmeSyncStatus = cloudmeSyncStatus; }
    
    public String getWebdavPath() { return webdavPath; }
    public void setWebdavPath(String webdavPath) { this.webdavPath = webdavPath; }
    
    public String getCloudStorage() { return cloudStorage; }
    public void setCloudStorage(String cloudStorage) { this.cloudStorage = cloudStorage; }
    
    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    
    public String getEncryptionKeyHash() { return encryptionKeyHash; }
    public void setEncryptionKeyHash(String encryptionKeyHash) { this.encryptionKeyHash = encryptionKeyHash; }
    
    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }
    
    public String getIvBase64() { return ivBase64; }
    public void setIvBase64(String ivBase64) { this.ivBase64 = ivBase64; }
    
    public String getSaltBase64() { return saltBase64; }
    public void setSaltBase64(String saltBase64) { this.saltBase64 = saltBase64; }
    
    public int getKeyIterations() { return keyIterations; }
    public void setKeyIterations(int keyIterations) { this.keyIterations = keyIterations; }
    
    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
    public void setEncryptionAlgorithm(String encryptionAlgorithm) { this.encryptionAlgorithm = encryptionAlgorithm; }
    
    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    
    public boolean isSensitive() { return sensitive; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }
    
    public Timestamp getAccessExpiry() { return accessExpiry; }
    public void setAccessExpiry(Timestamp accessExpiry) { this.accessExpiry = accessExpiry; }
    
    public Timestamp getUploadDate() { return uploadDate; }
    public void setUploadDate(Timestamp uploadDate) { this.uploadDate = uploadDate; }
    
    public Timestamp getLastDownloadDate() { return lastDownloadDate; }
    public void setLastDownloadDate(Timestamp lastDownloadDate) { this.lastDownloadDate = lastDownloadDate; }
    
    public int getAccessCount() { return accessCount; }
    public void setAccessCount(int accessCount) { this.accessCount = accessCount; }
    
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
    
    public int getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(int maxDownloads) { this.maxDownloads = maxDownloads; }
    
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public String getUploadMethod() { return uploadMethod; }
    public void setUploadMethod(String uploadMethod) { this.uploadMethod = uploadMethod; }
    
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    
    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
    
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    
    public String getSha256EncryptedHash() { return sha256EncryptedHash; }
    public void setSha256EncryptedHash(String sha256EncryptedHash) { this.sha256EncryptedHash = sha256EncryptedHash; }
    
    public double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(double compressionRatio) { this.compressionRatio = compressionRatio; }
    
    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }
    
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    
    public Timestamp getShareExpiry() { return shareExpiry; }
    public void setShareExpiry(Timestamp shareExpiry) { this.shareExpiry = shareExpiry; }
    
    public String getSharePasswordHash() { return sharePasswordHash; }
    public void setSharePasswordHash(String sharePasswordHash) { this.sharePasswordHash = sharePasswordHash; }
    
    public int getShareMaxDownloads() { return shareMaxDownloads; }
    public void setShareMaxDownloads(int shareMaxDownloads) { this.shareMaxDownloads = shareMaxDownloads; }
    
    // Constructor
    public FileMetadata() {
        this.encrypted = true;
        this.sensitive = false;
        this.accessCount = 0;
        this.downloadCount = 0;
        this.retentionDays = 30;
        this.uploadMethod = "standard";
        this.chunkSize = 5242880; // 5MB
        this.uploadStatus = "completed";
        this.cloudStorage = "CLOUDME";
        this.cloudmeSyncStatus = "synced";
        this.encryptionAlgorithm = "AES-256-GCM";
        this.keyIterations = 65536;
    }
    
    public String toDebugString() {
        return String.format(
            "FileMetadata{fileId=%d, userId=%d, original='%s', stored='%s', " +
            "size=%d, cloudFileId='%s', webdavPath='%s', encrypted=%s}",
            fileId, userId, originalFilename, storedFilename, 
            fileSize, cloudFileId, webdavPath, encrypted
        );
    }
}