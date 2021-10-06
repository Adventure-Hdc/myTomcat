package com.hou.mytomcat.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import com.hou.mytomcat.util.Constant;
import com.hou.mytomcat.servlet.DefaultServlet;
import com.hou.mytomcat.servlet.InvokerServlet;
import com.hou.mytomcat.http.Request;
import com.hou.mytomcat.http.Response;
import com.hou.mytomcat.util.SessionManager;
import org.apache.tomcat.util.bcel.Const;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpProcessor {

    public void execute(SocketChannel s, Request request, com.hou.mytomcat.http.Response response) throws IOException {
        try {
            String uri = request.getUri();
            if(null == uri) {
                return;
            }

//            prepareSession(request, response);

            Context context = request.getContext();
            String servletClassName = context.getServletClassName(uri);

            if(request.isForwarded())
                return;
            //访问servlet文件
            if (servletClassName != null) {
                InvokerServlet.getInstance().service(request, response);
            } else {
                DefaultServlet.getInstance().service(request, response);
            }
//            List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());
//            com.hou.mytomcat.catalina.ApplicationFilterChain filterChain = new com.hou.mytomcat.catalina.ApplicationFilterChain(filters, workingServlet);
//            filterChain.doFilter(request, response);

            if(Constant.CODE_200 == response.getStatus()) {
                handle200(s, request, response);
                return;
            }
            if(Constant.CODE_302 == response.getStatus()) {
                handle302(s, response);
                return;
            }
            if(response.getStatus() == Constant.CODE_404) {
                handle404(s, uri);
                return;
            }
        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(s, e);
        } finally {
            if(s != null){
                s.close();
            }
        }
    }

    //判断是否要进行gzip压缩
    private static boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncodings = request.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncodings, "gzip"))
            return false;

        Connector connector = request.getConnector();
        if(mimeType.contains(";")) {
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        }
        if(!(connector.getCompression().equals("on")))
            return false;
        if(body.length < connector.getCompressionMinSize())
            return false;
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for(String eachUserAgent : eachUserAgents){
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if(StrUtil.containsAny(userAgent, eachUserAgent))
                return false;
        }
        String mimeTypes = connector.getCompressibleMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for(String eachMimeType : eachMimeTypes) {
            if(mimeType.equals(eachMimeType))
                return true;
        }
        return false;
    }


//    public void prepareSession(Request request, Response response) {
//        String sessionID = request.getSessionIDFromCookie();
//        HttpSession session = SessionManager.getSession(sessionID, request, response);
//        request.setSession(session);
//    }

    protected static void handle200(SocketChannel s, Request request, Response response) throws IOException {

//        OutputStream os = s.getOutputStream();
        String contentType = response.getContentType();
        byte[] body = response.getBody();
        String headText;
        String cookiesHeader = response.getCookiesHeader();
        boolean gzip = isGzip(request, body, contentType);
        if(gzip) {
            headText = Constant.response_head_200_gzip;
        } else
            headText = Constant.response_head_200;
        headText = StrUtil.format(headText, contentType, cookiesHeader);
        if(gzip) {
          body = ZipUtil.gzip(body);
        }
        byte[] head = headText.getBytes(StandardCharsets.UTF_8);
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length,  body.length);
        int buffer_size = 1024;
        ByteBuffer bufferNIO = ByteBuffer.wrap(new byte[buffer_size]);
        bufferNIO.put(responseBytes);
        while(bufferNIO.hasRemaining()) {
            bufferNIO.flip();
            s.write(bufferNIO);
        }
//        os.write(responseBytes, 0, responseBytes.length);
//        os.flush();
//        os.close();

    }

    protected static void handle302(SocketChannel s, Response response) throws IOException {
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        String header = StrUtil.format(head_text, redirectPath);
        byte[] responseBytes = header.getBytes(StandardCharsets.UTF_8);

        int buffer_size = 1024;
        ByteBuffer bufferNIO = ByteBuffer.wrap(new byte[buffer_size]);
        bufferNIO.put(responseBytes);
        while(bufferNIO.hasRemaining()) {
            bufferNIO.flip();
            s.write(bufferNIO);
        }

    }

    protected static void handle404(SocketChannel s, String uri) throws IOException {
        String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
        int buffer_size = 1024;
        ByteBuffer bufferNIO = ByteBuffer.wrap(new byte[buffer_size]);
        bufferNIO.put(responseBytes);
        while(bufferNIO.hasRemaining()) {
            bufferNIO.flip();
            s.write(bufferNIO);
        }
    }

    protected static void handle500(SocketChannel s, Exception e) throws IOException {
        try {
            StackTraceElement[] stackTraces = e.getStackTrace();
            StringBuffer str = new StringBuffer();
            str.append(e.toString());
            str.append("\r\n");
            for(StackTraceElement element : stackTraces) {
                str.append("\t");
                str.append(element.toString());
                str.append("\r\n");
            }
            String msg = e.getMessage();
            //为了方便，消息太长的话截断一部分消息显示
            if(msg != null && msg.length() > 20) {
                msg = msg.substring(0, 19);
            }
            String text = StrUtil.format(Constant.textFormat_500, msg, e.toString(), str.toString());
            text = com.hou.mytomcat.util.Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes(StandardCharsets.UTF_8);
            int buffer_size = 1024;
            ByteBuffer bufferNIO = ByteBuffer.wrap(new byte[buffer_size]);
            bufferNIO.put(responseBytes);
            while(bufferNIO.hasRemaining()) {
                bufferNIO.flip();
                s.write(bufferNIO);
            }
        } catch (Exception E) {
            E.printStackTrace();
        } finally {
            s.close();
        }
    }
}
