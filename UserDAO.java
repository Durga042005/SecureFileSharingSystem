package com.securefileshare.dao;

import com.securefileshare.models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {
    
    public boolean registerUser(User user) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO users (username, email, password_hash, first_name, last_name, role, is_active, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            ps.setString(3, hashedPassword);
            
            ps.setString(4, user.getFirstName());
            ps.setString(5, user.getLastName());
            ps.setString(6, user.getRole() != null ? user.getRole() : "USER");
            ps.setBoolean(7, true);
            ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            
            int rows = ps.executeUpdate();
            System.out.println("User registered: " + user.getUsername() + ", rows affected: " + rows);
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error in registerUser: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }
    
    public User loginUser(String username, String password) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE username = ? AND is_active = TRUE";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                if (BCrypt.checkpw(password, storedHash)) {
                    User user = extractUserFromResultSet(rs);
                    
                    updateLastLogin(user.getUserId());
                    
                    return user;
                } else {
                    System.err.println("Password verification failed for user: " + username);
                }
            }
            
            return null;
            
        } catch (SQLException e) {
            System.err.println("Error in loginUser: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public boolean userExists(String username, String email) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) as count FROM users WHERE username = ? OR email = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, email);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt("count");
                return count > 0;
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error in userExists: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public boolean verifyPassword(String username, String password) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                return BCrypt.checkpw(password, storedHash);
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error in verifyPassword: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public User getUserById(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE user_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
            return null;
            
        } catch (SQLException e) {
            System.err.println("Error in getUserById: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public User getUserByUsername(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE username = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
            return null;
            
        } catch (SQLException e) {
            System.err.println("Error in getUserByUsername: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public User getUserByEmail(String email) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE email = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
            
            return null;
            
        } catch (SQLException e) {
            System.err.println("Error in getUserByEmail: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public List<User> getAllUsers() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<User> users = new ArrayList<>();
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users ORDER BY created_at DESC";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
            
            return users;
            
        } catch (SQLException e) {
            System.err.println("Error in getAllUsers: " + e.getMessage());
            e.printStackTrace();
            return users;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public List<User> getActiveUsers() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<User> users = new ArrayList<>();
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE is_active = TRUE ORDER BY created_at DESC";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
            
            return users;
            
        } catch (SQLException e) {
            System.err.println("Error in getActiveUsers: " + e.getMessage());
            e.printStackTrace();
            return users;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public boolean updateUserProfile(User user) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE user_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setInt(4, user.getUserId());
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error in updateUserProfile: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }
    
    public boolean updatePassword(int userId, String newPassword) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
            ps = conn.prepareStatement(sql);
            
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error in updatePassword: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }
    
    public boolean updateUserSettings(int userId, boolean emailNotifications, boolean twoFactorAuth, boolean autoEncrypt) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String sql = "UPDATE users SET ";
            boolean hasColumns = false;
            
            try {
                sql = "UPDATE users SET email_notifications = ?, two_factor_enabled = ?, auto_encrypt = ? WHERE user_id = ?";
                ps = conn.prepareStatement(sql);
                ps.setBoolean(1, emailNotifications);
                ps.setBoolean(2, twoFactorAuth);
                ps.setBoolean(3, autoEncrypt);
                ps.setInt(4, userId);
            } catch (SQLException e) {
                System.err.println("Settings columns may not exist, skipping...");
                return true;
            }
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error in updateUserSettings: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }
    
    public void updateLastLogin(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }
    
    public boolean updateUserRole(int userId, String role) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET role = ? WHERE user_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, role);
            ps.setInt(2, userId);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error in updateUserRole: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }
    
    public boolean updateUserStatus(int userId, boolean isActive) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET is_active = ? WHERE user_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setBoolean(1, isActive);
            ps.setInt(2, userId);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error in updateUserStatus: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }
    
    public boolean deactivateUser(int userId) {
        return updateUserStatus(userId, false);
    }
    
    public boolean activateUser(int userId) {
        return updateUserStatus(userId, true);
    }
    
    public boolean deleteUser(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "DELETE FROM users WHERE user_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error in deleteUser: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.close(conn, ps, null);
        }
    }
    
    public int getUserCount() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) as count FROM users";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
            
            return 0;
            
        } catch (SQLException e) {
            System.err.println("Error in getUserCount: " + e.getMessage());
            e.printStackTrace();
            return 0;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public int getActiveUserCount() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) as count FROM users WHERE is_active = TRUE";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
            
            return 0;
            
        } catch (SQLException e) {
            System.err.println("Error in getActiveUserCount: " + e.getMessage());
            e.printStackTrace();
            return 0;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    public List<User> searchUsers(String searchTerm) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<User> users = new ArrayList<>();
        
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? OR first_name LIKE ? OR last_name LIKE ?";
            ps = conn.prepareStatement(sql);
            String pattern = "%" + searchTerm + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            
            rs = ps.executeQuery();
            
            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
            
            return users;
            
        } catch (SQLException e) {
            System.err.println("Error in searchUsers: " + e.getMessage());
            e.printStackTrace();
            return users;
        } finally {
            DatabaseConnection.close(conn, ps, rs);
        }
    }
    
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        
        try {
            user.setEmailNotifications(rs.getBoolean("email_notifications"));
        } catch (SQLException e) {
            user.setEmailNotifications(true);
        }
        
        try {
            user.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));
        } catch (SQLException e) {
            user.setTwoFactorEnabled(false);
        }
        
        try {
            user.setAutoEncrypt(rs.getBoolean("auto_encrypt"));
        } catch (SQLException e) {
            user.setAutoEncrypt(true);
        }
        
        try {
            user.setItemsPerPage(rs.getInt("items_per_page"));
        } catch (SQLException e) {
            user.setItemsPerPage(25);
        }
        
        return user;
    }
}