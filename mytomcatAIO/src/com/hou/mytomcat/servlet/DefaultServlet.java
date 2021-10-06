package com.hou.mytomcat.servlet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hou.mytomcat.util.Constant;
import com.hou.mytomcat.util.WebXMLUtil;
import com.hou.mytomcat.catalina.Context;
import com.hou.mytomcat.http.Request;
import com.hou.mytomcat.http.Response;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;

public class DefaultServlet extends HttpServlet {
    private static DefaultServlet instance = new DefaultServlet();

    public static DefaultServlet getInstance() {
        return instance;
    }

    private DefaultServlet() {

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        com.hou.mytomcat.http.Request request = (Request) httpServletRequest;
        com.hou.mytomcat.http.Response response = (Response) httpServletResponse;

        Context context = request.getContext();

        String uri = request.getUri();

        if(uri.equals("/500.html")) {
            try {
                throw new Exception("this is a deliberately created exception");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if("/".equals(uri)) {
            //uri是/
            uri = WebXMLUtil.getWelcomeFile(request.getContext());
        }
        //非根目录，访问文件 uri: /index.html -> 文件: index.html
        String fileName = StrUtil.removePrefix(uri, "/");
        File file = FileUtil.file(request.getRealPath(fileName));
//        File file = FileUtil.file(context.getDocBase(), fileName);
        //这个路径下面没有这个文件名报404
        if(file.exists()) {
            //拿到后缀名,并设置后缀名
            String extName = FileUtil.extName(file);
            String mimeType = com.hou.mytomcat.util.WebXMLUtil.getMimeType(extName);
            response.setContentType(mimeType);
//                            String fileContent = FileUtil.readUtf8String(file);
//                            response.getWriter().println(fileContent);
            //直接将文件转换成字节流给response
            byte[] body = FileUtil.readBytes(file);
            response.setBody(body);

            if(fileName.equals("timeConsume.html")) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            response.setStatus(Constant.CODE_200);
        } else {
            response.setStatus(com.hou.mytomcat.util.Constant.CODE_404);
        }
    }
}
