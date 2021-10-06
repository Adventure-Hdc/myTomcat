package com.hou.mytomcat.webappServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            //response -> getWriter -> printWriter -> stringWriter -> byte[] body -> 返回到浏览器
            response.getWriter().println("Hello Servlet");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
