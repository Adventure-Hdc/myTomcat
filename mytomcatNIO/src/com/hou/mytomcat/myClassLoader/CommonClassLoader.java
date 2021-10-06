package com.hou.mytomcat.myClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

//TODO: 为什么放到包下面，全限定名找不到这个类
public class CommonClassLoader extends URLClassLoader {
    public CommonClassLoader() {
        super(new URL[] {});

        try {
            File workingFolder = new File(System.getProperty("user.dir"));
            //公共类加载器，加载lib目录下的所有jar包
            File libFolder = new File(workingFolder, "lib");
            File[] jarFiles = libFolder.listFiles();
            for(File file : jarFiles) {
                if(file.getName().endsWith("jar")) {
                    URL url = new URL("file:" + file.getAbsolutePath());
                    this.addURL(url);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
