package com.securefileshare.dao;

import com.securefileshare.models.Activity;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/securefileshare";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "durga2005";
    
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }
    
    public void logActivity(int userId, String actionType, String description, 
                           String ipAddress, String status) throws SQLException {
        String sql = "INSERT INTO access_logs (user_id, action_type, description, ip_address, " +
                    "access_time, status) VALUES (?, ?, ?, ?, NOW(), ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType);
            pstmt.setString(3, description);
            pstmt.setString(4, ipAddress);
            pstmt.setString(5, status);
            
            pstmt.executeUpdate();
            System.out.println("Activity logged: " + actionType + " - " + description);
        }
    }
    
    public void logFileActivity(int userId, int fileId, String actionType, 
                               String description, String ipAddress, String status) throws SQLException {
        String sql = "INSERT INTO access_logs (user_id, file_id, action_type, description, " +
                    "ip_address, access_time, status) VALUES (?, ?, ?, ?, ?, NOW(), ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, fileId);
            pstmt.setString(3, actionType);
            pstmt.setString(4, description);
            pstmt.setString(5, ipAddress);
            pstmt.setString(6, status);
            
            pstmt.executeUpdate();
        }
    }
    
    public List<ActivityLog> getRecentActivities(int userId, int limit) {
        List<ActivityLog> activities = new ArrayList<>();
        String sql = "SELECT * FROM access_logs WHERE user_id = ? ORDER BY access_time DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ActivityLog log = new ActivityLog();
                log.setId(rs.getInt("log_id"));
                log.setUserId(rs.getInt("user_id"));
                
                try {
                    log.setFileId(rs.getInt("file_id"));
                } catch (SQLException e) {
                    log.setFileId(0);
                }
                
                log.setAction(rs.getString("action_type"));
                log.setDescription(rs.getString("description"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setStatus(rs.getString("status"));
                log.setTimestamp(rs.getTimestamp("access_time"));
                
                activities.add(log);
            }
            
            System.out.println("Retrieved " + activities.size() + " recent activities for user: " + userId);
            
        } catch (SQLException e) {
            System.err.println("Error getting recent activities for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return activities;
    }
    
    public List<ActivityLog> getRecentActivities(int limit) {
        List<ActivityLog> activities = new ArrayList<>();
        String sql = "SELECT * FROM access_logs ORDER BY access_time DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ActivityLog log = new ActivityLog();
                log.setId(rs.getInt("log_id"));
                log.setUserId(rs.getInt("user_id"));
                
                try {
                    log.setFileId(rs.getInt("file_id"));
                } catch (SQLException e) {
                    log.setFileId(0);
                }
                
                log.setAction(rs.getString("action_type"));
                log.setDescription(rs.getString("description"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setStatus(rs.getString("status"));
                log.setTimestamp(rs.getTimestamp("access_time"));
                
                activities.add(log);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting recent activities: " + e.getMessage());
            e.printStackTrace();
        }
        
        return activities;
    }
    
    public List<ActivityLog> getRecentActivitiesByAction(String actionType, int limit) {
        List<ActivityLog> activities = new ArrayList<>();
        String sql = "SELECT * FROM access_logs WHERE action_type = ? ORDER BY access_time DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, actionType);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ActivityLog log = new ActivityLog();
                log.setId(rs.getInt("log_id"));
                log.setUserId(rs.getInt("user_id"));
                
                try {
                    log.setFileId(rs.getInt("file_id"));
                } catch (SQLException e) {
                    log.setFileId(0);
                }
                
                log.setAction(rs.getString("action_type"));
                log.setDescription(rs.getString("description"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setStatus(rs.getString("status"));
                log.setTimestamp(rs.getTimestamp("access_time"));
                
                activities.add(log);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting activities by action: " + e.getMessage());
            e.printStackTrace();
        }
        
        return activities;
    }
    
    public List<ActivityLog> getRecentActivitiesByDateRange(int userId, Timestamp startDate, Timestamp endDate) {
        List<ActivityLog> activities = new ArrayList<>();
        String sql = "SELECT * FROM access_logs WHERE user_id = ? AND access_time BETWEEN ? AND ? ORDER BY access_time DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, startDate);
            stmt.setTimestamp(3, endDate);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ActivityLog log = new ActivityLog();
                log.setId(rs.getInt("log_id"));
                log.setUserId(rs.getInt("user_id"));
                
                try {
                    log.setFileId(rs.getInt("file_id"));
                } catch (SQLException e) {
                    log.setFileId(0);
                }
                
                log.setAction(rs.getString("action_type"));
                log.setDescription(rs.getString("description"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setStatus(rs.getString("status"));
                log.setTimestamp(rs.getTimestamp("access_time"));
                
                activities.add(log);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting activities by date range: " + e.getMessage());
            e.printStackTrace();
        }
        
        return activities;
    }
    
    public List<ActivityLog> getRecentActivitiesByAction(int userId, String[] actions, int limit) {
        List<ActivityLog> activities = new ArrayList<>();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < actions.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        
        String sql = "SELECT * FROM access_logs WHERE user_id = ? AND action_type IN (" + 
                     placeholders.toString() + ") ORDER BY access_time DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int paramIndex = 1;
            stmt.setInt(paramIndex++, userId);
            for (String action : actions) {
                stmt.setString(paramIndex++, action);
            }
            stmt.setInt(paramIndex, limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ActivityLog log = new ActivityLog();
                log.setId(rs.getInt("log_id"));
                log.setUserId(rs.getInt("user_id"));
                
                try {
                    log.setFileId(rs.getInt("file_id"));
                } catch (SQLException e) {
                    log.setFileId(0);
                }
                
                log.setAction(rs.getString("action_type"));
                log.setDescription(rs.getString("description"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setStatus(rs.getString("status"));
                log.setTimestamp(rs.getTimestamp("access_time"));
                
                activities.add(log);
            }
        } catch (SQLException e) {
            System.err.println("Error getting recent activities by action: " + e.getMessage());
            e.printStackTrace();
        }
        
        return activities;
    }
    
    public int getDownloadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM access_logs WHERE user_id = ? AND action_type = 'DOWNLOAD' AND status = 'SUCCESS'";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public int getUploadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM access_logs WHERE user_id = ? AND action_type = 'UPLOAD' AND status = 'SUCCESS'";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public List<Activity> getRecentUserActivity(int userId, int limit) throws SQLException {
        List<Activity> activities = new ArrayList<>();
        String sql = "SELECT log_id, user_id, action_type, description, access_time " +
                    "FROM access_logs WHERE user_id = ? ORDER BY access_time DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Activity activity = new Activity();
                activity.setId(rs.getInt("log_id"));
                activity.setUserId(rs.getInt("user_id"));
                activity.setType(rs.getString("action_type"));
                activity.setDescription(rs.getString("description"));
                activity.setTimestamp(rs.getTimestamp("access_time"));
                activities.add(activity);
            }
        } catch (SQLException e) {
            System.err.println("Error loading activities: " + e.getMessage());
            throw e;
        }
        return activities;
    }
    
    public List<Activity> getAllLogs(int limit) {
        List<Activity> activities = new ArrayList<>();
        String sql = "SELECT log_id, user_id, action_type, description, access_time, ip_address, status " +
                     "FROM access_logs ORDER BY access_time DESC LIMIT ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Activity activity = new Activity();
                activity.setId(rs.getInt("log_id"));
                activity.setUserId(rs.getInt("user_id"));
                activity.setAction(rs.getString("action_type"));
                activity.setDescription(rs.getString("description"));
                activity.setTimestamp(rs.getTimestamp("access_time"));
                activity.setIpAddress(rs.getString("ip_address"));
                activity.setStatus(rs.getString("status"));
                activities.add(activity);
            }
        } catch (SQLException e) {
            System.err.println("Error getting logs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return activities;
    }
    
    public List<Activity> getUserLogs(int userId, int limit) throws SQLException {
        List<Activity> activities = new ArrayList<>();
        String sql = "SELECT log_id, user_id, action_type, description, access_time " +
                    "FROM access_logs WHERE user_id = ? ORDER BY access_time DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Activity activity = new Activity();
                activity.setId(rs.getInt("log_id"));
                activity.setUserId(rs.getInt("user_id"));
                activity.setType(rs.getString("action_type"));
                activity.setDescription(rs.getString("description"));
                activity.setTimestamp(rs.getTimestamp("access_time"));
                activities.add(activity);
            }
        } catch (SQLException e) {
            System.err.println("Error loading user logs: " + e.getMessage());
            throw e;
        }
        return activities;
    }
    
    /**
     * FIXED: Get activities by action type (returns Activity objects)
     * This method was missing and causing AdminServlet error
     */
    public List<Activity> getActivitiesByType(String actionType, int limit) {
        List<Activity> activities = new ArrayList<>();
        String sql = "SELECT log_id, user_id, action_type, description, access_time, ip_address, status " +
                     "FROM access_logs WHERE action_type = ? ORDER BY access_time DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, actionType);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Activity activity = new Activity();
                activity.setId(rs.getInt("log_id"));
                activity.setUserId(rs.getInt("user_id"));
                activity.setAction(rs.getString("action_type"));
                activity.setDescription(rs.getString("description"));
                activity.setTimestamp(rs.getTimestamp("access_time"));
                activity.setIpAddress(rs.getString("ip_address"));
                activity.setStatus(rs.getString("status"));
                activities.add(activity);
            }
            
            System.out.println("Retrieved " + activities.size() + " activities of type: " + actionType);
            
        } catch (SQLException e) {
            System.err.println("Error getting activities by type '" + actionType + "': " + e.getMessage());
            e.printStackTrace();
        }
        
        return activities;
    }
    
    public int getTotalActivities() throws SQLException {
        String sql = "SELECT COUNT(*) FROM access_logs";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public int clearOldLogs() throws SQLException {
        String sql = "DELETE FROM access_logs WHERE access_time < DATE_SUB(NOW(), INTERVAL 30 DAY)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            return pstmt.executeUpdate();
        }
    }
    
    public int getUnreadNotifications(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM access_logs WHERE user_id = ? AND status = 'SUCCESS' " +
                    "AND access_time > DATE_SUB(NOW(), INTERVAL 24 HOUR)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Math.min(rs.getInt(1), 9);
            }
        }
        return 0;
    }
    
    public String getLastLogin(int userId) throws SQLException {
        String sql = "SELECT MAX(access_time) FROM access_logs " +
                    "WHERE user_id = ? AND action_type = 'LOGIN' AND status = 'SUCCESS'";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Timestamp lastLogin = rs.getTimestamp(1);
                if (lastLogin != null) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastLogin);
                }
            }
        }
        return null;
    }
    
    /**
     * Get total download count for a specific user
     */
    public int getUserDownloadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM access_logs WHERE user_id = ? AND action_type = 'DOWNLOAD' AND status = 'SUCCESS'";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting download count for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return 0;
    }

    /**
     * Get today's download count for a specific user
     */
    public int getUserTodayDownloadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM access_logs WHERE user_id = ? AND action_type = 'DOWNLOAD' " +
                     "AND status = 'SUCCESS' AND DATE(access_time) = CURDATE()";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting today's download count for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return 0;
    }

    /**
     * Get total upload count for a specific user
     */
    public int getUserUploadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM access_logs WHERE user_id = ? AND action_type = 'UPLOAD' AND status = 'SUCCESS'";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting upload count for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return 0;
    }
    
    public String getLastLoginIP(int userId) throws SQLException {
        String sql = "SELECT ip_address FROM access_logs " +
                    "WHERE user_id = ? AND action_type = 'LOGIN' AND status = 'SUCCESS' " +
                    "ORDER BY access_time DESC LIMIT 1";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("ip_address");
            }
        }
        return null;
    }
    
    public void updateLastLogin(int userId, String ipAddress) throws SQLException {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
        
        logActivity(userId, "LOGIN", "User logged in", ipAddress, "SUCCESS");
    }
    
    public static class ActivityLog {
        private int id;
        private int userId;
        private int fileId;
        private String action;
        private String description;
        private String ipAddress;
        private String status;
        private Timestamp timestamp;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public int getFileId() { return fileId; }
        public void setFileId(int fileId) { this.fileId = fileId; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Timestamp getTimestamp() { return timestamp; }
        public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return "ActivityLog{" +
                    "id=" + id +
                    ", userId=" + userId +
                    ", fileId=" + fileId +
                    ", action='" + action + '\'' +
                    ", description='" + description + '\'' +
                    ", ipAddress='" + ipAddress + '\'' +
                    ", status='" + status + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    // REMOVED: The placeholder method that returned null
    // Now replaced with proper implementation above
}