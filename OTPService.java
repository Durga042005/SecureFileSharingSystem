package com.securefileshare.services;

import javax.servlet.http.HttpSession;
import java.time.Instant;
import java.time.Duration;
import java.util.Random;
import java.util.Date;

public class OTPService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private final Random random = new Random();

    public String generateOTP(HttpSession session, String email, String purpose) {
        String otp = generateRandomOTP();
        Instant now = Instant.now();

        session.setAttribute("otpValue", otp);
        session.setAttribute("otpEmail", email);
        session.setAttribute("otpGeneratedTime", now);
        session.setAttribute("otpPurpose", purpose);

        System.out.println("DEBUG: Generated OTP " + otp + " for " + email + " at " + now + " with purpose: " + purpose);
        
        storeOTPForDisplay(session, otp, email, purpose);
        
        return otp;
    }
    
    private void storeOTPForDisplay(HttpSession session, String otp, String email, String purpose) {
        session.setAttribute("displayOTP", otp);
        session.setAttribute("displayOTPEmail", email);
        session.setAttribute("displayOTPPurpose", purpose);
        session.setAttribute("displayOTPTime", new Date().toString());
        
        Instant expiryTime = Instant.now().plus(Duration.ofMinutes(OTP_EXPIRY_MINUTES));
        session.setAttribute("displayOTPExpiry", expiryTime.toString());
        
        System.out.println("=================================================================");
        System.out.println("📱 OTP FOR IMMEDIATE USE (EMAIL MAY BE DELAYED):");
        System.out.println("=================================================================");
        System.out.println("EMAIL: " + email);
        System.out.println("OTP: " + otp);
        System.out.println("TIME: " + new Date());
        System.out.println("PURPOSE: " + purpose);
        System.out.println("EXPIRES AT: " + expiryTime);
        System.out.println("=================================================================");
        System.out.println("Use this OTP if email delivery is delayed!");
        System.out.println("=================================================================");
    }
    
    public String getDisplayOTP(HttpSession session) {
        return (String) session.getAttribute("displayOTP");
    }
    
    public boolean hasDisplayOTP(HttpSession session) {
        String otp = (String) session.getAttribute("displayOTP");
        Instant generatedTime = (Instant) session.getAttribute("otpGeneratedTime");
        
        if (otp == null || generatedTime == null) {
            return false;
        }
        
        Instant now = Instant.now();
        Duration duration = Duration.between(generatedTime, now);
        return duration.toMinutes() < OTP_EXPIRY_MINUTES;
    }

    public boolean verifyOTP(HttpSession session, String email, String enteredOtp, String purpose) {
        String sessionOTP = (String) session.getAttribute("otpValue");
        String sessionEmail = (String) session.getAttribute("otpEmail");
        String sessionPurpose = (String) session.getAttribute("otpPurpose");
        Instant generatedTime = (Instant) session.getAttribute("otpGeneratedTime");

        System.out.println("=== OTP VERIFICATION DEBUG ===");
        System.out.println("Session OTP: " + sessionOTP);
        System.out.println("Session Email: " + sessionEmail);
        System.out.println("Session Purpose: " + sessionPurpose);
        System.out.println("Entered OTP: " + enteredOtp);
        System.out.println("Entered Email: " + email);
        System.out.println("Entered Purpose: " + purpose);
        System.out.println("===============================");

        if (sessionOTP == null || sessionEmail == null || sessionPurpose == null || generatedTime == null) {
            System.out.println("DEBUG: No OTP found in session");
            return false;
        }

        if (!sessionEmail.equals(email)) {
            System.out.println("DEBUG: Email mismatch - Session: " + sessionEmail + ", Provided: " + email);
            return false;
        }

        if (!sessionPurpose.equals(purpose)) {
            System.out.println("DEBUG: Purpose mismatch - Session: " + sessionPurpose + ", Provided: " + purpose);
            return false;
        }

        Instant now = Instant.now();
        Duration duration = Duration.between(generatedTime, now);
        if (duration.toMinutes() >= OTP_EXPIRY_MINUTES) {
            System.out.println("DEBUG: OTP expired - Generated: " + generatedTime + ", Now: " + now);
            return false;
        }

        boolean isValid = sessionOTP.equals(enteredOtp);
        System.out.println("DEBUG: OTP verification result: " + isValid);
        
        return isValid;
    }

    private String generateRandomOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public void clearOTP(HttpSession session) {
        session.removeAttribute("otpValue");
        session.removeAttribute("otpEmail");
        session.removeAttribute("otpGeneratedTime");
        session.removeAttribute("otpPurpose");
        
        session.removeAttribute("displayOTP");
        session.removeAttribute("displayOTPEmail");
        session.removeAttribute("displayOTPPurpose");
        session.removeAttribute("displayOTPTime");
        session.removeAttribute("displayOTPExpiry");
        
        System.out.println("DEBUG: OTP cleared from session");
    }
}