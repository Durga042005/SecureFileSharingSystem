package com.securefileshare.servlets;

import com.securefileshare.models.User;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

public class HelpServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/auth/login.jsp");
            return;
        }
        
        User user = (User) session.getAttribute("user");
        System.out.println("Loading Help page for user: " + user.getUsername());
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("/jsp/user/help.jsp");
        dispatcher.forward(request, response);
    }
}