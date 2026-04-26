package com.securefileshare.dao;

import com.securefileshare.models.FileMetadata;
import com.securefileshare.dao.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {
    
    public int saveFileMetadata(FileMetadata metadata) throws SQLException {
        String sql = "INSERT INTO file_metadata (" +
            "user_id, original_filename, stored_filename, " +
            "file_hash, file_size, file_type, description, " +
            "encryption_key, iv, salt, cloudme_path, cloudme_file_id, " +
            "is_sensitive, upload_date, access_count, " +
            "is_encrypted, sha256_hash, sha256_encrypted_hash" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        System.out.println("DEBUG: Saving file metadata with SQL: " + sql);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            int paramIndex = 1;
            
            pstmt.setInt(paramIndex++, metadata.getUserId());
            pstmt.setString(paramIndex++, metadata.getOriginalFilename());
            pstmt.setString(paramIndex++, metadata.getStoredFilename());
            pstmt.setString(paramIndex++, metadata.getFileHash());
            pstmt.setLong(paramIndex++, metadata.getFileSize());
            pstmt.setString(paramIndex++, metadata.getFileType());
            pstmt.setString(paramIndex++, metadata.getDescription());
            
            pstmt.setString(paramIndex++, metadata.getEncryptionKey());
            pstmt.setString(paramIndex++, metadata.getIv());
            pstmt.setString(paramIndex++, metadata.getSalt());
            
            pstmt.setString(paramIndex++, metadata.getStoragePath());
            pstmt.setString(paramIndex++, metadata.getCloudFileId());
            
            pstmt.setBoolean(paramIndex++, false);
            pstmt.setTimestamp(paramIndex++, metadata.getUploadDate() != null ? 
                metadata.getUploadDate() : new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(paramIndex++, metadata.getAccessCount());
            pstmt.setBoolean(paramIndex++, metadata.isEncrypted());
            
            pstmt.setString(paramIndex++, metadata.getFileHash());
            pstmt.setString(paramIndex++, metadata.getProcessedHash() != null ? 
                metadata.getProcessedHash() : metadata.getFileHash());
            
            System.out.println("DEBUG: Executing SQL with " + (paramIndex - 1) + " parameters");
            System.out.println("DEBUG: Cloud File ID: " + metadata.getCloudFileId());
            System.out.println("DEBUG: Salt: " + metadata.getSalt());
            System.out.println("DEBUG: IV: " + metadata.getIv());
            
            int affectedRows = pstmt.executeUpdate();
            System.out.println("DEBUG: Affected rows: " + affectedRows);
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int fileId = rs.getInt(1);
                        System.out.println("DEBUG: Generated file ID: " + fileId);
                        return fileId;
                    }
                }
            }
            
            return -1;
            
        } catch (SQLException e) {
            System.err.println("ERROR saving file metadata: " + e.getMessage());
            System.err.println("SQL: " + sql);
            e.printStackTrace();
            throw e;
        }
    }
    
    public FileMetadata getFileMetadata(int fileId) {
        FileMetadata metadata = null;
        String sql = "SELECT * FROM file_metadata WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, fileId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    metadata = mapResultSetToFileMetadata(rs);
                }
            }
            
            System.out.println("Retrieved file metadata for ID: " + fileId);
            
        } catch (SQLException e) {
            System.err.println("Error getting file metadata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return metadata;
    }
    
    public FileMetadata getFileById(int fileId) {
        return getFileMetadata(fileId);
    }
    
    public List<FileMetadata> getFilesByUserId(int userId) {
        List<FileMetadata> files = new ArrayList<>();
        String sql = "SELECT * FROM file_metadata WHERE user_id = ? ORDER BY upload_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FileMetadata metadata = mapResultSetToFileMetadata(rs);
                    files.add(metadata);
                }
            }
            
            System.out.println("Retrieved " + files.size() + " files for user ID: " + userId);
            
        } catch (SQLException e) {
            System.err.println("Error getting files by user ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return files;
    }
    
    public List<FileMetadata> getRecentFilesByUser(int userId, int limit) {
        List<FileMetadata> files = new ArrayList<>();
        String sql = "SELECT * FROM file_metadata WHERE user_id = ? ORDER BY upload_date DESC LIMIT ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FileMetadata metadata = mapResultSetToFileMetadata(rs);
                    files.add(metadata);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting recent files: " + e.getMessage());
            e.printStackTrace();
        }
        
        return files;
    }
    
    public List<FileMetadata> getAllFiles() {
        List<FileMetadata> files = new ArrayList<>();
        String sql = "SELECT * FROM file_metadata ORDER BY upload_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FileMetadata metadata = mapResultSetToFileMetadata(rs);
                    files.add(metadata);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all files: " + e.getMessage());
            e.printStackTrace();
        }
        
        return files;
    }
    
    public boolean updateAccessCount(int fileId, int accessCount) {
        String sql = "UPDATE file_metadata SET access_count = ? WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, accessCount);
            stmt.setInt(2, fileId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("Updated access count for file ID: " + fileId + " to " + accessCount);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating access count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean incrementDownloadCount(int fileId) {
        String sql = "UPDATE file_metadata SET download_count = COALESCE(download_count, 0) + 1, " +
                    "last_download_date = NOW(), access_count = COALESCE(access_count, 0) + 1 " +
                    "WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, fileId);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("Incremented download count for file ID: " + fileId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error incrementing download count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean updateEncryptionMetadata(int fileId, String salt, String iv, String encryptionKey) {
        String sql = "UPDATE file_metadata SET salt = ?, iv = ?, encryption_key = ? WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, salt);
            stmt.setString(2, iv);
            stmt.setString(3, encryptionKey);
            stmt.setInt(4, fileId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("Updated encryption metadata for file ID: " + fileId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating encryption metadata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean deleteFile(int fileId) {
        String sql = "DELETE FROM file_metadata WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, fileId);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("Deleted file with ID: " + fileId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error deleting file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public int getFileCountByUser(int userId) {
        String sql = "SELECT COUNT(*) as count FROM file_metadata WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting file count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public int getEncryptedFileCountByUser(int userId) {
        String sql = "SELECT COUNT(*) as count FROM file_metadata WHERE user_id = ? AND is_encrypted = true";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting encrypted file count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public long getTotalStorageByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(file_size), 0) as total_size FROM file_metadata WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_size");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting total storage: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public boolean updateFileMetadata(FileMetadata metadata) {
        String sql = "UPDATE file_metadata SET " +
                    "description = ?, is_sensitive = ?, tags = ?, " +
                    "max_downloads = ?, access_expiry = ? " +
                    "WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, metadata.getDescription());
            stmt.setBoolean(2, metadata.isSensitive());
            stmt.setString(3, metadata.getTags());
            stmt.setInt(4, metadata.getMaxDownloads());
            
            if (metadata.getAccessExpiry() != null) {
                stmt.setTimestamp(5, metadata.getAccessExpiry());
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }
            
            stmt.setInt(6, metadata.getFileId());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating file metadata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }

    public boolean softDeleteFile(int fileId, int userId) {
        TrashDAO trashDAO = new TrashDAO();
        return trashDAO.moveToTrash(fileId, userId);
    }

    public List<FileMetadata> getDeletedFiles() {
        List<FileMetadata> files = new ArrayList<>();
        String sql = "SELECT * FROM file_metadata WHERE is_deleted = true ORDER BY deleted_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                FileMetadata file = new FileMetadata();
                file.setFileId(rs.getInt("file_id"));
                file.setUserId(rs.getInt("user_id"));
                file.setOriginalFilename(rs.getString("original_filename"));
                file.setStoredFilename(rs.getString("stored_filename"));
                file.setFileHash(rs.getString("file_hash"));
                file.setFileSize(rs.getLong("file_size"));
                file.setFileType(rs.getString("file_type"));
                file.setDescription(rs.getString("description"));
                file.setCloudFileId(rs.getString("cloud_file_id"));
                file.setEncrypted(rs.getBoolean("is_encrypted"));
                file.setUploadDate(rs.getTimestamp("upload_date"));
                files.add(file);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return files;
    }
    
    private FileMetadata mapResultSetToFileMetadata(ResultSet rs) throws SQLException {
        FileMetadata file = new FileMetadata();
        
        file.setFileId(rs.getInt("file_id"));
        file.setUserId(rs.getInt("user_id"));
        file.setOriginalFilename(rs.getString("original_filename"));
        file.setStoredFilename(rs.getString("stored_filename"));
        file.setFileSize(rs.getLong("file_size"));
        file.setFileType(rs.getString("file_type"));
        file.setDescription(rs.getString("description"));
        
        file.setFileHash(rs.getString("file_hash"));
        
        try {
            file.setProcessedHash(rs.getString("sha256_encrypted_hash"));
        } catch (SQLException e) {
            file.setProcessedHash(rs.getString("file_hash"));
        }
        
        try {
            file.setCloudFileId(rs.getString("cloudme_file_id"));
        } catch (SQLException e) {
            try {
                file.setCloudFileId(rs.getString("cloud_file_id"));
            } catch (SQLException e2) {
                file.setCloudFileId(null);
            }
        }
        
        try {
            file.setStoragePath(rs.getString("cloudme_path"));
        } catch (SQLException e) {
            file.setStoragePath(null);
        }
        
        file.setEncrypted(rs.getBoolean("is_encrypted"));
        file.setEncryptionKey(rs.getString("encryption_key"));
        file.setIv(rs.getString("iv"));
        
        try {
            file.setSalt(rs.getString("salt"));
        } catch (SQLException e) {
            file.setSalt(null);
        }
        
        try {
            file.setEncryptionAlgorithm(rs.getString("encryption_algorithm"));
        } catch (SQLException e) {
            file.setEncryptionAlgorithm("AES-256");
        }
        
        try {
            file.setEncryptionKeyHash(rs.getString("encryption_key_hash"));
        } catch (SQLException e) {
            file.setEncryptionKeyHash(null);
        }
        
        try {
            file.setSensitive(rs.getBoolean("is_sensitive"));
        } catch (SQLException e) {
            file.setSensitive(false);
        }
        
        file.setUploadDate(rs.getTimestamp("upload_date"));
        
        try {
            file.setLastDownloadDate(rs.getTimestamp("last_download_date"));
        } catch (SQLException e) {
            file.setLastDownloadDate(null);
        }
        
        try {
            file.setAccessExpiry(rs.getTimestamp("access_expiry"));
        } catch (SQLException e) {
            file.setAccessExpiry(null);
        }
        
        file.setAccessCount(rs.getInt("access_count"));
        
        try {
            file.setDownloadCount(rs.getInt("download_count"));
        } catch (SQLException e) {
            file.setDownloadCount(0);
        }
        
        try {
            file.setMaxDownloads(rs.getInt("max_downloads"));
        } catch (SQLException e) {
            file.setMaxDownloads(-1);
        }
        
        try {
            file.setTags(rs.getString("tags"));
        } catch (SQLException e) {
            file.setTags(null);
        }
        
        try {
            file.setUploadMethod(rs.getString("upload_method"));
        } catch (SQLException e) {
            file.setUploadMethod("web");
        }
        
        try {
            file.setUploadStatus(rs.getString("upload_status"));
        } catch (SQLException e) {
            file.setUploadStatus("completed");
        }
        
        try {
            file.setCloudStorage(rs.getString("cloud_storage"));
        } catch (SQLException e) {
            file.setCloudStorage("Google Drive");
        }
        
        return file;
    }
}