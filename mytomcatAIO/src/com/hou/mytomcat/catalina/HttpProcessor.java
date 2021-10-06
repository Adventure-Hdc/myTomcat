package com.hou.mytomcat.catalina;

import cn.hutool.core.util.ArrayUtil;
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

    public String execute(Request request, com.hou.mytomcat.http.Response response) throws IOException {
        try {
            String uri = request.getUri();
            if(null == uri) {
                return null;
            }

//            prepareSession(request, response);

            Context context = request.getContext();
            String servletClassName = context.getServletClassName(uri);

            if(request.isForwarded())
                return null;
//            //访问servlet文件
//            HttpServlet workingServlet;
            //访问servlet文件
            if (servletClassName != null) {
                InvokerServlet.getInstance().service(request, response);
            } else {
                DefaultServlet.getInstance().service(request, response);
            }
//            List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());
//            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
//            filterChain.doFilter(request, response);


            if(Constant.CODE_200 == response.getStatus()) {
                return handle200(request, response);
            }
            if(Constant.CODE_302 == response.getStatus()) {
                return handle302(response);
            }
            if(response.getStatus() == Constant.CODE_404) {
                return handle404(uri);
            }
        } catch (Exception e) {
            LogFactory.get().error(e);
            return handle500(e);
        }
        return null;
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

    protected static String handle200(Request request, Response response) throws IOException {

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
        String res = new String(responseBytes);
        return res;
    }

    protected static String handle302(Response response) throws IOException {
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        String header = StrUtil.format(head_text, redirectPath);
        return header;
    }

    protected static String handle404(String uri) throws IOException {
        String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
        responseText = Constant.response_head_404 + responseText;
        return responseText;
    }

    protected static String handle500(Exception e) throws IOException {
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
            return text;
        } catch (Exception E) {
            E.printStackTrace();
        }
        return null;
    }
}
