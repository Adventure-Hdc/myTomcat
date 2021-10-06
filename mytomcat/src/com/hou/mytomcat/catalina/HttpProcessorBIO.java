package com.hou.mytomcat.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import com.hou.mytomcat.util.Constant;
import com.hou.mytomcat.servlet.DefaultServlet;
import com.hou.mytomcat.servlet.InvokerServlet;
import com.hou.mytomcat.http.RequestBIO;
import com.hou.mytomcat.http.Response;
import com.hou.mytomcat.util.SessionManager;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 从response中抽象出来Http服务器的处理方法，
 */
public class HttpProcessorBIO {
    public void execute(Socket s, RequestBIO requestBIO, com.hou.mytomcat.http.Response response) {
        try {
            String uri = requestBIO.getUri();
            if(null == uri) {
                return;
            }

            prepareSession(requestBIO, response);

            Context context = requestBIO.getContext();
            String servletClassName = context.getServletClassName(uri);

            if(requestBIO.isForwarded())
                return;
            HttpServlet workingServlet;
            //访问servlet文件
            if (servletClassName != null) {
                workingServlet = InvokerServlet.getInstance();
            } else {
                workingServlet = DefaultServlet.getInstance();
            }
            List<Filter> filters = requestBIO.getContext().getMatchedFilters(requestBIO.getRequestURI());
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(requestBIO, response);

            if(Constant.CODE_200 == response.getStatus()) {
                handle200(s, requestBIO, response);
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
            if(!s.isClosed()){
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //判断是否要进行gzip压缩
    private static boolean isGzip(RequestBIO requestBIO, byte[] body, String mimeType) {
        String acceptEncodings = requestBIO.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncodings, "gzip"))
            return false;

        Connector connectorBIO = requestBIO.getConnector();
        if(mimeType.contains(";")) {
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        }
        if(!(connectorBIO.getCompression().equals("on")))
            return false;
        if(body.length < connectorBIO.getCompressionMinSize())
            return false;
        String userAgents = connectorBIO.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for(String eachUserAgent : eachUserAgents){
            eachUserAgent = eachUserAgent.trim();
            String userAgent = requestBIO.getHeader("User-Agent");
            if(StrUtil.containsAny(userAgent, eachUserAgent))
                return false;
        }
        String mimeTypes = connectorBIO.getCompressibleMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for(String eachMimeType : eachMimeTypes) {
            if(mimeType.equals(eachMimeType))
                return true;
        }
        return false;
    }


    public void prepareSession(RequestBIO requestBIO, Response response) {
        String sessionID = requestBIO.getSessionIDFromCookie();
        HttpSession session = SessionManager.getSession(sessionID, requestBIO, response);
        requestBIO.setSession(session);
    }

    protected static void handle200(Socket s, RequestBIO requestBIO, Response response) throws IOException {
        OutputStream os = s.getOutputStream();
        String contentType = response.getContentType();
        byte[] body = response.getBody();
        String headText;
        String cookiesHeader = response.getCookiesHeader();
        boolean gzip = isGzip(requestBIO, body, contentType);
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

        os.write(responseBytes, 0, responseBytes.length);
        os.flush();
        os.close();
    }

    protected static void handle302(Socket s, Response response) throws IOException {
        OutputStream os = s.getOutputStream();
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        String header = StrUtil.format(head_text, redirectPath);
        byte[] responseBytes = header.getBytes(StandardCharsets.UTF_8);
        os.write(responseBytes);
    }

    protected static void handle404(Socket s, String uri) throws IOException {
        OutputStream os = s.getOutputStream();
        String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseByte = responseText.getBytes(StandardCharsets.UTF_8);
        os.write(responseByte);
    }

    protected static void handle500(Socket s, Exception e) {
        try {
            OutputStream os = s.getOutputStream();
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
            os.write(responseBytes);
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
    }
}
