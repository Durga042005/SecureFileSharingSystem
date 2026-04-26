package com.securefileshare.services;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {
    
    private static final String SMTP_HOST = "smtp.sendgrid.net";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USERNAME = "apikey";
    private static final String SMTP_PASSWORD = "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";
    private static final String FROM_EMAIL = "securefilesharee@gmail.com";
    private static final String FROM_NAME = "Secure File Sharing System";
    
    private static EmailService instance;
    
    public EmailService() {}
    
    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }
    
    public boolean sendOTPEmail(String toEmail, String username, String otp, String purpose) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
                }
            });
            
            session.setDebug(true);
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, 
                InternetAddress.parse(toEmail));
            message.setSubject("OTP Verification - Secure File Sharing System");
            
            String emailBody = "Hello " + username + ",\n\n" +
                "Your OTP for " + purpose + " is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "Regards,\nSecure File Sharing System";
            
            message.setText(emailBody);
            
            System.out.println("========================================");
            System.out.println("📧 SENDING REAL OTP EMAIL VIA SENDGRID");
            System.out.println("========================================");
            System.out.println("From: " + FROM_EMAIL);
            System.out.println("To: " + toEmail);
            System.out.println("User: " + username);
            System.out.println("OTP: " + otp);
            System.out.println("========================================");
            
            Transport.send(message);
            
            System.out.println("✅ REAL OTP email sent successfully via SendGrid!");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ FAILED to send REAL email via SendGrid");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}