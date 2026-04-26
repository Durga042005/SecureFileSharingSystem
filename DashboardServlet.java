package com.securefileshare.servlets;

import com.securefileshare.dao.AuditDAO;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.models.FileMetadata;
import com.securefileshare.models.User;
import com.securefileshare.models.Activity;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/dashboard")  // Make sure this is present
public class DashboardServlet extends HttpServlet {
    
    private FileDAO fileDAO;
    private AuditDAO auditDAO;
    
    @Override
    public void init() throws ServletException {
        try {
            fileDAO = new FileDAO();
            auditDAO = new AuditDAO();
            System.out.println("DashboardServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize DashboardServlet: " + e.getMessage());
            throw new ServletException(e);
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        User user = (User) session.getAttribute("user");
        System.out.println("Loading dashboard for user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
        
        try {
            if ("ADMIN".equals(user.getRole())) {
                response.sendRedirect(request.getContextPath() + "/admin");
                return;
            }
            
            List<FileMetadata> userFiles;
            try {
                userFiles = fileDAO.getFilesByUserId(user.getUserId());
                System.out.println("Retrieved " + userFiles.size() + " files for user " + user.getUsername());
            } catch (Exception e) {
                System.err.println("Database error: " + e.getMessage());
                userFiles = new ArrayList<>();
            }
            
            int totalFiles = userFiles.size();
            int encryptedFiles = 0;
            long totalStorageUsed = 0;
            
            for (FileMetadata file : userFiles) {
                totalStorageUsed += file.getFileSize();
                if (file.getEncryptionKey() != null && !file.getEncryptionKey().isEmpty()) {
                    encryptedFiles++;
                }
            }
            
            double storageUsedGB = totalStorageUsed / (1024.0 * 1024.0 * 1024.0);
            double storagePercent = Math.min(100, (storageUsedGB / 10) * 100);
            
            // Get download count for this specific user
            int downloadCount = 0;
            int todayDownloads = 0;
            try {
                downloadCount = auditDAO.getUserDownloadCount(user.getUserId());
                todayDownloads = auditDAO.getUserTodayDownloadCount(user.getUserId());
                System.out.println("Download count for user " + user.getUsername() + ": " + downloadCount);
                System.out.println("Today's downloads for user " + user.getUsername() + ": " + todayDownloads);
            } catch (Exception e) {
                System.err.println("Error getting download count: " + e.getMessage());
                e.printStackTrace();
            }
            
            List<FileMetadata> recentFiles = userFiles.size() > 5 ? 
                userFiles.subList(0, 5) : userFiles;
            
            List<Activity> recentActivity = getRecentActivity(user.getUserId());
            
            request.setAttribute("user", user);
            request.setAttribute("uploadCount", totalFiles);
            request.setAttribute("downloadCount", downloadCount);
            request.setAttribute("todayDownloads", todayDownloads);
            request.setAttribute("encryptedFiles", encryptedFiles);
            request.setAttribute("storageUsed", String.format("%.2f", storageUsedGB));
            request.setAttribute("storagePercent", (int) storagePercent);
            request.setAttribute("securityScore", calculateSecurityScore(userFiles, encryptedFiles));
            request.setAttribute("fileCount", totalFiles);
            request.setAttribute("notifications", getNotificationCount(user.getUserId()));
            request.setAttribute("lastLogin", getLastLogin(user.getUserId()));
            request.setAttribute("lastLoginIP", getLastLoginIP(user.getUserId()));
            request.setAttribute("recentFiles", recentFiles);
            request.setAttribute("recentActivity", recentActivity);
            
            if (session.getAttribute("encryptionPassword") != null) {
                request.setAttribute("encryptionPassword", session.getAttribute("encryptionPassword"));
                session.removeAttribute("encryptionPassword");
            }
            
            if (session.getAttribute("message") != null) {
                request.setAttribute("message", session.getAttribute("message"));
                request.setAttribute("messageType", session.getAttribute("messageType"));
                session.removeAttribute("message");
                session.removeAttribute("messageType");
            }
            
            System.out.println("Forwarding to dashboard.jsp");
            System.out.println("Statistics: " + totalFiles + " files, " + 
                             encryptedFiles + " encrypted, " + 
                             String.format("%.2f", storageUsedGB) + " GB used, " +
                             downloadCount + " total downloads, " + 
                             todayDownloads + " today");
            
            request.getRequestDispatcher("/jsp/user/dashboard.jsp").forward(request, response);
            
        } catch (Exception e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
            request.setAttribute("error", "Error loading dashboard: " + e.getMessage());
            request.getRequestDispatcher("/jsp/error/error.jsp").forward(request, response);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
    
    private List<Activity> getRecentActivity(int userId) {
        List<Activity> activities = new ArrayList<>();
        
        try {
            activities = auditDAO.getRecentUserActivity(userId, 10);
            System.out.println("Retrieved " + activities.size() + " activities for user " + userId);
            
            if (activities.isEmpty()) {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                Timestamp oneHourAgo = new Timestamp(System.currentTimeMillis() - 3600000);
                Timestamp twoHoursAgo = new Timestamp(System.currentTimeMillis() - 7200000);
                
                activities.add(new Activity(0, userId, "SYSTEM", "Welcome to Secure File Sharing System!", now));
                activities.add(new Activity(0, userId, "UPLOAD", "Your account has been activated", oneHourAgo));
                activities.add(new Activity(0, userId, "SECURITY", "Two-factor authentication available", twoHoursAgo));
            }
            
        } catch (Exception e) {
            System.err.println("Error loading activities: " + e.getMessage());
            e.printStackTrace();
            Timestamp now = new Timestamp(System.currentTimeMillis());
            activities.add(new Activity(0, userId, "SYSTEM", "System initialized", now));
        }
        
        return activities;
    }
    
    private int calculateSecurityScore(List<FileMetadata> files, int encryptedCount) {
        if (files.isEmpty()) return 100;
        
        double percentEncrypted = (encryptedCount * 100.0) / files.size();
        
        int encryptionScore = (int) (percentEncrypted * 0.7);
        int fileCountScore = Math.min(files.size() * 2, 15);
        int defaultScore = 15;
        
        int totalScore = encryptionScore + fileCountScore + defaultScore;
        
        return Math.min(100, Math.max(0, totalScore));
    }
    
    private int getNotificationCount(int userId) {
        try {
            return auditDAO.getUnreadNotifications(userId);
        } catch (Exception e) {
            System.err.println("Error getting notification count: " + e.getMessage());
            return 0;
        }
    }
    
    private String getLastLogin(int userId) {
        try {
            String lastLogin = auditDAO.getLastLogin(userId);
            if (lastLogin != null) {
                return lastLogin;
            }
            return "Never logged in";
        } catch (Exception e) {
            System.err.println("Error getting last login: " + e.getMessage());
            return "Unknown";
        }
    }
    
    private String getLastLoginIP(int userId) {
        try {
            String lastLoginIP = auditDAO.getLastLoginIP(userId);
            return lastLoginIP != null ? lastLoginIP : "Unknown";
        } catch (Exception e) {
            System.err.println("Error getting last login IP: " + e.getMessage());
            return "Unknown";
        }
    }
}