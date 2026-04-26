package com.securefileshare.servlets;

import com.securefileshare.models.User;
import com.securefileshare.dao.UserDAO;
import com.securefileshare.dao.AuditDAO;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

public class ProfileServlet extends HttpServlet {
    
    private UserDAO userDAO;
    private AuditDAO auditDAO;
    
    @Override
    public void init() throws ServletException {
        try {
            this.userDAO = new UserDAO();
            this.auditDAO = new AuditDAO();
            System.out.println("✓ ProfileServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ ProfileServlet initialization failed: " + e.getMessage());
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
            
            System.out.println("\n=== PROFILE REQUEST ===");
            System.out.println("User: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
            
            request.setAttribute("profileUser", user);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/profile.jsp");
            dispatcher.forward(request, response);
            
        } catch (Exception e) {
            System.err.println("✗ ProfileServlet error: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load profile");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                response.setStatus(401);
                response.getWriter().write("{\"success\":false,\"message\":\"Authentication required\"}");
                return;
            }
            
            User sessionUser = (User) session.getAttribute("user");
            String action = request.getParameter("action");
            
            if ("updateProfile".equals(action)) {
                updateProfile(request, response, sessionUser);
            } else if ("changePassword".equals(action)) {
                changePassword(request, response, sessionUser);
            } else if ("updateSettings".equals(action)) {
                updateSettings(request, response, sessionUser);
            } else {
                response.setStatus(400);
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid action\"}");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Profile update error: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            try {
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void updateProfile(HttpServletRequest request, HttpServletResponse response, User sessionUser) 
            throws Exception {
        
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        
        User user = userDAO.getUserById(sessionUser.getUserId());
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        
        boolean updated = userDAO.updateUserProfile(user);
        
        if (updated) {
            sessionUser.setFullName(fullName);
            sessionUser.setEmail(email);
            sessionUser.setPhone(phone);
            
            auditDAO.logActivity(
                user.getUserId(),
                "PROFILE_UPDATE",
                "Updated profile information",
                request.getRemoteAddr(),
                "SUCCESS"
            );
            
            response.getWriter().write("{\"success\":true,\"message\":\"Profile updated successfully\"}");
        } else {
            response.getWriter().write("{\"success\":false,\"message\":\"Failed to update profile\"}");
        }
    }
    
    private void changePassword(HttpServletRequest request, HttpServletResponse response, User sessionUser) 
            throws Exception {
        
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        
        if (!newPassword.equals(confirmPassword)) {
            response.getWriter().write("{\"success\":false,\"message\":\"New passwords do not match\"}");
            return;
        }
        
        User user = userDAO.getUserById(sessionUser.getUserId());
        if (!userDAO.verifyPassword(user.getUsername(), currentPassword)) {
            auditDAO.logActivity(
                user.getUserId(),
                "PASSWORD_CHANGE_FAILED",
                "Failed password change attempt - incorrect current password",
                request.getRemoteAddr(),
                "FAILED"
            );
            response.getWriter().write("{\"success\":false,\"message\":\"Current password is incorrect\"}");
            return;
        }
        
        boolean updated = userDAO.updatePassword(user.getUserId(), newPassword);
        
        if (updated) {
            auditDAO.logActivity(
                user.getUserId(),
                "PASSWORD_CHANGE",
                "Password changed successfully",
                request.getRemoteAddr(),
                "SUCCESS"
            );
            
            response.getWriter().write("{\"success\":true,\"message\":\"Password changed successfully\"}");
        } else {
            response.getWriter().write("{\"success\":false,\"message\":\"Failed to change password\"}");
        }
    }
    
    private void updateSettings(HttpServletRequest request, HttpServletResponse response, User sessionUser) 
            throws Exception {
        
        boolean emailNotifications = "true".equals(request.getParameter("emailNotifications"));
        boolean twoFactorAuth = "true".equals(request.getParameter("twoFactorAuth"));
        boolean autoEncrypt = "true".equals(request.getParameter("autoEncrypt"));
        
        boolean updated = userDAO.updateUserSettings(
            sessionUser.getUserId(),
            emailNotifications,
            twoFactorAuth,
            autoEncrypt
        );
        
        if (updated) {
            sessionUser.setEmailNotifications(emailNotifications);
            sessionUser.setTwoFactorEnabled(twoFactorAuth);
            sessionUser.setAutoEncrypt(autoEncrypt);
            
            auditDAO.logActivity(
                sessionUser.getUserId(),
                "SETTINGS_UPDATE",
                "Updated account settings",
                request.getRemoteAddr(),
                "SUCCESS"
            );
            
            response.getWriter().write("{\"success\":true,\"message\":\"Settings updated successfully\"}");
        } else {
            response.getWriter().write("{\"success\":false,\"message\":\"Failed to update settings\"}");
        }
    }
}