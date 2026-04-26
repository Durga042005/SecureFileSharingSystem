package com.securefileshare.dao;

import com.securefileshare.models.FileVersion;
import com.securefileshare.dao.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VersionDAO {
    
    public boolean createVersion(int fileId, byte[] fileData, String comment, int createdBy) {
        String sql = "INSERT INTO file_versions (file_id, version_data, version_number, comment, created_by, created_at) " +
                    "VALUES (?, ?, (SELECT COALESCE(MAX(version_number), 0) + 1 FROM file_versions WHERE file_id = ?), ?, ?, NOW())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, fileId);
            stmt.setBytes(2, fileData);
            stmt.setInt(3, fileId);
            stmt.setString(4, comment);
            stmt.setInt(5, createdBy);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Version created for file ID: " + fileId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error creating version: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public List<FileVersion> getFileVersions(int fileId) {
        List<FileVersion> versions = new ArrayList<>();
        String sql = "SELECT * FROM file_versions WHERE file_id = ? ORDER BY version_number DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, fileId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                versions.add(mapResultSetToFileVersion(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error getting file versions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return versions;
    }
    
    public FileVersion getVersion(int versionId) {
        String sql = "SELECT * FROM file_versions WHERE version_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, versionId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToFileVersion(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error getting version: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean restoreVersion(int versionId) {
        String sql = "UPDATE file_versions SET is_current = true WHERE version_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, versionId);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                String updateOtherSql = "UPDATE file_versions SET is_current = false WHERE version_id != ? AND file_id = " +
                                        "(SELECT file_id FROM file_versions WHERE version_id = ?)";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateOtherSql)) {
                    updateStmt.setInt(1, versionId);
                    updateStmt.setInt(2, versionId);
                    updateStmt.executeUpdate();
                }
                
                System.out.println("✓ Version " + versionId + " restored");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error restoring version: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean deleteVersion(int versionId) {
        String sql = "DELETE FROM file_versions WHERE version_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, versionId);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✓ Version " + versionId + " deleted");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Error deleting version: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean permanentDeleteVersion(int versionId) {
        return deleteVersion(versionId);
    }
    
    public int getTotalVersionsCount() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM file_versions";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        
        return 0;
    }
    
    private FileVersion mapResultSetToFileVersion(ResultSet rs) throws SQLException {
        FileVersion version = new FileVersion();
        version.setVersionId(rs.getInt("version_id"));
        version.setFileId(rs.getInt("file_id"));
        version.setVersionNumber(rs.getInt("version_number"));
        version.setComment(rs.getString("comment"));
        version.setCreatedBy(rs.getInt("created_by"));
        version.setCreatedAt(rs.getTimestamp("created_at"));
        version.setFileSize(rs.getLong("file_size"));
        version.setCurrent(rs.getBoolean("is_current"));
        return version;
    }
}