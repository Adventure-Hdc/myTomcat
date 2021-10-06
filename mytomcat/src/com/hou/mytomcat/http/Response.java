package com.hou.mytomcat.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class Response extends BaseResponse {

    private List<Cookie> cookies;
    //用于存放返回html内容
    private StringWriter stringWriter;
    //提供一个getWriter方法，可以像HttpServletResponse那样写成response.getWriter().println();这种风格
    private PrintWriter printWriter;
    //字节数组，存放图片、pdf等文件转化的字节
    private byte[] body;
    private String contentType;
    //状态码
    private int status;
    //保存客户端跳转路径
    private String redirectPath;

    public Response() {
        this.stringWriter = new StringWriter();
        this.printWriter = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    public String getRedirectPath() {
        return this.redirectPath;
    }

    public void sendRedirect(String redirect) throws IOException {
        this.redirectPath = redirect;
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


    public PrintWriter getWriter() {
        return printWriter;
    }

    public byte[] getBody() {
        //body == null -> 纯文本文件 转换成字节返回给浏览器
        if (body == null) {
            String content = stringWriter.toString();
            body = content.getBytes(StandardCharsets.UTF_8);
        }
        return body;
    }
    //如果不是纯文本文件就转换成二进制字节流给body
    public void setBody(byte[] body) {
        this.body = body;
    }

    //cookie集合转换成字符串
    public String getCookiesHeader() {
        if(cookies == null) {
            return "";
        }

        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
        StringBuffer str = new StringBuffer();
        for(Cookie cookie : getCookies()) {
            str.append("\r\n");
            str.append("Set-Cookie: ");
            str.append(cookie.getName() + "=" + cookie.getValue() + "; ");
            if(cookie.getMaxAge() != -1) {
                str.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                str.append(sdf.format(expire));
                str.append("; ");
            }
            if(cookie.getPath() != null) {
                str.append("Path=" + cookie.getPath());
            }
        }
        return str.toString();
    }
}
