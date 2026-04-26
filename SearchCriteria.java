package com.securefileshare.models;

public class SearchCriteria {
    private String filename;
    private String fileType;
    private String username;
    private String email;
    private String dateFrom;
    private String dateTo;
    private long minSize;
    private long maxSize;
    private boolean encrypted;
    private boolean shared;
    
    // Getters and Setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    
    public long getMinSize() { return minSize; }
    public void setMinSize(long minSize) { this.minSize = minSize; }
    
    public long getMaxSize() { return maxSize; }
    public void setMaxSize(long maxSize) { this.maxSize = maxSize; }
    
    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    
    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }
}