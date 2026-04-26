package com.securefileshare.servlets;

import com.securefileshare.dao.AuditDAO;
import com.securefileshare.models.User;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

public class LogoutServlet extends HttpServlet {
    
    private AuditDAO auditDAO;
    
    @Override
    public void init() throws ServletException {
        try {
            this.auditDAO = new AuditDAO();
            System.out.println("✓ LogoutServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("✗ LogoutServlet initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        
        try {
            if (session != null) {
                User user = (User) session.getAttribute("user");
                if (user != null) {
                    auditDAO.logActivity(
                        user.getUserId(),
                        "LOGOUT",
                        "User logged out successfully",
                        request.getRemoteAddr(),
                        "SUCCESS"
                    );
                    System.out.println("User logged out: " + user.getUsername());
                }
                
                session.invalidate();
            }
            
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp?logout=success");
            
        } catch (Exception e) {
            System.err.println("✗ Logout error: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp?error=logout_failed");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}