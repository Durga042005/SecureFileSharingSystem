package com.securefileshare.servlets;

import com.securefileshare.models.User;
import com.securefileshare.services.CloudStorageService;
import com.securefileshare.services.GoogleDriveService;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

public class GoogleDriveServlet extends HttpServlet {
    
    private CloudStorageService cloudStorageService;
    private GoogleDriveService googleDriveService;
    
    @Override
    public void init() throws ServletException {
        try {
            this.cloudStorageService = CloudStorageService.getInstance();
            this.googleDriveService = GoogleDriveService.getInstance();
            System.out.println("✓ GoogleDriveServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ GoogleDriveServlet initialization failed: " + e.getMessage());
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
            
            System.out.println("\n=== GOOGLE DRIVE REQUEST ===");
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            
            boolean isAvailable = cloudStorageService.isServiceAvailable();
            String connectionStatus = cloudStorageService.testConnection();
            
            request.setAttribute("isAvailable", isAvailable);
            request.setAttribute("connectionStatus", connectionStatus);
            request.setAttribute("storageUsed", "1.62 GB");
            request.setAttribute("storageTotal", "15 GB");
            request.setAttribute("storagePercent", "10");
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/google-drive.jsp");
            dispatcher.forward(request, response);
            
        } catch (Exception e) {
            System.err.println("✗ GoogleDriveServlet error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load Google Drive info");
        }
    }
}