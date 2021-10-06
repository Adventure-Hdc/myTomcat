package com.hou.mytomcat.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

public class CustomizedClassLoader extends  ClassLoader {
    private File classesFolder = new File(System.getProperty("user.dir"), "classes_4_test");

    /**
     * 这个函数就相当于用自定义类加载器加载.class文件
     */
    @Override
    protected Class<?> findClass(String QualifiedName) throws ClassNotFoundException {
        byte[] data = loadClassData(QualifiedName);
        return defineClass(QualifiedName, data, 0, data.length);
    }

    private byte[] loadClassData(String fullQualifiedName) throws ClassNotFoundException {
        String fileName = StrUtil.replace(fullQualifiedName, ".", "/") + ".class";
        File classFile = new File(classesFolder, fileName);
        if(!classFile.exists())
            throw new ClassNotFoundException(fullQualifiedName);
        return FileUtil.readBytes(classFile);
    }

    @Test
    public void test() throws Exception {
        CustomizedClassLoader loader = new CustomizedClassLoader();
        //TODO: 怎么只能加载类名，加上全限定名就找不到
        Class<?> houClass = loader.loadClass("hou");
        Object o = houClass.newInstance();
        Method m = houClass.getMethod("hello");
        m.invoke(o);
        System.out.println(houClass.getClassLoader());
    }

}
