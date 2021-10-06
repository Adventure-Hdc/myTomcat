package com.hou.mytomcat.test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * URLClassLoader -> 支持从jar文件和文件夹中获取class
 */
public class CustomizedURLClassLoader extends URLClassLoader {

    public CustomizedURLClassLoader(URL[] urls) {
        super(urls);
    }

    public static void main(String[] args) throws Exception {
        // URL: 文件夹或者jar包
        URL url = new URL("file:d:/web/mytomcat/classes_4_test/");
        URL url1 = new URL("file:d:/web/mytomcat/classes_3_test/");
        URL[] urls = new URL[] {url, url1};

        CustomizedURLClassLoader loader = new CustomizedURLClassLoader(urls);
        //TODO: 怎么只能加载类名，加上全限定名就找不到
        Class<?> how2jClass = loader.loadClass("testLoader");
        Class<?> hou = loader.loadClass("hou");
        Class<?> testLoader2 = loader.loadClass("testLoader2");
        Object o1 = how2jClass.newInstance();
        Method m1 = how2jClass.getMethod("hello");
        m1.invoke(o1);

        Object o = how2jClass.newInstance();
        Method m = how2jClass.getMethod("hello");
        m.invoke(o);

        System.out.println(how2jClass.getClassLoader());
        System.out.println(hou.getClassLoader());
        System.out.println(testLoader2.getClassLoader());
    }
}
