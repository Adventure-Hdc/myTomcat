package com.hou.mytomcat.servlet;

import cn.hutool.core.util.ReflectUtil;
import com.hou.mytomcat.catalina.Context;
import com.hou.mytomcat.http.RequestBIO;
import com.hou.mytomcat.http.Response;
import com.hou.mytomcat.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class InvokerServlet extends HttpServlet {
    //单例设计模式
    private static InvokerServlet instance = new InvokerServlet();

    public static InvokerServlet getInstance() {
        return instance;
    }

    private InvokerServlet() {
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws IOException, ServletException {

        RequestBIO requestBIO = (RequestBIO) httpServletRequest;
        com.hou.mytomcat.http.Response response = (Response) httpServletResponse;

        String uri = requestBIO.getUri();
        Context context = requestBIO.getContext();
        String servletClassName = context.getServletClassName(uri);

        try {
            Class servletClass = context.getWebappClassLoader().loadClass(servletClassName);
//            System.out.println("servletClass: " + servletClass);
//            System.out.println("servletClassLoader: " + servletClass.getClassLoader());
            //通过反射找到类, servlet类都继承了HttpServlet, 所以直接调用父类的service方法
            Object servletObject = context.getServlet(servletClass);
            //通过反射获得了servlet类，由于servlet都继承了HttpServlet，因此调用父类的service方法
            ReflectUtil.invoke(servletObject, "service", requestBIO, response);

            if(response.getRedirectPath() != null) {
                response.setStatus(Constant.CODE_302);
            } else {
                response.setStatus(Constant.CODE_200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
