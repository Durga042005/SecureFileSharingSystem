package com.securefileshare.servlets;

import com.securefileshare.models.FileMetadata;
import com.securefileshare.models.User;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.dao.AuditDAO;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.List;

public class RecentActivityServlet extends HttpServlet {
    
    private FileDAO fileDAO;
    private AuditDAO auditDAO;
    
    @Override
    public void init() throws ServletException {
        try {
            this.fileDAO = new FileDAO();
            this.auditDAO = new AuditDAO();
            System.out.println("✓ RecentFilesServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ RecentFilesServlet initialization failed: " + e.getMessage());
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
            String period = request.getParameter("period");
            
            System.out.println("\n=== RECENT FILES REQUEST ===");
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            System.out.println("Period: " + (period != null ? period : "all"));
            
            List<FileMetadata> recentFiles = fileDAO.getRecentFilesByUser(user.getUserId(), 20);
            
            List<AuditDAO.ActivityLog> recentActivity = auditDAO.getRecentActivities(user.getUserId(), 20);
            
            request.setAttribute("recentFiles", recentFiles);
            request.setAttribute("recentActivity", recentActivity);
            request.setAttribute("fileCount", recentFiles != null ? recentFiles.size() : 0);
            request.setAttribute("selectedPeriod", period);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/recent-files.jsp");
            dispatcher.forward(request, response);
            
        } catch (Exception e) {
            System.err.println("✗ RecentFilesServlet error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load recent files");
        }
    }
}