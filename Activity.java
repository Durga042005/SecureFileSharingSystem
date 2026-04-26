package com.securefileshare.models;

import java.sql.Timestamp;

public class Activity {
    private int id;
    private int userId;
    private String type;
    private String description;
    private Timestamp timestamp;
    private String action;  
    private String ipAddress;
    private String status;
    private String username; 
    
    // Constructors
    public Activity() {}
    
    public Activity(int id, int userId, String type, String description, Timestamp timestamp) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
    }
    
    // Simplified constructor for default activities
    public Activity(String type, String description, Timestamp timestamp) {
        this(0, 0, type, description, timestamp);
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "Activity{" +
                "id=" + id +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                ", ipAddress='" + ipAddress + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}