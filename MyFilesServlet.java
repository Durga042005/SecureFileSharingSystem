package com.securefileshare.servlets;

import com.securefileshare.models.FileMetadata;
import com.securefileshare.models.User;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.services.CloudStorageService;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.List;

public class MyFilesServlet extends HttpServlet {
    
    private FileDAO fileDAO;
    private CloudStorageService cloudStorageService;
    
    @Override
    public void init() throws ServletException {
        try {
            this.fileDAO = new FileDAO();
            this.cloudStorageService = CloudStorageService.getInstance();
            System.out.println("✓ MyFilesServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ MyFilesServlet initialization failed: " + e.getMessage());
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
            
            System.out.println("\n=== MY FILES REQUEST ===");
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            
            List<FileMetadata> files = fileDAO.getFilesByUserId(user.getUserId());
            
            long totalBytes = 0;
            if (files != null) {
                for (FileMetadata file : files) {
                    totalBytes += file.getFileSize();
                }
            }
            
            request.setAttribute("files", files);
            request.setAttribute("fileCount", files != null ? files.size() : 0);
            request.setAttribute("totalStorage", formatFileSize(totalBytes));
            request.setAttribute("cloudStorageAvailable", cloudStorageService.isServiceAvailable());
            
            System.out.println("Found " + (files != null ? files.size() : 0) + " files");
            System.out.println("Total storage: " + formatFileSize(totalBytes));
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/myfiles.jsp");
            dispatcher.forward(request, response);
            
        } catch (Exception e) {
            System.err.println("✗ MyFilesServlet error: " + e.getMessage());
            e.printStackTrace();
            
            request.setAttribute("error", "Failed to load files: " + e.getMessage());
            RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/myfiles.jsp");
            dispatcher.forward(request, response);
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}