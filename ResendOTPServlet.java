package com.securefileshare.servlets;

import com.securefileshare.models.User;
import com.securefileshare.services.OTPService;
import com.securefileshare.services.EmailService;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

public class ResendOTPServlet extends HttpServlet {
    
    private OTPService otpService;
    private EmailService emailService;
    
    @Override
    public void init() throws ServletException {
        otpService = new OTPService();
        emailService = new EmailService();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        User pendingUser = (User) session.getAttribute("pendingUser");
        String purpose = (String) session.getAttribute("otpPurpose");
        
        if (pendingUser == null || purpose == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        String newOTP = otpService.generateOTP(session, pendingUser.getEmail(), purpose);
        
        try {
            emailService.sendOTPEmail(
                pendingUser.getEmail(), 
                pendingUser.getUsername(), 
                newOTP, 
                purpose
            );
            System.out.println("DEBUG: New OTP " + newOTP + " sent to " + pendingUser.getEmail());
        } catch (Exception e) {
            System.err.println("DEBUG: Email sending failed, but OTP is available on screen: " + newOTP);
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
    }
}