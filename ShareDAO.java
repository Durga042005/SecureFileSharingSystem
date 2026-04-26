package com.securefileshare.dao;

import com.securefileshare.models.FileShare;
import com.securefileshare.models.User;
import com.securefileshare.dao.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShareDAO {
    
    public boolean createShare(FileShare share) {
        if (share.getShareId() == null || share.getShareId().isEmpty()) {
            share.setShareId("SHARE-" + UUID.randomUUID().toString());
        }
        
        String sql = "INSERT INTO file_shares (share_id, file_id, shared_by_user_id, shared_with_email, " +
                    "permission, share_token, share_password_hash, expiry_date, max_downloads, " +
                    "download_count, created_at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, share.getShareId());
            stmt.setInt(2, share.getFileId());
            stmt.setInt(3, share.getOwnerId());
            stmt.setString(4, share.getRecipientEmail());
            stmt.setString(5, share.getPermission());
            stmt.setString(6, share.getShareToken());
            stmt.setString(7, share.getSharePasswordHash());
            stmt.setTimestamp(8, share.getExpiryDate());
            stmt.setInt(9, share.getMaxDownloads());
            stmt.setInt(10, share.getDownloadCount());
            stmt.setTimestamp(11, share.getCreatedAt() != null ? share.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            stmt.setBoolean(12, share.isActive());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Share created successfully with ID: " + share.getShareId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error creating share: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public List<FileShare> getSharesByOwner(int ownerId) {
        List<FileShare> shares = new ArrayList<>();
        String sql = "SELECT * FROM file_shares WHERE shared_by_user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, ownerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                shares.add(mapResultSetToFileShare(rs));
            }
            
            System.out.println("Retrieved " + shares.size() + " shares for owner ID: " + ownerId);
            
        } catch (SQLException e) {
            System.err.println("✗ Error getting shares by owner: " + e.getMessage());
            e.printStackTrace();
        }
        
        return shares;
    }
    
    public List<FileShare> getSharesByRecipientEmail(String email) {
        List<FileShare> shares = new ArrayList<>();
        String sql = "SELECT * FROM file_shares WHERE shared_with_email = ? AND is_active = true ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                shares.add(mapResultSetToFileShare(rs));
            }
            
            System.out.println("Retrieved " + shares.size() + " shares for recipient email: " + email);
            
        } catch (SQLException e) {
            System.err.println("✗ Error getting shares by recipient email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return shares;
    }
    
    public List<FileShare> getSharesByRecipient(int recipientId) {
        UserDAO userDAO = new UserDAO();
        User recipient = userDAO.getUserById(recipientId);
        if (recipient != null && recipient.getEmail() != null) {
            return getSharesByRecipientEmail(recipient.getEmail());
        }
        return new ArrayList<>();
    }
    
    public FileShare getShareById(String shareId) {
        String sql = "SELECT * FROM file_shares WHERE share_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, shareId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToFileShare(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error getting share by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public FileShare getShareByToken(String token) {
        String sql = "SELECT * FROM file_shares WHERE share_token = ? AND is_active = true";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToFileShare(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error getting share by token: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean isFileSharedWithUser(int fileId, int userId) {
        UserDAO userDAO = new UserDAO();
        User recipient = userDAO.getUserById(userId);
        if (recipient != null && recipient.getEmail() != null) {
            return isFileSharedWithEmail(fileId, recipient.getEmail());
        }
        return false;
    }
    
    public boolean isFileSharedWithEmail(int fileId, String email) {
        String sql = "SELECT COUNT(*) FROM file_shares WHERE file_id = ? AND shared_with_email = ? AND is_active = true";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, fileId);
            stmt.setString(2, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error checking if file is shared: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean updateSharePermission(String shareId, String permission) {
        String sql = "UPDATE file_shares SET permission = ? WHERE share_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, permission);
            stmt.setString(2, shareId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✓ Updated permission for share ID: " + shareId + " to: " + permission);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error updating share permission: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean revokeShare(String shareId) {
        String sql = "UPDATE file_shares SET is_active = false WHERE share_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, shareId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✓ Revoked share ID: " + shareId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error revoking share: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean incrementDownloadCount(String shareId) {
        String sql = "UPDATE file_shares SET download_count = download_count + 1, " +
                    "last_accessed = CURRENT_TIMESTAMP WHERE share_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, shareId);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("✗ Error incrementing download count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean hasReachedDownloadLimit(String shareId) {
        String sql = "SELECT download_count, max_downloads FROM file_shares WHERE share_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, shareId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int downloadCount = rs.getInt("download_count");
                int maxDownloads = rs.getInt("max_downloads");
                return maxDownloads > 0 && downloadCount >= maxDownloads;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error checking download limit: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean verifySharePassword(String shareId, String password) {
        String sql = "SELECT share_password_hash FROM file_shares WHERE share_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, shareId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("share_password_hash");
                if (storedHash == null || storedHash.isEmpty()) {
                    return true;
                }
                return storedHash.equals(password);
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error verifying share password: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public List<FileShare> getActiveSharesForFile(int fileId) {
        List<FileShare> shares = new ArrayList<>();
        String sql = "SELECT * FROM file_shares WHERE file_id = ? AND is_active = true ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, fileId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                shares.add(mapResultSetToFileShare(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error getting active shares for file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return shares;
    }
    
    public int cleanupExpiredShares() {
        String sql = "UPDATE file_shares SET is_active = false WHERE expiry_date < NOW() AND expiry_date IS NOT NULL";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("✓ Cleaned up " + affectedRows + " expired shares");
            }
            return affectedRows;
            
        } catch (SQLException e) {
            System.err.println("✗ Error cleaning up expired shares: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    private FileShare mapResultSetToFileShare(ResultSet rs) throws SQLException {
        FileShare share = new FileShare();
        share.setShareId(rs.getString("share_id"));
        share.setFileId(rs.getInt("file_id"));
        share.setOwnerId(rs.getInt("shared_by_user_id"));
        share.setRecipientEmail(rs.getString("shared_with_email"));
        share.setPermission(rs.getString("permission"));
        share.setShareToken(rs.getString("share_token"));
        share.setSharePasswordHash(rs.getString("share_password_hash"));
        share.setExpiryDate(rs.getTimestamp("expiry_date"));
        share.setMaxDownloads(rs.getInt("max_downloads"));
        share.setDownloadCount(rs.getInt("download_count"));
        share.setCreatedAt(rs.getTimestamp("created_at"));
        share.setActive(rs.getBoolean("is_active"));
        share.setLastAccessed(rs.getTimestamp("last_accessed"));
        return share;
    }
}