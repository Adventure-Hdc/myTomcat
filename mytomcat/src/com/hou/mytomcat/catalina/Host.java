package com.hou.mytomcat.catalina;

import cn.hutool.log.LogFactory;
import com.hou.mytomcat.util.ServerXMLUtil;
import com.hou.mytomcat.util.Constant;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 一个host对象对应着一个name, 一堆contextMap
 * 因为一个域名对应着多个web应用
 * contextMap映射文件夹相对路径与绝对路径的关系：  /a <- -> D:\web\mytomcat\webapps\a
 */
public class Host {
    private String name;
    private Map<String, com.hou.mytomcat.catalina.Context> contextMap;
    private Engine engine;

    public Host() {
        this.contextMap = new HashMap<>();
        this.name = ServerXMLUtil.getHostName();

        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();
    }

    public Host(String name, Engine engine) {
        this.contextMap = new HashMap<>();
        this.name = name;
        this.engine = engine;

        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();
    }

    public Host(String name, Map<String, com.hou.mytomcat.catalina.Context> contextMap) {
        this.name = name;
        this.contextMap = contextMap;

        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();
    }

    public com.hou.mytomcat.catalina.Context getContext(String path) {
        return contextMap.get(path);
    }
    private void scanContextsInServerXML() {
        List<com.hou.mytomcat.catalina.Context> contexts = com.hou.mytomcat.util.ServerXMLUtil.getContexts(this);
        for (com.hou.mytomcat.catalina.Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        for (File folder : folders) {
            //只加载目录 a b ROOT
            if(!folder.isDirectory()) {
                continue;
            }
            loadContext(folder);
        }
    }

    private void loadContext(File folder) {
        String path = folder.getName();
        if("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;

        String docBase = folder.getAbsolutePath();
        com.hou.mytomcat.catalina.Context context = new Context(path, docBase, this, true);
        // /a, context     /, context
        contextMap.put(context.getPath(), context);
    }

    public void reload(Context context) {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        //关闭该context的类加载器和监听器工作
        context.stop();
        //把当前context从contextMap里删除
        contextMap.remove(path);
        //根据刚刚保存的信息，创建一个新的context，也就是重新加载
        Context newContext = new Context(path, docBase, this, reloadable);
        //存储进contextMap
        contextMap.put(newContext.getPath(), newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());
    }
}
