package com.securefileshare.servlets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

public class ClearUploadSessionServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("encryptionPassword");
            session.removeAttribute("originalHash");
            session.removeAttribute("encryptedHash");
            session.removeAttribute("uploadedFile");
            session.removeAttribute("storedFile");
            session.removeAttribute("fileSize");
            
            System.out.println("Cleared upload session data");
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
    }
}