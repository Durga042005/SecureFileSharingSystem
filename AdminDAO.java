package com.securefileshare.dao;

import com.securefileshare.dao.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class AdminDAO {
    
    public Map<String, Object> getStorageStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
            "COUNT(*) as total_files, " +
            "COALESCE(SUM(file_size), 0) as total_size, " +
            "COUNT(CASE WHEN is_encrypted = true THEN 1 END) as encrypted_count " +
            "FROM file_metadata WHERE is_deleted = false";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                stats.put("totalFiles", rs.getInt("total_files"));
                stats.put("totalSize", rs.getLong("total_size"));
                stats.put("encryptedCount", rs.getInt("encrypted_count"));
                stats.put("formattedSize", formatFileSize(rs.getLong("total_size")));
                stats.put("formattedTotal", "15.00 GB");
                stats.put("percent", (double) rs.getLong("total_size") / (15L * 1024 * 1024 * 1024) * 100);
            }
        }
        return stats;
    }
    
    public Map<String, Object> getUserStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
            "COUNT(*) as total_users, " +
            "COUNT(CASE WHEN is_active = true THEN 1 END) as active_users, " +
            "COUNT(CASE WHEN role = 'ADMIN' THEN 1 END) as admin_count, " +
            "MAX(last_login) as last_login " +
            "FROM users";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                stats.put("totalUsers", rs.getInt("total_users"));
                stats.put("activeUsers", rs.getInt("active_users"));
                stats.put("adminCount", rs.getInt("admin_count"));
                stats.put("lastLogin", rs.getTimestamp("last_login"));
            }
        }
        return stats;
    }
    
    public Map<String, Integer> getActivityStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT action, COUNT(*) as count FROM audit_log GROUP BY action";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                stats.put(rs.getString("action"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get activity stats: " + e.getMessage());
        }
        return stats;
    }
    
    public Map<String, Integer> getWeeklyActivity(String actionType) throws SQLException {
        Map<String, Integer> activity = new LinkedHashMap<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        
        for (String day : days) {
            activity.put(day, 0);
        }
        
        String sql = "SELECT DAYOFWEEK(timestamp) as day_num, COUNT(*) as count " +
                    "FROM audit_log " +
                    "WHERE action = ? AND timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                    "GROUP BY DAYOFWEEK(timestamp) " +
                    "ORDER BY day_num";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, actionType);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int dayNum = rs.getInt("day_num");
                int count = rs.getInt("count");
                if (dayNum >= 2 && dayNum <= 7) {
                    activity.put(days[dayNum - 2], count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get weekly activity: " + e.getMessage());
        }
        return activity;
    }
    
    public int getSensitiveFileCount() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM file_metadata WHERE is_sensitive = true AND is_deleted = false";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get sensitive file count: " + e.getMessage());
        }
        return 0;
    }
    
    public int getTodayDownloads() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM audit_log " +
                    "WHERE action = 'DOWNLOAD' AND DATE(timestamp) = CURDATE()";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get today's downloads: " + e.getMessage());
        }
        return 0;
    }
    
    public Map<Integer, Integer> getUserFileCounts() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT user_id, COUNT(*) as count FROM file_metadata WHERE is_deleted = false GROUP BY user_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                counts.put(rs.getInt("user_id"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get user file counts: " + e.getMessage());
        }
        return counts;
    }
    
    public Map<Integer, String> getUserLastActivity() throws SQLException {
        Map<Integer, String> activity = new HashMap<>();
        
        String checkTableSql = "SELECT COUNT(*) as count FROM information_schema.tables WHERE table_name = 'audit_log'";
        boolean tableExists = false;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkTableSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                tableExists = rs.getInt("count") > 0;
            }
        }
        
        if (!tableExists) {
            System.out.println("Warning: audit_log table does not exist yet");
            return activity;
        }
        
        String sql = "SELECT a.user_id, MAX(a.timestamp) as last_time, u.username " +
                    "FROM audit_log a JOIN users u ON a.user_id = u.user_id " +
                    "GROUP BY a.user_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("last_time");
                if (ts != null) {
                    activity.put(rs.getInt("user_id"), 
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts));
                }
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get user last activity: " + e.getMessage());
        }
        return activity;
    }
    
    public Map<Integer, String> getFileOwners() throws SQLException {
        Map<Integer, String> owners = new HashMap<>();
        String sql = "SELECT f.file_id, u.username " +
                    "FROM file_metadata f JOIN users u ON f.user_id = u.user_id " +
                    "WHERE f.is_deleted = false";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                owners.put(rs.getInt("file_id"), rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get file owners: " + e.getMessage());
        }
        return owners;
    }
    
    public Map<Integer, Integer> getFileShareCounts() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT file_id, COUNT(*) as count FROM file_shares WHERE is_active = true GROUP BY file_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                counts.put(rs.getInt("file_id"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get file share counts: " + e.getMessage());
        }
        return counts;
    }
    
    public Map<String, Object> getStorageHealth() throws SQLException {
        Map<String, Object> health = new HashMap<>();
        String sql = "SELECT " +
            "COALESCE(SUM(file_size), 0) as used " +
            "FROM file_metadata WHERE is_deleted = false";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                long used = rs.getLong("used");
                long total = 15L * 1024 * 1024 * 1024;
                double percent = (double) used / total * 100;
                
                health.put("used", used);
                health.put("total", total);
                health.put("percent", Math.round(percent * 100) / 100.0);
                health.put("formattedUsed", formatFileSize(used));
                health.put("formattedTotal", formatFileSize(total));
                health.put("status", percent < 80 ? "good" : (percent < 95 ? "warning" : "critical"));
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not get storage health: " + e.getMessage());
        }
        return health;
    }
    
    public int getActiveSessionCount() {
        return 5;
    }
    
    public String getLastBackupTime() {
        return "2024-02-16 03:00 AM";
    }
    
    public String getSystemUptime() {
        return "2 days, 5 hours";
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}