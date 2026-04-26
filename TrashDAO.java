package com.securefileshare.dao;

import com.securefileshare.models.TrashItem;
import com.securefileshare.dao.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrashDAO {
    
    public boolean moveToTrash(int fileId, int deletedBy) {
        String selectSql = "SELECT * FROM file_metadata WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            
            selectStmt.setInt(1, fileId);
            ResultSet rs = selectStmt.executeQuery();
            
            if (rs.next()) {
                String insertSql = "INSERT INTO trash_items (file_id, original_filename, file_size, deleted_by, deleted_at, cloud_file_id) " +
                                  "VALUES (?, ?, ?, ?, NOW(), ?)";
                
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, fileId);
                    insertStmt.setString(2, rs.getString("original_filename"));
                    insertStmt.setLong(3, rs.getLong("file_size"));
                    insertStmt.setInt(4, deletedBy);
                    insertStmt.setString(5, rs.getString("cloud_file_id"));
                    
                    int affectedRows = insertStmt.executeUpdate();
                    
                    if (affectedRows > 0) {
                        String updateSql = "UPDATE file_metadata SET is_deleted = true, deleted_at = NOW() WHERE file_id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, fileId);
                            updateStmt.executeUpdate();
                        }
                        
                        System.out.println("✓ File ID " + fileId + " moved to trash");
                        return true;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error moving to trash: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean restoreFile(int trashItemId) {
        String selectSql = "SELECT * FROM trash_items WHERE trash_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            
            selectStmt.setInt(1, trashItemId);
            ResultSet rs = selectStmt.executeQuery();
            
            if (rs.next()) {
                int fileId = rs.getInt("file_id");
                
                String updateSql = "UPDATE file_metadata SET is_deleted = false, deleted_at = NULL WHERE file_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, fileId);
                    updateStmt.executeUpdate();
                }
                
                String deleteSql = "DELETE FROM trash_items WHERE trash_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, trashItemId);
                    deleteStmt.executeUpdate();
                }
                
                System.out.println("✓ File ID " + fileId + " restored from trash");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error restoring from trash: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean permanentDeleteFile(int fileId) {
        String deleteFileSql = "DELETE FROM file_metadata WHERE file_id = ?";
        String deleteTrashSql = "DELETE FROM trash_items WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement deleteFileStmt = conn.prepareStatement(deleteFileSql);
                 PreparedStatement deleteTrashStmt = conn.prepareStatement(deleteTrashSql)) {
                
                deleteTrashStmt.setInt(1, fileId);
                deleteTrashStmt.executeUpdate();
                
                deleteFileStmt.setInt(1, fileId);
                deleteFileStmt.executeUpdate();
                
                conn.commit();
                System.out.println("✓ File ID " + fileId + " permanently deleted");
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error permanently deleting file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public List<TrashItem> getAllTrashItems() throws SQLException {
        List<TrashItem> items = new ArrayList<>();
        String sql = "SELECT t.*, u.username as deleted_by_username FROM trash_items t " +
                    "LEFT JOIN users u ON t.deleted_by = u.user_id " +
                    "ORDER BY t.deleted_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                TrashItem item = new TrashItem();
                item.setTrashId(rs.getInt("trash_id"));
                item.setFileId(rs.getInt("file_id"));
                item.setOriginalFilename(rs.getString("original_filename"));
                item.setFileSize(rs.getLong("file_size"));
                item.setDeletedBy(rs.getInt("deleted_by"));
                item.setDeletedByUsername(rs.getString("deleted_by_username"));
                item.setDeletedAt(rs.getTimestamp("deleted_at"));
                item.setCloudFileId(rs.getString("cloud_file_id"));
                items.add(item);
            }
        }
        
        return items;
    }
    
    public int getTrashCount() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM trash_items";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        
        return 0;
    }
    
    public int emptyTrash() throws SQLException {
        String sql = "DELETE FROM trash_items";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int affectedRows = stmt.executeUpdate();
            System.out.println("✓ Emptied trash, " + affectedRows + " items removed");
            return affectedRows;
        }
    }
}