package com.hou.mytomcat.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.hou.mytomcat.http.Request;
import com.hou.mytomcat.http.Response;
import com.hou.mytomcat.http.StandardSession;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.*;

public class SessionManager {
//    //Map<String, StandardSession>  -->  Map<sessionID, sessionObject>
//    private static Map<String, StandardSession> sessionMap = new HashMap<>();
//    private static int defaultTimeout = getTimeout();
//
//    private static int getTimeout() {
//        int defaultTimeout = 30;
//        try {
//            Document d = Jsoup.parse(Constant.webXmlFile, "utf-8");
//            Elements es = d.select("session-config session-timeout");
//            if(es.isEmpty()) {
//                return defaultTimeout;
//            }
//            return Convert.toInt(es.get(0).text());
//        } catch (Exception e) {
//            return defaultTimeout;
//        }
//    }
//
//    static {
//        startSessionTimeoutCheckThread();
//    }
//
//    private static void startSessionTimeoutCheckThread() {
//        new Thread(){
//            public void run() {
//                while(true) {
//                    checkTimeoutSession();
//                    try {
//                        Thread.sleep(1000 * 30);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();
//    }
//    private static void checkTimeoutSession() {
//        Set<String> sessionIDs = sessionMap.keySet();
//        List<String> timeoutSessionIDs = new ArrayList<>();
//        //从一个集合里删除元素的通用方式：两个循环，第一个循环将要删除的对象存储在一个容器，第二个循环遍历这个容器开始删除  --- 将遍历与删除解耦
//        //如果不解耦的话，会出现同步异常
//        // TODO: 试一下不解耦会怎么样
//        for (String id : sessionIDs) {
//            StandardSession session = sessionMap.get(id);
//            long interval = System.currentTimeMillis() - session.getLastAccessedTime();
//            if (interval > session.getMaxInactiveInterval() * 1000) {
//                timeoutSessionIDs.add(id);
//            }
//        }
//        for (String id : timeoutSessionIDs) {
//            sessionMap.remove(id);
//        }
//    }
//
//    //sessionID生成
//    public static synchronized String generatedSessionID() {
//        String result = null;
//        byte[] bytes = RandomUtil.randomBytes(16);
//        result = new String(bytes);
//        result = SecureUtil.md5(result);
//        result = result.toUpperCase();
//
//        return result;
//    }
//
//    public static HttpSession getSession(String id, Request request, Response response) {
//        if(id == null) {
//            //浏览器没有传递sessionID
//            return newSession(request, response);
//        } else {
//            StandardSession currentSession = sessionMap.get(id);
//            if(currentSession == null) {
//                //浏览器传递的是无效sessionID
//                return newSession(request, response);
//            } else {
//                currentSession.setLastAccessedTime(System.currentTimeMillis());
//                creatCookieBySession(currentSession, request, response);
//                return currentSession;
//            }
//        }
//    }
//
//    private static HttpSession newSession(Request request, Response response) {
//        ServletContext servletContext = request.getServletContext();
//        String sid = generatedSessionID();
//        StandardSession session = new StandardSession(sid, servletContext);
//        session.setMaxInactiveInterval(defaultTimeout);
//        sessionMap.put(sid, session);
//        creatCookieBySession(session, request, response);
//        return session;
//    }
//
//    private static void creatCookieBySession(StandardSession session, Request request, Response response) {
//        Cookie cookie = new Cookie("JSESSIONID", session.getId());
//        cookie.setMaxAge(session.getMaxInactiveInterval());
//        cookie.setPath(request.getContext().getPath());
//        response.addCookie(cookie);
//    }


}
