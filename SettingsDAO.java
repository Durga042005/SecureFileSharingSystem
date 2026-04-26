package com.securefileshare.dao;

import java.sql.*;
import java.util.Properties;

public class SettingsDAO {
    
    public boolean saveSettings(Properties settings) {
        String sql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String key : settings.stringPropertyNames()) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, settings.getProperty(key));
                    pstmt.setString(2, key);
                    pstmt.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public Properties loadSettings() {
        Properties props = new Properties();
        String sql = "SELECT setting_key, setting_value FROM system_settings";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                props.setProperty(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return props;
    }
}