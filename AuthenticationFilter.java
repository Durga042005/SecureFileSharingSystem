package com.securefileshare.filters;

import com.securefileshare.models.User;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AuthenticationFilter implements Filter {
    
    // List of public paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/index.jsp",
        "/jsp/auth/login.jsp",
        "/jsp/auth/register.jsp",
        "/jsp/auth/otp-verification.jsp",
        "/auth",
        "/verify-otp",
        "/resend-otp"
    );
    
    // List of public resource extensions
    private static final List<String> PUBLIC_EXTENSIONS = Arrays.asList(
        ".css", ".js", ".jpg", ".jpeg", ".png", ".gif", ".ico", ".svg"
    );
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("✅ AuthenticationFilter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestURI.substring(contextPath.length());
        
        System.out.println("🔍 Filter processing: " + path);
        
        // ============== ALLOW PUBLIC ACCESS ==============
        
        // 1. Allow root path (/) - THIS IS THE KEY FIX!
        if (path.equals("/") || path.isEmpty()) {
            System.out.println("✅ Root path accessed - allowing");
            chain.doFilter(request, response);
            return;
        }
        
        // 2. Allow index.jsp explicitly
        if (path.equals("/index.jsp")) {
            System.out.println("✅ index.jsp accessed - allowing");
            chain.doFilter(request, response);
            return;
        }
        
        // 3. Allow all public paths from the list
        if (PUBLIC_PATHS.contains(path)) {
            System.out.println("✅ Public path accessed: " + path);
            chain.doFilter(request, response);
            return;
        }
        
        // 4. Allow public resources by extension
        for (String ext : PUBLIC_EXTENSIONS) {
            if (path.endsWith(ext)) {
                System.out.println("✅ Public resource: " + path);
                chain.doFilter(request, response);
                return;
            }
        }
        
        // 5. Allow auth servlet paths (they handle their own authentication)
        if (path.startsWith("/auth") || path.startsWith("/verify-otp") || path.startsWith("/resend-otp")) {
            System.out.println("✅ Auth servlet accessed: " + path);
            chain.doFilter(request, response);
            return;
        }
        
        // ============== CHECK AUTHENTICATION ==============
        
        HttpSession session = httpRequest.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);
        
        if (!isLoggedIn) {
            System.out.println("❌ Not authenticated - redirecting to login from: " + path);
            
            // Store the original requested URL to redirect back after login
            httpRequest.getSession().setAttribute("redirectAfterLogin", path);
            
            httpResponse.sendRedirect(contextPath + "/jsp/auth/login.jsp");
            return;
        }
        
        // User is authenticated - allow access
        System.out.println("✅ Authenticated user accessing: " + path);
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        System.out.println("✅ AuthenticationFilter destroyed");
    }
}