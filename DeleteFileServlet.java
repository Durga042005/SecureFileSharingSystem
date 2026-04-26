package com.securefileshare.servlets;

import com.securefileshare.models.FileMetadata;
import com.securefileshare.models.User;
import com.securefileshare.services.CloudStorageService;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.dao.AuditDAO;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import org.json.JSONObject;

public class DeleteFileServlet extends HttpServlet {
    
    private FileDAO fileDAO;
    private CloudStorageService cloudStorageService;
    private AuditDAO auditDAO;
    
    @Override
    public void init() throws ServletException {
        try {
            this.fileDAO = new FileDAO();
            this.cloudStorageService = CloudStorageService.getInstance();
            this.auditDAO = new AuditDAO();
            System.out.println("✓ DeleteFileServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ DeleteFileServlet initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject jsonResponse = new JSONObject();
        
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Authentication required");
                response.setStatus(401);
                response.getWriter().write(jsonResponse.toString());
                return;
            }
            
            User user = (User) session.getAttribute("user");
            String fileIdParam = request.getParameter("fileId");
            
            if (fileIdParam == null || fileIdParam.isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "File ID is required");
                response.getWriter().write(jsonResponse.toString());
                return;
            }
            
            int fileId = Integer.parseInt(fileIdParam);
            
            FileMetadata metadata = fileDAO.getFileMetadata(fileId);
            
            if (metadata == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "File not found");
                response.getWriter().write(jsonResponse.toString());
                return;
            }
            
            if (metadata.getUserId() != user.getUserId()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "You don't have permission to delete this file");
                response.getWriter().write(jsonResponse.toString());
                return;
            }
            
            boolean cloudDeleted = true;
            if (metadata.getCloudFileId() != null && !metadata.getCloudFileId().isEmpty()) {
                try {
                    cloudDeleted = cloudStorageService.deleteFile(metadata.getCloudFileId());
                } catch (Exception e) {
                    System.err.println("Failed to delete from Google Drive: " + e.getMessage());
                    cloudDeleted = false;
                }
            }
            
            boolean dbDeleted = fileDAO.deleteFile(fileId);
            
            if (dbDeleted) {
                auditDAO.logActivity(
                    user.getUserId(),
                    "DELETE",
                    "Deleted file: " + metadata.getOriginalFilename() + 
                    (cloudDeleted ? "" : " (Cloud file may remain)"),
                    request.getRemoteAddr(),
                    cloudDeleted ? "SUCCESS" : "PARTIAL"
                );
                
                jsonResponse.put("success", true);
                jsonResponse.put("message", cloudDeleted ? 
                    "File deleted successfully" : 
                    "File deleted from database but cloud file may remain");
                jsonResponse.put("cloudDeleted", cloudDeleted);
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Failed to delete file from database");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Delete file error: " + e.getMessage());
            e.printStackTrace();
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Delete failed: " + e.getMessage());
        }
        
        response.getWriter().write(jsonResponse.toString());
    }
}