package com.securefileshare.models;

import java.sql.Timestamp;

public class TrashItem {
    private int trashId;
    private int fileId;
    private String originalFilename;
    private long fileSize;
    private int deletedBy;
    private String deletedByUsername;
    private Timestamp deletedAt;
    private String cloudFileId;
    
    // Getters and Setters
    public int getTrashId() { return trashId; }
    public void setTrashId(int trashId) { this.trashId = trashId; }
    
    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }
    
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public int getDeletedBy() { return deletedBy; }
    public void setDeletedBy(int deletedBy) { this.deletedBy = deletedBy; }
    
    public String getDeletedByUsername() { return deletedByUsername; }
    public void setDeletedByUsername(String deletedByUsername) { this.deletedByUsername = deletedByUsername; }
    
    public Timestamp getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Timestamp deletedAt) { this.deletedAt = deletedAt; }
    
    public String getCloudFileId() { return cloudFileId; }
    public void setCloudFileId(String cloudFileId) { this.cloudFileId = cloudFileId; }
    
    public String getFormattedSize() {
        if (fileSize < 1024) return fileSize + " B";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", fileSize / Math.pow(1024, exp), pre);
    }
}