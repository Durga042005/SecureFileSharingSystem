package com.securefileshare.servlets;

import com.securefileshare.models.User;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.dao.AuditDAO;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.List;

public class SecurityServlet extends HttpServlet {
    
    private FileDAO fileDAO;
    private AuditDAO auditDAO;
    
    @Override
    public void init() throws ServletException {
        try {
            this.fileDAO = new FileDAO();
            this.auditDAO = new AuditDAO();
            System.out.println("✓ SecurityServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ SecurityServlet initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                response.sendRedirect(request.getContextPath() + "/jsp/login.jsp");
                return;
            }
            
            User user = (User) session.getAttribute("user");
            
            System.out.println("\n=== SECURITY DASHBOARD REQUEST ===");
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            
            int totalFiles = fileDAO.getFileCountByUser(user.getUserId());
            int encryptedFiles = fileDAO.getEncryptedFileCountByUser(user.getUserId());
            int encryptionPercentage = totalFiles > 0 ? (encryptedFiles * 100 / totalFiles) : 0;
            
            List<AuditDAO.ActivityLog> securityEvents = auditDAO.getRecentActivitiesByAction(
                user.getUserId(), 
                new String[]{"LOGIN", "LOGIN_FAILED", "DOWNLOAD", "UPLOAD", "DELETE"}, 
                20
            );
            
            request.setAttribute("totalFiles", totalFiles);
            request.setAttribute("encryptedFiles", encryptedFiles);
            request.setAttribute("encryptionPercentage", encryptionPercentage);
            request.setAttribute("securityEvents", securityEvents);
            request.setAttribute("securityScore", calculateSecurityScore(user, totalFiles, encryptedFiles));
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/security.jsp");
            dispatcher.forward(request, response);
            
        } catch (Exception e) {
            System.err.println("✗ SecurityServlet error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load security dashboard");
        }
    }
    
    private int calculateSecurityScore(User user, int totalFiles, int encryptedFiles) {
        int score = 0;
        
        if (user != null) {
            score += 20;
        }
        
        if (totalFiles > 0) {
            score += (encryptedFiles * 40) / totalFiles;
        } else {
            score += 40;
        }
        
        score += 20;
        
        return Math.min(score, 100);
    }
}