package com.hou.mytomcat.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.hou.mytomcat.catalina.Connector;
import com.hou.mytomcat.catalina.Context;
import com.hou.mytomcat.catalina.Engine;
import com.hou.mytomcat.catalina.Service;
import com.hou.mytomcat.util.MiniBrowser;
import com.sun.corba.se.spi.ior.ObjectKey;
import sun.security.ssl.CookieExtension;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Request extends BaseRequest {

    private String requestString;
    //其实不是uri，只是为了找到html文件用的，/+文件名
    private String uri;
    private Socket socket;
    private Context context;
    private String method;
    //查询字符串
    private String queryString;
    //参数map   ?name=hou --> map<name, [hou]>
    //value设置成数组的目的是考虑 ?name=hou&name=wang&name=sun 这种一对多情况
    private Map<String, String[]> parameterMap;
    //获取请求头信息
    private Map<String, String> headerMap;
    //接收cookie
    private Cookie[] cookies;
    private HttpSession session;
    private Connector connector;
    //标识符 --- 是否跳转
    private boolean forwarded;
    private Map<String, Object> attributesMap;
    private byte[] httpInfo;
    private SocketChannel socketChannel;

    public Request(Socket socket, Connector connector, byte[] httpInfo, SocketChannel socketChannel) throws IOException {
        this.socket = socket;
        this.connector = connector;
        this.httpInfo = httpInfo;
        this.socketChannel = socketChannel;
        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();
        this.attributesMap = new HashMap<>();

        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        parseUri();
        parseContext();
        parseMethode();
        //非根目录下的uri被剥离到只剩下文件名 /index.html
        if(!"/".equals(context.getPath())) {
            uri = StrUtil.removePrefix(uri, context.getPath());
            // uri: /a /b .. -> /
            if(StrUtil.isEmpty(uri)){
                uri = "/";
            }
        }

        parseParameters();
        parseHeaders();
        parseCookies();
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public Connector getConnector() {
        return connector;
    }

    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    //从cookie里获取sessionID
    public String getSessionIDFromCookie() {
        if(cookies == null) {
            return null;
        }
        for(Cookie cookie : cookies) {
            if("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public Cookie[] getCookies() {
        return cookies;
    }

    private void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        if(cookies != null) {
            String[] pairs = StrUtil.split(cookies, ";");
            for(String pair : pairs) {
                if (StrUtil.isBlank(pair))
                    continue;
                String[] segs = StrUtil.split(pair,"=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }
    public String getHeader(String name) {
        if (name == null) {
            return null;
        }
        //大写转小写
        name = name.toLowerCase();
        return headerMap.get(name);
    }

    public Enumeration getHeaderNames() {
        Set keys = headerMap.keySet();
        return Collections.enumeration(keys);
    }

    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }

    public void parseHeaders() {
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);
        for(int i = 1; i < lines.size(); i ++) {
            String line = lines.get(i);
            if(line.length() == 0) {
                break;
            }
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            headerMap.put(headerName, headerValue);
        }
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if(values != null && values.length != 0) {
            return values[0];
        }
        return null;
    }

    /**
     *  根据Post和get方式解析请求参数
     */
    private void parseParameters() {
        if("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(requestString, " ", " ");
            if(StrUtil.contains(url, '?')) {
                queryString = StrUtil.subAfter(url, '?', false);
            }
        }
        if("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if(queryString == null) {
            return;
        }
        queryString = URLUtil.decode(queryString);
        String[] parameterValues = queryString.split("&");
        if(parameterValues != null) {
            for(String parameterValue : parameterValues) {
                String[] nameValues = parameterValue.split("=");
                String name = nameValues[0];
                String value = nameValues[1];
                //看看以前这个name是否存值，存值就追加，没存就put
                String[] values = parameterMap.get(name);
                if(values == null) {
                    values = new String[] {value};
                    parameterMap.put(name, values);
                } else {
                    values = ArrayUtil.append(values, value);
                    parameterMap.put(name, values);
                }
            }
        }
    }

    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    @Override
    public String getMethod() {
        return method;
    }

    private void parseMethode() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    /**
     * context(path, 绝对地址)
     * context只是给你提供了一个绝对路径，方便request从这个绝对地址中找文件
     */
    //path:  /index.html -> /    /a -> /   /a/index.html -> /a
    private void parseContext() {
        Service service = connector.getService();
        Engine engine = service.getEngine();
        //此时的uri: /a/index.html
        context = engine.getDefaultHost().getContext(uri);
        //uri是目录的情况，直接过掉
        if(context != null)
            return;
        //未解析到context, /a/index.html
        String path = StrUtil.subBetween(uri, "/", "/");
        //根目录 -> ROOT,
        if(null == path)
            path = "/";
        else
            // /a /b ..
            path = "/" + path;
        context = engine.getDefaultHost().getContext(path);
        if(null == context)
            context = engine.getDefaultHost().getContext("/");
    }

    private void parseHttpRequest() throws IOException {
//        InputStream is = this.socket.getInputStream();
//        byte[] bytes = MiniBrowser.readBytes(is, false);
        requestString = new String(this.httpInfo, StandardCharsets.UTF_8);
    }
    /*
        解析uri：
            eg: http://127.0.0.1:18080/index.html?name=hou
            uri: /index.html
            http请求：
                GET /index.html?name=hou HTTP/1.1
                Host: 127.0.0.1:18080
                Connection: keep-alive
                ...
     */
    private void parseUri() {
        if(StrUtil.isEmpty(requestString))
            return;

        String temp;
        temp = StrUtil.subBetween(requestString, " ", " ");

        if(!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, '?', false);
        uri = temp;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString() {
        return requestString;
    }

    //地址
    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress();
    }

    //名称
    public String getLocalName() {
        return socket.getLocalAddress().getHostName();
    }

    //端口
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    //协议
    public String getProtocol() {
        return "HTTP:/1.1";
    }

    //浏览器地址
    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }

    //浏览器名字
    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();
    }

    //浏览器端口
    public int getRemotePort() {
        return socket.getPort();
    }

    //浏览器协议
    public String getScheme() {
        return "http";
    }

    //服务器名称
    public String getServerName() {
        return getHeader("host").trim();
    }

    //服务器端口
    public int getServerPort() {
        return getLocalPort();
    }

    //应用路径
    public String getContextPath() {
        String result = this.context.getPath();
        if("/".equals(result))
            return "";
        return result;
    }

    public String getRequestURI() {
        return uri;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if(port < 0) {
            port = 80;
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))){
            url.append(":");
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }

    //服务器路径
    public String getServletPath() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isForwarded() {
        return forwarded;
    }
    public void setForwarded(boolean b) {
        this.forwarded = b;
    }
    public RequestDispatcher getRequestDispatcher(String uri){
        return new ApplicationRequestDispatcher(uri);
    }
}
