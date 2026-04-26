package com.securefileshare.servlets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import org.json.JSONObject;

public class CheckHashVerificationServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        JSONObject jsonResponse = new JSONObject();
        
        if (session != null) {
            String downloadedHash = (String) session.getAttribute("downloadedHash");
            String expectedHash = (String) session.getAttribute("expectedHash");
            String encryptedHash = (String) session.getAttribute("encryptedHash");
            String fileName = (String) session.getAttribute("fileName");
            String fileSize = (String) session.getAttribute("fileSize");
            
            jsonResponse.put("success", true);
            jsonResponse.put("downloadedHash", downloadedHash);
            jsonResponse.put("expectedHash", expectedHash);
            jsonResponse.put("encryptedHash", encryptedHash);
            jsonResponse.put("fileName", fileName);
            jsonResponse.put("fileSize", fileSize);
            jsonResponse.put("match", downloadedHash != null && downloadedHash.equals(expectedHash));
        } else {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "No session found");
        }
        
        response.setContentType("application/json");
        response.getWriter().write(jsonResponse.toString());
    }
}