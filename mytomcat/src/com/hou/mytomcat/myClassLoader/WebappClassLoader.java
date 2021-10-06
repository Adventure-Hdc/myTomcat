package com.hou.mytomcat.myClassLoader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class WebappClassLoader extends URLClassLoader {
    public WebappClassLoader(String docBase, ClassLoader commonClassLoader) {
        //制定它的parent类加载器是commonClassLoader
        super(new URL[] {}, commonClassLoader);

        try {
            File webInfFolder = new File(docBase, "WEB-INF");
            File classesFolder = new File(webInfFolder, "classes");
            File libFolder = new File(webInfFolder, "lib");
            URL url;
            //加载编译好的class文件，web应用自己写的那部分类，结尾的"/"表示加载的是目录
            url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);
            //加载lib目录下引入的jar包
            List<File> jarFiles = FileUtil.loopFiles(libFolder);
            for(File file : jarFiles) {
                url = new URL("file:" + file.getAbsolutePath());
                this.addURL(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void stop() {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
