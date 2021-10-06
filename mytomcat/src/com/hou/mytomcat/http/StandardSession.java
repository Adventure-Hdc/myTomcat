package com.hou.mytomcat.http;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.*;

public class StandardSession implements HttpSession {
    //存放session数据
    private Map<String, Object> attributesMap;
    private String sessionID;
    //创建时间
    private long creationTime;
    //最后一次访问的时间
    private long lastAccessedTime;
    private ServletContext servletContext;
    //最大持续时间的分钟数
    private int maxInactiveInterval;

    public StandardSession(String sessionID, ServletContext servletContext) {
        this.attributesMap = new HashMap<>();
        this.sessionID = sessionID;
        this.creationTime = System.currentTimeMillis();
        this.servletContext = servletContext;
    }

    public void setLastAccessedTime(long time) {
        this.lastAccessedTime = time;
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.sessionID;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        this.maxInactiveInterval = i;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return attributesMap.get(s);
    }

    @Override
    public Object getValue(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    @Override
    public String[] getValueNames() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributesMap.put(s, o);
    }

    @Override
    public void putValue(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {
        attributesMap.remove(s);
    }

    @Override
    public void removeValue(String s) {

    }

    @Override
    public void invalidate() {
        attributesMap.clear();
    }

    @Override
    public boolean isNew() {
        return creationTime == lastAccessedTime;
    }
}
