package com.securefileshare.servlets;

import com.securefileshare.models.FileMetadata;
import com.securefileshare.models.User;
import com.securefileshare.models.FileShare;
import com.securefileshare.dao.FileDAO;
import com.securefileshare.dao.UserDAO;
import com.securefileshare.dao.ShareDAO;
import com.securefileshare.dao.AuditDAO;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.json.JSONObject;

public class SharedFilesServlet extends HttpServlet {
    
    private FileDAO fileDAO;
    private UserDAO userDAO;
    private ShareDAO shareDAO;
    private AuditDAO auditDAO;
    
    @Override
    public void init() throws ServletException {
        try {
            this.fileDAO = new FileDAO();
            this.userDAO = new UserDAO();
            this.shareDAO = new ShareDAO();
            this.auditDAO = new AuditDAO();
            System.out.println("✓ SharedFilesServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ SharedFilesServlet initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
                return;
            }
            
            User user = (User) session.getAttribute("user");
            String action = request.getParameter("action");
            
            List<FileMetadata> userFiles = fileDAO.getFilesByUserId(user.getUserId());
            request.setAttribute("userFiles", userFiles);
            
            if ("view".equals(action)) {
                handleViewSharedFile(request, response, user);
            } else if ("shared-with-me".equals(action)) {
                handleSharedWithMe(request, response, user);
            } else if ("shared-by-me".equals(action)) {
                handleSharedByMe(request, response, user);
            } else if ("share-details".equals(action)) {
                handleShareDetails(request, response, user);
            } else {
                handleSharedFilesDashboard(request, response, user);
            }
            
        } catch (Exception e) {
            System.err.println("✗ SharedFilesServlet error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load shared files");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();
        
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Authentication required");
                jsonResponse.put("errorCode", "AUTH_REQUIRED");
                response.setStatus(401);
                out.print(jsonResponse.toString());
                return;
            }
            
            User user = (User) session.getAttribute("user");
            String action = request.getParameter("action");
            
            if ("share".equals(action)) {
                handleShareFile(request, response, user, jsonResponse);
            } else if ("update-permissions".equals(action)) {
                handleUpdatePermissions(request, response, user, jsonResponse);
            } else if ("revoke".equals(action)) {
                handleRevokeShare(request, response, user, jsonResponse);
            } else if ("generate-link".equals(action)) {
                handleGenerateShareLink(request, response, user, jsonResponse);
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "Invalid action");
                response.setStatus(400);
                out.print(jsonResponse.toString());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Error: " + e.getMessage());
            response.setStatus(500);
            out.print(jsonResponse.toString());
        }
    }

    private void handleSharedFilesDashboard(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        
        List<FileShare> sharedByMe = shareDAO.getSharesByOwner(user.getUserId());
        List<FileShare> sharedWithMe = shareDAO.getSharesByRecipient(user.getUserId());
        
        if (sharedByMe != null) {
            for (FileShare share : sharedByMe) {
                FileMetadata file = fileDAO.getFileMetadata(share.getFileId());
                share.setFile(file);
                if (share.getRecipientEmail() != null && !share.getRecipientEmail().isEmpty()) {
                    User recipient = userDAO.getUserByEmail(share.getRecipientEmail());
                    share.setRecipient(recipient);
                }
            }
        }
        
        if (sharedWithMe != null) {
            for (FileShare share : sharedWithMe) {
                FileMetadata file = fileDAO.getFileMetadata(share.getFileId());
                share.setFile(file);
                User owner = userDAO.getUserById(share.getOwnerId());
                share.setOwner(owner);
            }
        }
        
        request.setAttribute("sharedByMe", sharedByMe);
        request.setAttribute("sharedWithMe", sharedWithMe);
        request.setAttribute("sharedByMeCount", sharedByMe != null ? sharedByMe.size() : 0);
        request.setAttribute("sharedWithMeCount", sharedWithMe != null ? sharedWithMe.size() : 0);
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/shared-files.jsp");
        dispatcher.forward(request, response);
    }

    private void handleShareFile(HttpServletRequest request, HttpServletResponse response, User user, JSONObject jsonResponse) 
            throws Exception {
        
        int fileId = Integer.parseInt(request.getParameter("fileId"));
        String recipientEmail = request.getParameter("recipientEmail");
        String permission = request.getParameter("permission");
        String expiryStr = request.getParameter("expiryDate");
        boolean sendEmail = "true".equals(request.getParameter("sendEmail"));
        
        FileMetadata file = fileDAO.getFileMetadata(fileId);
        if (file == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "File not found");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        if (file.getUserId() != user.getUserId()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "You don't have permission to share this file");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        User recipient = userDAO.getUserByEmail(recipientEmail);
        if (recipient == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "User with email " + recipientEmail + " not found");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        if (recipient.getUserId() == user.getUserId()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "You cannot share files with yourself");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        if (shareDAO.isFileSharedWithEmail(fileId, recipientEmail)) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "File already shared with this user");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setOwnerId(user.getUserId());
        share.setRecipientEmail(recipientEmail);
        
        String dbPermission = mapPermissionToDB(permission);
        share.setPermission(dbPermission);
        
        share.setShareToken(UUID.randomUUID().toString());
        share.setActive(true);
        share.setDownloadCount(0);
        share.setMaxDownloads(0);
        
        if (expiryStr != null && !expiryStr.isEmpty()) {
            share.setExpiryDate(Timestamp.valueOf(expiryStr + " 23:59:59"));
        }
        
        boolean success = shareDAO.createShare(share);
        
        if (success) {
            auditDAO.logActivity(
                user.getUserId(),
                "SHARE",
                "Shared file: " + file.getOriginalFilename() + " with " + recipientEmail,
                request.getRemoteAddr(),
                "SUCCESS"
            );
            
            if (sendEmail) {
                sendShareNotification(user, recipient, file, share);
            }
            
            jsonResponse.put("success", true);
            jsonResponse.put("message", "File shared successfully with " + recipientEmail);
            jsonResponse.put("shareId", share.getShareId());
            jsonResponse.put("shareToken", share.getShareToken());
        } else {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Failed to share file");
        }
        
        response.getWriter().write(jsonResponse.toString());
    }

    private void handleGenerateShareLink(HttpServletRequest request, HttpServletResponse response, User user, JSONObject jsonResponse) 
            throws Exception {
        
        int fileId = Integer.parseInt(request.getParameter("fileId"));
        String permission = request.getParameter("permission");
        String expiryStr = request.getParameter("expiryDate");
        
        FileMetadata file = fileDAO.getFileMetadata(fileId);
        if (file == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "File not found");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        if (file.getUserId() != user.getUserId()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "You don't have permission to share this file");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setOwnerId(user.getUserId());
        share.setRecipientEmail(null);
        share.setPermission(mapPermissionToDB(permission));
        share.setShareToken(UUID.randomUUID().toString());
        share.setActive(true);
        share.setDownloadCount(0);
        share.setMaxDownloads(0);
        
        if (expiryStr != null && !expiryStr.isEmpty()) {
            share.setExpiryDate(Timestamp.valueOf(expiryStr + " 23:59:59"));
        }
        
        boolean success = shareDAO.createShare(share);
        
        if (success) {
            String shareLink = request.getContextPath() + "/shared?action=view&token=" + share.getShareToken();
            
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Share link generated successfully");
            jsonResponse.put("shareLink", shareLink);
            jsonResponse.put("shareToken", share.getShareToken());
            jsonResponse.put("expiryDate", share.getExpiryDate() != null ? share.getExpiryDate().toString() : "No expiry");
        } else {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Failed to generate share link");
        }
        
        response.getWriter().write(jsonResponse.toString());
    }

    private void handleViewSharedFile(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        
        String token = request.getParameter("token");
        
        try {
            FileShare share = shareDAO.getShareByToken(token);
            
            if (share == null || !share.isActive()) {
                request.setAttribute("error", "Invalid or expired share link");
                request.getRequestDispatcher("/jsp/user/share-error.jsp").forward(request, response);
                return;
            }
            
            if (share.getExpiryDate() != null && share.getExpiryDate().before(new Timestamp(System.currentTimeMillis()))) {
                request.setAttribute("error", "This share link has expired");
                request.getRequestDispatcher("/jsp/user/share-error.jsp").forward(request, response);
                return;
            }
            
            if (share.hasReachedDownloadLimit()) {
                request.setAttribute("error", "This share link has reached its maximum download limit");
                request.getRequestDispatcher("/jsp/user/share-error.jsp").forward(request, response);
                return;
            }
            
            if (share.getRecipientEmail() != null && !share.getRecipientEmail().isEmpty()) {
                if (user == null) {
                    response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp?redirect=shared&token=" + token);
                    return;
                }
                
                if (!share.getRecipientEmail().equals(user.getEmail()) && share.getOwnerId() != user.getUserId()) {
                    request.setAttribute("error", "You don't have permission to access this file");
                    request.getRequestDispatcher("/jsp/user/share-error.jsp").forward(request, response);
                    return;
                }
            }
            
            FileMetadata file = fileDAO.getFileMetadata(share.getFileId());
            
            request.setAttribute("file", file);
            request.setAttribute("share", share);
            request.setAttribute("permission", share.getPermission());
            
            request.getRequestDispatcher("/jsp/user/view-shared-file.jsp").forward(request, response);
            
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Error accessing shared file");
            request.getRequestDispatcher("/jsp/user/share-error.jsp").forward(request, response);
        }
    }

    private void handleUpdatePermissions(HttpServletRequest request, HttpServletResponse response, User user, JSONObject jsonResponse) 
            throws Exception {
        
        String shareId = request.getParameter("shareId");
        String permission = request.getParameter("permission");
        
        FileShare share = shareDAO.getShareById(shareId);
        
        if (share == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Share record not found");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        if (share.getOwnerId() != user.getUserId()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "You don't have permission to modify this share");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        boolean success = shareDAO.updateSharePermission(shareId, mapPermissionToDB(permission));
        
        if (success) {
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Permissions updated successfully");
        } else {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Failed to update permissions");
        }
        
        response.getWriter().write(jsonResponse.toString());
    }

    private void handleRevokeShare(HttpServletRequest request, HttpServletResponse response, User user, JSONObject jsonResponse) 
            throws Exception {
        
        String shareId = request.getParameter("shareId");
        
        FileShare share = shareDAO.getShareById(shareId);
        
        if (share == null) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Share record not found");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        if (share.getOwnerId() != user.getUserId()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "You don't have permission to revoke this share");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        boolean success = shareDAO.revokeShare(shareId);
        
        if (success) {
            FileMetadata file = fileDAO.getFileMetadata(share.getFileId());
            
            auditDAO.logActivity(
                user.getUserId(),
                "REVOKE_SHARE",
                "Revoked access to file: " + (file != null ? file.getOriginalFilename() : "Unknown"),
                request.getRemoteAddr(),
                "SUCCESS"
            );
            
            jsonResponse.put("success", true);
            jsonResponse.put("message", "Share access revoked successfully");
        } else {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "Failed to revoke share access");
        }
        
        response.getWriter().write(jsonResponse.toString());
    }

    private void handleSharedWithMe(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        
        List<FileShare> sharedWithMe = shareDAO.getSharesByRecipient(user.getUserId());
        
        if (sharedWithMe != null) {
            for (FileShare share : sharedWithMe) {
                FileMetadata file = fileDAO.getFileMetadata(share.getFileId());
                share.setFile(file);
                User owner = userDAO.getUserById(share.getOwnerId());
                share.setOwner(owner);
            }
        }
        
        request.setAttribute("sharedFiles", sharedWithMe);
        request.setAttribute("fileCount", sharedWithMe != null ? sharedWithMe.size() : 0);
        request.setAttribute("viewType", "shared-with-me");
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/shared-files.jsp");
        dispatcher.forward(request, response);
    }

    private void handleSharedByMe(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        
        List<FileShare> sharedByMe = shareDAO.getSharesByOwner(user.getUserId());
        
        if (sharedByMe != null) {
            for (FileShare share : sharedByMe) {
                FileMetadata file = fileDAO.getFileMetadata(share.getFileId());
                share.setFile(file);
                if (share.getRecipientEmail() != null && !share.getRecipientEmail().isEmpty()) {
                    User recipient = userDAO.getUserByEmail(share.getRecipientEmail());
                    share.setRecipient(recipient);
                }
            }
        }
        
        request.setAttribute("sharedFiles", sharedByMe);
        request.setAttribute("fileCount", sharedByMe != null ? sharedByMe.size() : 0);
        request.setAttribute("viewType", "shared-by-me");
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/shared-files.jsp");
        dispatcher.forward(request, response);
    }

    private void handleShareDetails(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        
        String shareId = request.getParameter("shareId");
        
        FileShare share = shareDAO.getShareById(shareId);
        
        if (share == null) {
            request.setAttribute("error", "Share record not found");
            request.getRequestDispatcher("/jsp/user/shared-files.jsp").forward(request, response);
            return;
        }
        
        if (share.getOwnerId() != user.getUserId() && 
            (share.getRecipientEmail() == null || !share.getRecipientEmail().equals(user.getEmail()))) {
            request.setAttribute("error", "You don't have permission to view this share");
            request.getRequestDispatcher("/jsp/user/shared-files.jsp").forward(request, response);
            return;
        }
        
        FileMetadata file = fileDAO.getFileMetadata(share.getFileId());
        User owner = userDAO.getUserById(share.getOwnerId());
        User recipient = share.getRecipientEmail() != null ? userDAO.getUserByEmail(share.getRecipientEmail()) : null;
        
        share.setFile(file);
        share.setOwner(owner);
        share.setRecipient(recipient);
        
        request.setAttribute("share", share);
        request.setAttribute("file", file);
        request.setAttribute("owner", owner);
        request.setAttribute("recipient", recipient);
        
        request.getRequestDispatcher("/jsp/user/share-details.jsp").forward(request, response);
    }

    private String mapPermissionToDB(String uiPermission) {
        if (uiPermission == null) return "VIEW";
        switch (uiPermission) {
            case "READ":
            case "VIEW":
                return "VIEW";
            case "DOWNLOAD":
                return "DOWNLOAD";
            case "WRITE":
            case "EDIT":
                return "EDIT";
            default:
                return "VIEW";
        }
    }
    
    private void sendShareNotification(User sharer, User recipient, FileMetadata file, FileShare share) {
        System.out.println("Email notification would be sent to: " + recipient.getEmail());
        System.out.println("File: " + file.getOriginalFilename() + " shared by: " + sharer.getUsername());
        System.out.println("Share token: " + share.getShareToken());
        System.out.println("Share link: " + "/shared?action=view&token=" + share.getShareToken());
    }
}