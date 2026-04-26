package com.securefileshare.servlets;

import com.securefileshare.models.User;
import com.securefileshare.services.OTPService;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;

public class OTPVerificationServlet extends HttpServlet {
    
    private OTPService otpService;
    
    @Override
    public void init() throws ServletException {
        otpService = new OTPService();
        System.out.println("✓ OTPVerificationServlet initialized successfully");
        System.out.println("✓ OTPVerificationServlet mapped to: /verify-otp");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        System.out.println("\n=== OTP VERIFICATION REQUEST ===");
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("Context Path: " + request.getContextPath());
        System.out.println("Method: " + request.getMethod());
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("✗ No session found");
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        User pendingUser = (User) session.getAttribute("pendingUser");
        String otpPurpose = (String) session.getAttribute("otpPurpose");
        String enteredOTP = request.getParameter("otp");
        
        System.out.println("Pending User: " + (pendingUser != null ? pendingUser.getUsername() : "null"));
        System.out.println("User Role: " + (pendingUser != null ? pendingUser.getRole() : "null"));
        System.out.println("OTP Purpose: " + otpPurpose);
        System.out.println("Entered OTP: " + enteredOTP);
        
        if (pendingUser == null || otpPurpose == null) {
            System.out.println("✗ No pending user or purpose");
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        boolean isValid = otpService.verifyOTP(session, pendingUser.getEmail(), enteredOTP, otpPurpose);
        
        if (isValid) {
            System.out.println("✓ OTP is valid!");
            
            // Set the user in session
            session.setAttribute("user", pendingUser);
            session.removeAttribute("pendingUser");
            session.removeAttribute("otpPurpose");
            
            System.out.println("✓ User set in session: " + pendingUser.getUsername());
            System.out.println("✓ User role: " + pendingUser.getRole());
            
            // Determine redirect URL based on role
            String redirectURL;
            if ("ADMIN".equals(pendingUser.getRole())) {
                redirectURL = request.getContextPath() + "/admin";
                System.out.println("→ Admin user detected, redirecting to: " + redirectURL);
            } else {
                redirectURL = request.getContextPath() + "/dashboard";
                System.out.println("→ Regular user detected, redirecting to: " + redirectURL);
            }
            
            // Log successful login
            System.out.println("✓ Login successful for user: " + pendingUser.getUsername());
            System.out.println("✓ Redirecting to: " + redirectURL);
            
            response.sendRedirect(redirectURL);
            
        } else {
            System.out.println("✗ OTP is invalid!");
            request.setAttribute("error", "Invalid or expired OTP. Please try again.");
            RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/auth/otp-verification.jsp");
            dispatcher.forward(request, response);
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>OTP Verification Servlet</h1>");
        out.println("<p>This servlet is working correctly!</p>");
        out.println("<p>Context Path: " + request.getContextPath() + "</p>");
        out.println("<p>Servlet Path: " + request.getServletPath() + "</p>");
        out.println("<p>Use POST method to submit OTP</p>");
        out.println("</body></html>");
    }
}