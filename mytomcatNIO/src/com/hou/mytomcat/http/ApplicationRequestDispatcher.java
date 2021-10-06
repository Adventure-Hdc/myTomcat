package com.hou.mytomcat.http;

import com.hou.mytomcat.catalina.HttpProcessor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 服务端跳转实现思路：
 * 修改request的uri
 * 通过HttpProcessor的execute再执行一次
 * 相当于在服务器内部再次访问了某个页面
 * 之所以设计服务端跳转一般是为了传参
 */
public class ApplicationRequestDispatcher implements RequestDispatcher {
    private String uri;
    public ApplicationRequestDispatcher(String uri) {
        if(!uri.startsWith("/")){
            uri = "/" + uri;
        }
        this.uri = uri;
    }

    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        Request request = (Request) servletRequest;
        Response response = (Response) servletResponse;

        request.setUri(uri);

        HttpProcessor processor = new HttpProcessor();
        processor.execute(request.getSocketChannel(), request, response);
        request.setForwarded(true);
    }

    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

    }
}
