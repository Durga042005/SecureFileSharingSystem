package com.securefileshare.models;

import java.sql.Timestamp;

public class User {
    private int userId;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private Timestamp createdAt;
    private Timestamp lastLogin;
    private String lastLoginIP;
    private boolean isActive;
    private String firstName;
    private String lastName;
    
    // Additional profile settings
    private boolean emailNotifications;
    private boolean twoFactorEnabled;
    private boolean autoEncrypt;
    private int itemsPerPage;
    
    // Security settings
    private String trustedDevices;
    private String trustedIPs;
    
    // Constructors
    public User() {
        this.emailNotifications = true;
        this.autoEncrypt = true;
        this.itemsPerPage = 25;
        this.isActive = true;
        this.role = "USER";
    }
    
    public User(String username, String email, String password) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    // ==================== BASIC GETTERS AND SETTERS ====================
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public String getLastLoginIP() {
        return lastLoginIP;
    }
    
    public void setLastLoginIP(String lastLoginIP) {
        this.lastLoginIP = lastLoginIP;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    // ==================== SETTINGS GETTERS AND SETTERS ====================
    
    public boolean isEmailNotifications() {
        return emailNotifications;
    }
    
    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
    
    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }
    
    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
    
    public boolean isAutoEncrypt() {
        return autoEncrypt;
    }
    
    public void setAutoEncrypt(boolean autoEncrypt) {
        this.autoEncrypt = autoEncrypt;
    }
    
    public int getItemsPerPage() {
        return itemsPerPage;
    }
    
    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage > 0 ? itemsPerPage : 25;
    }
    
    // ==================== SECURITY GETTERS AND SETTERS ====================
    
    public String getTrustedDevices() {
        return trustedDevices;
    }
    
    public void setTrustedDevices(String trustedDevices) {
        this.trustedDevices = trustedDevices;
    }
    
    public String getTrustedIPs() {
        return trustedIPs;
    }
    
    public void setTrustedIPs(String trustedIPs) {
        this.trustedIPs = trustedIPs;
    }
    
    // ==================== HELPER METHODS ====================
    
    public String getDisplayName() {
        return (fullName != null && !fullName.trim().isEmpty()) ? fullName : username;
    }
    
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
    
    public void addTrustedDevice(String deviceId) {
        if (trustedDevices == null || trustedDevices.isEmpty()) {
            trustedDevices = deviceId;
        } else {
            trustedDevices += "," + deviceId;
        }
    }
    
    public void addTrustedIP(String ipAddress) {
        if (trustedIPs == null || trustedIPs.isEmpty()) {
            trustedIPs = ipAddress;
        } else {
            trustedIPs += "," + ipAddress;
        }
    }
    
    public boolean isDeviceTrusted(String deviceId) {
        if (trustedDevices == null || trustedDevices.isEmpty()) return false;
        String[] devices = trustedDevices.split(",");
        for (String device : devices) {
            if (device.equals(deviceId)) return true;
        }
        return false;
    }
    
    public boolean isIPTrusted(String ipAddress) {
        if (trustedIPs == null || trustedIPs.isEmpty()) return false;
        String[] ips = trustedIPs.split(",");
        for (String ip : ips) {
            if (ip.equals(ipAddress)) return true;
        }
        return false;
    }
    
    public void removeTrustedDevice(String deviceId) {
        if (trustedDevices == null || trustedDevices.isEmpty()) return;
        String[] devices = trustedDevices.split(",");
        StringBuilder newDevices = new StringBuilder();
        for (String device : devices) {
            if (!device.equals(deviceId)) {
                if (newDevices.length() > 0) newDevices.append(",");
                newDevices.append(device);
            }
        }
        trustedDevices = newDevices.toString();
    }
    
    public void removeTrustedIP(String ipAddress) {
        if (trustedIPs == null || trustedIPs.isEmpty()) return;
        String[] ips = trustedIPs.split(",");
        StringBuilder newIPs = new StringBuilder();
        for (String ip : ips) {
            if (!ip.equals(ipAddress)) {
                if (newIPs.length() > 0) newIPs.append(",");
                newIPs.append(ip);
            }
        }
        trustedIPs = newIPs.toString();
    }
    
    public void clearTrustedDevices() {
        trustedDevices = null;
    }
    
    public void clearTrustedIPs() {
        trustedIPs = null;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", emailNotifications=" + emailNotifications +
                ", twoFactorEnabled=" + twoFactorEnabled +
                ", autoEncrypt=" + autoEncrypt +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return userId == user.userId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }

    public void setPasswordHash(String passwordHash) {
        this.password = passwordHash;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    public String getFirstName() {
        return firstName != null ? firstName : "";
    }

    public String getLastName() {
        return lastName != null ? lastName : "";
    }

    private void updateFullName() {
        if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
            this.fullName = firstName + " " + lastName;
        } else if (firstName != null && !firstName.isEmpty()) {
            this.fullName = firstName;
        } else if (lastName != null && !lastName.isEmpty()) {
            this.fullName = lastName;
        }
    }
}