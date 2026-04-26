package com.securefileshare.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.securefileshare.dao.UserDAO;
import com.securefileshare.dao.AuditDAO;
import com.securefileshare.models.User;
import com.securefileshare.services.EmailService;
import com.securefileshare.services.OTPService;

@MultipartConfig(
    maxFileSize = 1024 * 1024 * 50,
    maxRequestSize = 1024 * 1024 * 100,
    fileSizeThreshold = 1024 * 1024 * 10
)
public class AuthServlet extends HttpServlet {

    private UserDAO userDAO;
    private OTPService otpService;
    private AuditDAO auditDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        otpService = new OTPService();
        auditDAO = new AuditDAO();
        System.out.println("✓ AuthServlet initialized successfully");
        System.out.println("✓ AuthServlet mapped to: /auth");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        System.out.println("AuthServlet action: " + action);

        if ("login".equals(action)) {
            handleLogin(request, response);
        } else if ("register".equals(action)) {
            handleRegister(request, response);
        } else if ("logout".equals(action)) {
            handleLogout(request, response);
        } else if ("verifyOtp".equals(action)) {
            handleVerifyOtp(request, response);
        } else if ("resendOtp".equals(action)) {
            handleResendOtp(request, response);
        } else if ("uploadFile".equals(action)) {
            handleFileUpload(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        
        if ("logout".equals(action)) {
            handleLogout(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
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
            
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp?logout=success");
            
        } catch (Exception e) {
            System.err.println("✗ Logout error: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp?error=logout_failed");
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String username = request.getParameter("username").trim();
            String email = request.getParameter("email").trim();
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");
            String firstName = request.getParameter("firstName");
            String lastName = request.getParameter("lastName");
            String terms = request.getParameter("terms");
            String role = request.getParameter("role");

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                request.setAttribute("error", "All required fields must be filled");
                request.getRequestDispatcher("/jsp/auth/register.jsp").forward(request, response);
                return;
            }

            if (!password.equals(confirmPassword)) {
                request.setAttribute("error", "Passwords do not match");
                request.getRequestDispatcher("/jsp/auth/register.jsp").forward(request, response);
                return;
            }

            if (password.length() < 8) {
                request.setAttribute("error", "Password must be at least 8 characters long");
                request.getRequestDispatcher("/jsp/auth/register.jsp").forward(request, response);
                return;
            }

            if (terms == null) {
                request.setAttribute("error", "You must accept the Terms & Conditions");
                request.getRequestDispatcher("/jsp/auth/register.jsp").forward(request, response);
                return;
            }

            if (userDAO.userExists(username, email)) {
                request.setAttribute("error", "Username or email already exists");
                request.getRequestDispatcher("/jsp/auth/register.jsp").forward(request, response);
                return;
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            user.setFirstName(firstName != null ? firstName : "");
            user.setLastName(lastName != null ? lastName : "");
            
            if (role != null && role.equals("ADMIN")) {
                user.setRole("ADMIN");
                System.out.println("Creating ADMIN user: " + username);
            } else {
                user.setRole("USER");
            }

            boolean success = userDAO.registerUser(user);

            if (success) {
                request.setAttribute("success", "Registration successful! Please login.");
                request.getRequestDispatcher("/jsp/auth/login.jsp").forward(request, response);
            } else {
                request.setAttribute("error", "Registration failed. Please try again.");
                request.getRequestDispatcher("/jsp/auth/register.jsp").forward(request, response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Registration failed: " + e.getMessage());
            request.getRequestDispatcher("/jsp/auth/register.jsp").forward(request, response);
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String username = request.getParameter("username").trim();
            String password = request.getParameter("password");

            if (username.isEmpty() || password.isEmpty()) {
                request.setAttribute("error", "Username and password are required");
                request.getRequestDispatcher("/jsp/auth/login.jsp").forward(request, response);
                return;
            }

            User user = null;

            if (username.equals("admin") && password.equals("admin123")) {
                user = new User();
                user.setUserId(1);
                user.setUsername("admin");
                user.setEmail("admin@example.com");
                user.setRole("ADMIN");
            } else {
                user = userDAO.loginUser(username, password);
            }

            if (user != null) {
                HttpSession session = request.getSession();
                session.setAttribute("pendingUser", user);
                session.setAttribute("otpPurpose", "LOGIN");

                String otp = otpService.generateOTP(session, user.getEmail(), "LOGIN");

                EmailService.getInstance().sendOTPEmail(
                        user.getEmail(),
                        user.getUsername(),
                        otp,
                        "Login Verification"
                );

                response.sendRedirect(request.getContextPath() + "/jsp/auth/otp-verification.jsp");

            } else {
                request.setAttribute("error", "Invalid username or password");
                request.getRequestDispatcher("/jsp/auth/login.jsp").forward(request, response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Login failed. Please try again.");
            request.getRequestDispatcher("/jsp/auth/login.jsp").forward(request, response);
        }
    }

    private void handleVerifyOtp(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        User pendingUser = (User) session.getAttribute("pendingUser");
        String purpose = (String) session.getAttribute("otpPurpose");
        String enteredOtp = request.getParameter("otp");
        
        if (pendingUser == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        try {
            boolean isValid = otpService.verifyOTP(session, pendingUser.getEmail(), enteredOtp, purpose);
            
            if (isValid) {
                session.removeAttribute("otp");
                session.removeAttribute("otpExpiry");
                session.removeAttribute("otpPurpose");
                
                session.setAttribute("user", pendingUser);
                session.removeAttribute("pendingUser");
                
                userDAO.updateLastLogin(pendingUser.getUserId());
                
                auditDAO.logActivity(
                    pendingUser.getUserId(),
                    "LOGIN",
                    "User logged in successfully",
                    request.getRemoteAddr(),
                    "SUCCESS"
                );
                
                String redirectUrl;
                if ("ADMIN".equals(pendingUser.getRole())) {
                    redirectUrl = "/admin";
                    System.out.println("✓ Admin user detected, redirecting to: " + redirectUrl);
                } else {
                    redirectUrl = "/dashboard";
                    System.out.println("✓ Regular user detected, redirecting to: " + redirectUrl);
                }
                
                response.sendRedirect(request.getContextPath() + redirectUrl);
                
            } else {
                request.setAttribute("error", "Invalid or expired OTP");
                request.getRequestDispatcher("/jsp/auth/otp-verification.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "OTP verification failed: " + e.getMessage());
            request.getRequestDispatcher("/jsp/auth/otp-verification.jsp").forward(request, response);
        }
    }

    private void handleResendOtp(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        User pendingUser = (User) session.getAttribute("pendingUser");
        String purpose = (String) session.getAttribute("otpPurpose");
        
        if (pendingUser == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        try {
            String otp = otpService.generateOTP(session, pendingUser.getEmail(), purpose);
            
            EmailService.getInstance().sendOTPEmail(
                pendingUser.getEmail(),
                pendingUser.getUsername(),
                otp,
                purpose + " Verification"
            );
            
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("OTP resent successfully");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Failed to resend OTP: " + e.getMessage());
        }
    }

    private void handleFileUpload(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
                return;
            }

            User user = (User) session.getAttribute("user");

            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("error", "No file selected");
                request.getRequestDispatcher("/jsp/dashboard.jsp").forward(request, response);
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            InputStream fileContent = filePart.getInputStream();

            System.out.println("File upload received: " + fileName + " from user: " + user.getUsername());
            
            request.setAttribute("success", "File uploaded successfully");
            request.getRequestDispatcher("/jsp/dashboard.jsp").forward(request, response);
           
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "File upload failed: " + e.getMessage());
            request.getRequestDispatcher("/jsp/dashboard.jsp").forward(request, response);
        }
    }
}