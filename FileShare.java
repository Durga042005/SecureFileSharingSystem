package com.securefileshare.models;

import java.sql.Timestamp;

public class FileShare {
    private String shareId;
    private int fileId;
    private int ownerId;
    private String recipientEmail;
    private String permission;
    private String shareToken;
    private String sharePasswordHash;
    private Timestamp expiryDate;
    private int maxDownloads;
    private int downloadCount;
    private Timestamp createdAt;
    private boolean isActive;
    private Timestamp lastAccessed;
    
    // File details (joined)
    private FileMetadata file;
    private User owner;
    private User recipient;
    
    // Constructors
    public FileShare() {
        this.shareId = "SHARE-" + java.util.UUID.randomUUID().toString();
        this.downloadCount = 0;
        this.maxDownloads = 0;
        this.isActive = true;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
    
    // Getters and Setters
    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }
    
    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }
    
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    
    public String getSharePasswordHash() { return sharePasswordHash; }
    public void setSharePasswordHash(String sharePasswordHash) { this.sharePasswordHash = sharePasswordHash; }
    
    public Timestamp getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Timestamp expiryDate) { this.expiryDate = expiryDate; }
    
    public int getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(int maxDownloads) { this.maxDownloads = maxDownloads; }
    
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    
    public Timestamp getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(Timestamp lastAccessed) { this.lastAccessed = lastAccessed; }
    
    public FileMetadata getFile() { return file; }
    public void setFile(FileMetadata file) { this.file = file; }
    
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { 
        this.recipient = recipient;
        if (recipient != null) {
            this.recipientEmail = recipient.getEmail();
        }
    }
    
    // Helper methods
    public boolean isExpired() {
        return expiryDate != null && expiryDate.before(new Timestamp(System.currentTimeMillis()));
    }
    
    public boolean isPublicLink() {
        return recipientEmail == null || recipientEmail.isEmpty();
    }
    
    public boolean hasReachedDownloadLimit() {
        return maxDownloads > 0 && downloadCount >= maxDownloads;
    }
    
    public boolean isPasswordProtected() {
        return sharePasswordHash != null && !sharePasswordHash.isEmpty();
    }
}