package com.hou.mytomcat.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.SecureUtil;
import com.hou.mytomcat.util.MiniBrowser;
import org.apache.tools.ant.taskdefs.Zip;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.rmi.transport.ObjectTable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestTomcat {
    private static int port = 18080;
    private static String ip = "127.0.0.1";
    @BeforeClass
    public static void beforeClass() {
        //所有测试开始前看diy tomcat 是否已经启动了
        if(NetUtil.isUsableLocalPort(port)) {
            System.err.println("请先启动 位于端口: " +port+ " 的diy tomcat，否则无法进行单元测试");
            System.exit(1);
        }
        else {
            System.out.println("检测到 diy tomcat已经启动，开始进行单元测试");
        }
    }

    @Test
    public void testHelloTomcat() {
        String html = getContentString("/");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@ROOT");
    }

    @Test
    public void testaHtml() {
        String html = getContentString("/a.html");
        Assert.assertEquals(html,"Hello DIY Tomcat from how2j.cn");
    }
    @Test
    public void testTimeConsumeHtml() throws InterruptedException {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(20, 20, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(10));
        TimeInterval timeInterval = DateUtil.timer();

        for(int i = 0; i < 3; i ++) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    String html = getContentString("/timeConsume.html");
                }
            });
        }

        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);

        long duration = timeInterval.intervalMs();
        System.out.println("duration: " + duration);
        Assert.assertTrue(duration < 3000);
    }
    @Test
    public void testaIndex(){
        String html = getContentString("/a");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@a");
    }
    @Test
    public void testbIndex(){
        String html = getContentString("/b/");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@b");
    }
    @Test
    public void test404() {
        String response = getHttpString("/not_exist.html");
        containAssert(response, "HTTP/1.1 404 Not Found");
    }
//    @Test
//    public void test500() {
//        String response = getHttpString("/500.html");
//        containAssert(response, "HTTP/1.1 500 Internal Server Error");
//    }
    @Test
    public void testaTxt() {
        String response = getHttpString("/a.txt");
        containAssert(response, "Content-Type: text/plain");
    }
    @Test
    public void testHello() {
        String html = getContentString("/j2ee/hello");
        Assert.assertEquals(html, "Hello Servlet");
    }

    @Test
    public void testSingleton() {
        String html1 = getContentString("/javaweb/hello");
        String html2 = getContentString("/javaweb/hello");
        Assert.assertEquals(html1, html2);
    }

    @Test
    public void testGetParam() {
        String uri = "/javaweb/param";
        String url = StrUtil.format("http://{}:{}{}", ip, port, uri);
        Map<String, Object> params = new HashMap<>();
        params.put("name", "hou");
        String html = MiniBrowser.getContentString(url, params, true);
        Assert.assertEquals(html, "get name: hou");
    }

    @Test
    public void testPostParam() {
        String uri = "/javaweb/param";
        String url = StrUtil.format("http://{}:{}{}", ip, port, uri);
        Map<String, Object> params = new HashMap<>();
        params.put("name", "hou");
        String html = MiniBrowser.getContentString(url, params, false);
        Assert.assertEquals(html, "post name: hou");
    }

    @Test
    public void testHeader() {
        String html = getContentString("/javaweb/header");
        Assert.assertEquals(html, "how2j mini brower / java1.8");
    }

    @Test
    public void testCookie() {
        String html = getHttpString("/javaweb/setCookie");
        containAssert(html, "Set-Cookie: name=hou(cookie); Expires=");
    }

    @Test
    public void testgetCookie() throws IOException {
        String url = StrUtil.format("http://{}:{}{}", ip,port,"/javaweb/getCookie");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Cookie","name=hou(cookie)");
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, "utf-8");
        containAssert(html,"name:hou(cookie)");
    }

    @Test
    public void testSession() throws IOException {
        String jsessionid = getContentString("/javaweb/setSession");
        if(null!=jsessionid)
            jsessionid = jsessionid.trim();
        String url = StrUtil.format("http://{}:{}{}", ip,port,"/javaweb/getSession");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Cookie","JSESSIONID="+jsessionid);
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, "utf-8");
        containAssert(html,"hou(session)");
    }

    @Test
    public void testClientJump() {
        String http_servlet = getHttpString("/javaweb/jump1");
        System.out.println(http_servlet);
        containAssert(http_servlet, "HTTP/1.1 302 Found");
    }

    @Test
    public void testServerJump() {
        String http_servlet = getHttpString("/javaweb/jump2");
        containAssert(http_servlet, "Hello Servlet@javaweb");
    }

    @Test
    public void testServerJumpWithAttributes() {
        String http_servlet = getHttpString("/javaweb/jump2");
        containAssert(http_servlet, "the name is");
    }
    private byte[] getContentBytes(String uri) {
        return getContentBytes(uri,false);
    }
    private byte[] getContentBytes(String uri,boolean gzip) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentBytes(url,gzip);
    }
    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        String content = MiniBrowser.getContentString(url);
        return content;
    }
    private String getHttpString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        String http = MiniBrowser.getHttpString(url);
        return http;
    }
    private void containAssert(String html, String string) {
        boolean match = StrUtil.containsAny(html, string);
        Assert.assertTrue(match);
    }

    @Test
    public void test01(){
        String s = "/a/index.html";
        String front = StrUtil.removePrefix(s, "/");
        System.out.println(front);
    }
}