package com.securefileshare.models;

import java.sql.Timestamp;

public class FileVersion {
    private int versionId;
    private int fileId;
    private int versionNumber;
    private String comment;
    private int createdBy;
    private Timestamp createdAt;
    private long fileSize;
    private boolean isCurrent;
    
    // File details (for display)
    private String fileName;
    private String createdByUsername;
    
    // Getters and Setters
    public int getVersionId() { return versionId; }
    public void setVersionId(int versionId) { this.versionId = versionId; }
    
    public int getFileId() { return fileId; }
    public void setFileId(int fileId) { this.fileId = fileId; }
    
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public boolean isCurrent() { return isCurrent; }
    public void setCurrent(boolean isCurrent) { this.isCurrent = isCurrent; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
}