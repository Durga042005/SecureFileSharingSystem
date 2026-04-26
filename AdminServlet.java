package com.securefileshare.servlets;

import com.securefileshare.models.User;
import com.securefileshare.dao.AdminDAO;
import com.securefileshare.dao.AuditDAO;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.dao.UserDAO;
import com.securefileshare.models.Activity;
import com.securefileshare.models.FileMetadata;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {
    
    private UserDAO userDAO;
    private FileDAO fileDAO;
    private AuditDAO auditDAO;
    private AdminDAO adminDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        fileDAO = new FileDAO();
        auditDAO = new AuditDAO();
        adminDAO = new AdminDAO();
        gson = new Gson();
        System.out.println("✓ AdminServlet initialized successfully");
        System.out.println("✓ AdminServlet mapped to: /admin/*");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        System.out.println("\n=== ADMIN SERVLET REQUEST ===");
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("PathInfo: " + request.getPathInfo());
        System.out.println("User: " + (user != null ? user.getUsername() : "null"));
        System.out.println("User Role: " + (user != null ? user.getRole() : "null"));
        
        if (user == null) {
            System.out.println("✗ No user in session, redirecting to login");
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        if (!"ADMIN".equals(user.getRole())) {
            System.out.println("✗ User " + user.getUsername() + " is not admin (role: " + user.getRole() + ")");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        
        String pathInfo = request.getPathInfo();
        System.out.println("Processing path: " + pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/dashboard")) {
                System.out.println("→ Showing admin dashboard");
                showAdminDashboard(request, response);
            } else if (pathInfo.equals("/users")) {
                System.out.println("→ Showing user management");
                showUsers(request, response);
            } else if (pathInfo.matches("/users/\\d+")) {
                System.out.println("→ Showing user details");
                showUserDetails(request, response);
            } else if (pathInfo.equals("/files")) {
                System.out.println("→ Showing file management");
                showFiles(request, response);
            } else if (pathInfo.matches("/files/\\d+")) {
                System.out.println("→ Showing file details");
                showFileDetails(request, response);
            } else if (pathInfo.equals("/logs")) {
                System.out.println("→ Showing audit logs");
                showAuditLogs(request, response);
            } else if (pathInfo.equals("/settings")) {
                System.out.println("→ Showing system settings");
                showSystemSettings(request, response);
            } else if (pathInfo.equals("/system-status")) {
                System.out.println("→ Getting system status");
                getSystemStatus(request, response);
            } else {
                System.out.println("✗ Unknown path: " + pathInfo);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            System.err.println("✗ Error in AdminServlet: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        
        if (user == null || !"ADMIN".equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        String pathInfo = request.getPathInfo();
        String action = request.getParameter("action");
        
        System.out.println("Admin POST - PathInfo: " + pathInfo + ", Action: " + action);
        
        try {
            // Handle settings save from /admin/settings
            if (pathInfo != null && pathInfo.equals("/settings")) {
                System.out.println("→ Saving system settings");
                saveSettings(request, response);
                return;
            }
            
            // Handle other actions with action parameter
            if ("toggle-user".equals(action)) {
                toggleUserStatus(request, response);
            } else if ("toggle-admin".equals(action)) {
                toggleAdminStatus(request, response);
            } else if ("delete-user".equals(action)) {
                deleteUser(request, response);
            } else if ("delete-file".equals(action)) {
                deleteFile(request, response);
            } else if ("clear-logs".equals(action)) {
                clearAuditLogs(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * FIXED: Save system settings from admin panel with proper JSON response
     */
    private void saveSettings(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            System.out.println("\n=== SAVING SYSTEM SETTINGS ===");
            
            // Log all received parameters for debugging
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (parameterMap.isEmpty()) {
                System.out.println("⚠ No parameters received!");
                result.put("success", false);
                result.put("message", "No settings data received");
            } else {
                System.out.println("Received " + parameterMap.size() + " parameters:");
                for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                    String key = entry.getKey();
                    String[] values = entry.getValue();
                    String value = (values != null && values.length > 0) ? values[0] : "";
                    System.out.println("  " + key + ": " + value);
                }
                
                // Get all parameters from the form (optional - you can process them as needed)
                String sessionTimeout = request.getParameter("sessionTimeout");
                String otpExpiry = request.getParameter("otpExpiry");
                String maxLoginAttempts = request.getParameter("maxLoginAttempts");
                
                // Security settings
                String encryptionEnabled = request.getParameter("encryptionEnabled");
                String integrityCheck = request.getParameter("integrityCheck");
                String otpEnabled = request.getParameter("otpEnabled");
                String accessControl = request.getParameter("accessControl");
                String auditLogging = request.getParameter("auditLogging");
                String httpsOnly = request.getParameter("httpsOnly");
                
                // Password policy
                String minPasswordLength = request.getParameter("minPasswordLength");
                String requireUppercase = request.getParameter("requireUppercase");
                String requireSpecial = request.getParameter("requireSpecial");
                
                // Storage settings
                String storageProvider = request.getParameter("storageProvider");
                String maxFileSize = request.getParameter("maxFileSize");
                String allowedFileTypes = request.getParameter("allowedFileTypes");
                
                // Email settings
                String smtpHost = request.getParameter("smtpHost");
                String smtpPort = request.getParameter("smtpPort");
                String smtpUsername = request.getParameter("smtpUsername");
                String smtpPassword = request.getParameter("smtpPassword");
                String fromEmail = request.getParameter("fromEmail");
                String emailEnabled = request.getParameter("emailEnabled");
                
                // Scheduled tasks
                String autoBackup = request.getParameter("autoBackup");
                String logRetention = request.getParameter("logRetention");
                String trashCleanup = request.getParameter("trashCleanup");
                
                // TODO: Save settings to database or properties file
                // For now, store in application context as demo
                request.getServletContext().setAttribute("systemSettings", parameterMap);
                
                result.put("success", true);
                result.put("message", "Settings saved successfully!");
                
                System.out.println("✓ Settings saved successfully");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error saving settings: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error saving settings: " + e.getMessage());
        }
        
        // Always return JSON response
        String jsonResponse = gson.toJson(result);
        System.out.println("Returning response: " + jsonResponse);
        response.getWriter().write(jsonResponse);
    }
    
    private void showAdminDashboard(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException, SQLException {
        
        try {
            int totalUsers = userDAO.getAllUsers().size();
            int activeUsers = userDAO.getActiveUserCount();
            int totalFiles = fileDAO.getAllFiles().size();
            
            List<Activity> recentActivity = auditDAO.getAllLogs(10);
            if (recentActivity == null) {
                recentActivity = new ArrayList<>();
            }
            
            List<User> allUsers = userDAO.getAllUsers();
            if (allUsers == null) {
                allUsers = new ArrayList<>();
            }
            
            Map<String, Object> storageStats = adminDAO.getStorageStats();
            if (storageStats == null) {
                storageStats = new HashMap<>();
            }
            
            Map<String, Object> userStats = adminDAO.getUserStats();
            if (userStats == null) {
                userStats = new HashMap<>();
            }
            
            Map<String, Integer> activityStats = adminDAO.getActivityStats();
            if (activityStats == null) {
                activityStats = new HashMap<>();
            }
            
            Map<String, Integer> weeklyUploads = adminDAO.getWeeklyActivity("UPLOAD");
            if (weeklyUploads == null) {
                weeklyUploads = new HashMap<>();
            }
            
            Map<String, Integer> weeklyDownloads = adminDAO.getWeeklyActivity("DOWNLOAD");
            if (weeklyDownloads == null) {
                weeklyDownloads = new HashMap<>();
            }
            
            Map<String, Boolean> securityStatus = new HashMap<>();
            securityStatus.put("encryption", true);
            securityStatus.put("integrity", true);
            securityStatus.put("otp", true);
            securityStatus.put("accessControl", true);
            securityStatus.put("auditLogging", true);
            
            request.setAttribute("totalUsers", totalUsers);
            request.setAttribute("activeUsers", activeUsers);
            request.setAttribute("totalFiles", totalFiles);
            request.setAttribute("sensitiveFiles", adminDAO.getSensitiveFileCount());
            request.setAttribute("todayDownloads", adminDAO.getTodayDownloads());
            request.setAttribute("recentActivity", recentActivity);
            request.setAttribute("allUsers", allUsers);
            request.setAttribute("storageStats", storageStats);
            request.setAttribute("userStats", userStats);
            request.setAttribute("activityStats", activityStats);
            request.setAttribute("weeklyUploads", weeklyUploads);
            request.setAttribute("weeklyDownloads", weeklyDownloads);
            request.setAttribute("securityStatus", securityStatus);
            
            System.out.println("✓ Admin dashboard attributes set successfully");
            System.out.println("Total Users: " + totalUsers);
            System.out.println("Total Files: " + totalFiles);
            System.out.println("Recent Activity count: " + recentActivity.size());
            System.out.println("Forwarding to: /jsp/admin/admin-dashboard.jsp");
            
            request.getRequestDispatcher("/jsp/admin/admin-dashboard.jsp").forward(request, response);
            
        } catch (Exception e) {
            System.err.println("✗ Error in showAdminDashboard: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private void showUsers(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException, SQLException {
        
        List<User> users = userDAO.getAllUsers();
        if (users == null) {
            users = new ArrayList<>();
        }
        
        Map<Integer, Integer> userFileCounts = adminDAO.getUserFileCounts();
        if (userFileCounts == null) {
            userFileCounts = new HashMap<>();
        }
        
        Map<Integer, String> userLastActivity = adminDAO.getUserLastActivity();
        if (userLastActivity == null) {
            userLastActivity = new HashMap<>();
        }
        
        request.setAttribute("users", users);
        request.setAttribute("userFileCounts", userFileCounts);
        request.setAttribute("userLastActivity", userLastActivity);
        
        System.out.println("Showing users page with " + users.size() + " users");
        
        request.getRequestDispatcher("/jsp/admin/users.jsp").forward(request, response);
    }
    
    private void showUserDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException, SQLException {
        
        String pathInfo = request.getPathInfo();
        int userId = Integer.parseInt(pathInfo.substring(pathInfo.lastIndexOf("/") + 1));
        
        User targetUser = userDAO.getUserById(userId);
        List<FileMetadata> userFiles = fileDAO.getFilesByUserId(userId);
        if (userFiles == null) {
            userFiles = new ArrayList<>();
        }
        
        List<Activity> userActivity = auditDAO.getUserLogs(userId, 20);
        if (userActivity == null) {
            userActivity = new ArrayList<>();
        }
        
        int fileCount = fileDAO.getFileCountByUser(userId);
        
        request.setAttribute("targetUser", targetUser);
        request.setAttribute("userFiles", userFiles);
        request.setAttribute("userActivity", userActivity);
        request.setAttribute("fileCount", fileCount);
        
        System.out.println("Showing details for user ID: " + userId);
        
        request.getRequestDispatcher("/jsp/admin/user-details.jsp").forward(request, response);
    }
    
    private void showFiles(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException, SQLException {
        
        List<FileMetadata> files = fileDAO.getAllFiles();
        if (files == null) {
            files = new ArrayList<>();
        }
        
        Map<Integer, String> fileOwners = adminDAO.getFileOwners();
        if (fileOwners == null) {
            fileOwners = new HashMap<>();
        }
        
        Map<Integer, Integer> fileShares = adminDAO.getFileShareCounts();
        if (fileShares == null) {
            fileShares = new HashMap<>();
        }
        
        request.setAttribute("files", files);
        request.setAttribute("fileOwners", fileOwners);
        request.setAttribute("fileShares", fileShares);
        
        System.out.println("Showing files page with " + files.size() + " files");
        
        request.getRequestDispatcher("/jsp/admin/files.jsp").forward(request, response);
    }
    
    private void showFileDetails(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException, SQLException {
        
        String pathInfo = request.getPathInfo();
        int fileId = Integer.parseInt(pathInfo.substring(pathInfo.lastIndexOf("/") + 1));
        
        FileMetadata file = fileDAO.getFileMetadata(fileId);
        String owner = adminDAO.getFileOwners().get(fileId);
        int shareCount = adminDAO.getFileShareCounts().getOrDefault(fileId, 0);
        
        request.setAttribute("file", file);
        request.setAttribute("owner", owner);
        request.setAttribute("shareCount", shareCount);
        
        System.out.println("Showing details for file ID: " + fileId);
        System.out.println("File: " + (file != null ? file.getOriginalFilename() : "Not found"));
        System.out.println("Owner: " + owner);
        System.out.println("Share Count: " + shareCount);
        
        request.getRequestDispatcher("/jsp/admin/file-details.jsp").forward(request, response);
    }
    
    private void showAuditLogs(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException, SQLException {
        
        String filter = request.getParameter("filter");
        String dateFrom = request.getParameter("dateFrom");
        String dateTo = request.getParameter("dateTo");
        
        List<Activity> logs = null;
        
        try {
            if (filter != null && !filter.isEmpty()) {
                logs = auditDAO.getActivitiesByType(filter, 1000);
            } else {
                logs = auditDAO.getAllLogs(1000);
            }
        } catch (Exception e) {
            System.err.println("Error fetching audit logs: " + e.getMessage());
            e.printStackTrace();
            logs = new ArrayList<>();
        }
        
        if (logs == null) {
            logs = new ArrayList<>();
        }
        
        request.setAttribute("logs", logs);
        request.setAttribute("filter", filter);
        request.setAttribute("dateFrom", dateFrom);
        request.setAttribute("dateTo", dateTo);
        
        System.out.println("Showing audit logs page with " + logs.size() + " logs");
        
        request.getRequestDispatcher("/jsp/admin/audit-logs.jsp").forward(request, response);
    }
    
    private void showSystemSettings(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        System.out.println("Showing system settings page");
        request.getRequestDispatcher("/jsp/admin/settings.jsp").forward(request, response);
    }
    
    private void getSystemStatus(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("application/json");
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("database", "operational");
            status.put("storage", adminDAO.getStorageHealth());
            status.put("sessions", adminDAO.getActiveSessionCount());
            status.put("lastBackup", adminDAO.getLastBackupTime());
            status.put("uptime", adminDAO.getSystemUptime());
            status.put("timestamp", System.currentTimeMillis());
            
            String jsonResponse = gson.toJson(status);
            System.out.println("System status: " + jsonResponse);
            
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            System.err.println("Error getting system status: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    private void toggleUserStatus(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, SQLException {
        
        int userId = Integer.parseInt(request.getParameter("userId"));
        boolean activate = Boolean.parseBoolean(request.getParameter("activate"));
        
        boolean success;
        if (activate) {
            success = userDAO.activateUser(userId);
        } else {
            success = userDAO.deactivateUser(userId);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "User status updated successfully" : "Failed to update user status");
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(result));
    }
    
    private void toggleAdminStatus(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, SQLException {
        
        int userId = Integer.parseInt(request.getParameter("userId"));
        String newRole = request.getParameter("role");
        
        boolean success = userDAO.updateUserRole(userId, newRole);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        String message = success ? 
            (newRole.equals("ADMIN") ? "User is now an administrator" : "Administrator privileges removed") : 
            "Failed to update admin status";
        result.put("message", message);
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(result));
    }
    
    private void deleteUser(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, SQLException {
        
        int userId = Integer.parseInt(request.getParameter("userId"));
        
        User targetUser = userDAO.getUserById(userId);
        
        boolean success = userDAO.deleteUser(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "User deleted successfully" : "Failed to delete user");
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(result));
        
        if (success && targetUser != null) {
            System.out.println("User deleted: " + targetUser.getUsername() + " (ID: " + userId + ")");
        }
    }
    
    private void deleteFile(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, SQLException {
        
        int fileId = Integer.parseInt(request.getParameter("fileId"));
        boolean success = fileDAO.deleteFile(fileId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "File deleted successfully" : "Failed to delete file");
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(result));
    }
    
    private void clearAuditLogs(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, SQLException {
        
        String daysStr = request.getParameter("days");
        int days = daysStr != null ? Integer.parseInt(daysStr) : 30;
        
        int count = auditDAO.clearOldLogs();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", count);
        result.put("message", count + " old logs cleared");
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(result));
    }
}