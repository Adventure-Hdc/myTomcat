package com.hou.mytomcat;

import com.hou.mytomcat.myClassLoader.CommonClassLoader;

import java.lang.reflect.Method;


public class Bootstrap {
    public static void main(String[] args) throws Exception {
        com.hou.mytomcat.myClassLoader.CommonClassLoader commonClassLoader = new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(commonClassLoader);
        String serverClassName = "com.hou.mytomcat.catalina.Server";
        Class<?> serverClazz = commonClassLoader.loadClass(serverClassName);
        Object serverObject = serverClazz.newInstance();
        Method method = serverClazz.getMethod("start");
        method.invoke(serverObject);
        System.out.println(serverClazz.getClassLoader());
    }
}
