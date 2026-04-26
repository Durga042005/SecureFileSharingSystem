package com.securefileshare.servlets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

public class ClearDownloadSessionServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("downloadedHash");
            session.removeAttribute("expectedHash");
            session.removeAttribute("encryptedHash");
            session.removeAttribute("fileName");
            session.removeAttribute("fileSize");
            
            System.out.println("Cleared download session data for session: " + session.getId());
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":true}");
    }
}