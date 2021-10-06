package com.hou.mytomcat.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import com.hou.mytomcat.catalina.Context;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class ContextFileChangeWatcher {
    private WatchMonitor monitor;
    private boolean stop = false;

    public ContextFileChangeWatcher(Context context) {
        System.out.println("context.getDocBase(): " + context.getDocBase() + "context.getPath(): " + context.getPath());
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
//            private void dealWith(WatchEvent<?> event) {
//                //此处加同步块的原因是防止在并发处理情况下，context被重新加载多次
//                synchronized (ContextFileChangeWatcher.class) {
//                    String fileName = event.context().toString();
//
//                    if (stop)
//                        return;
//                    //TODO: 变化的是文件夹
//                    LogFactory.get().info("=======变化的文件：" + fileName + "==============");
//                    if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
//                        stop = true;
//                        LogFactory.get().info(ContextFileChangeWatcher.this + "检测到了web应用下的重要文件变化{}", fileName);
//                        context.reload();
//                    }
//                }
//            }
            private void dealWith(WatchEvent<?> event) {
                //此处加同步块的原因是防止在并发处理情况下，context被重新加载多次
                synchronized (context) {
                    String fileName = event.context().toString();

                    //TODO:监听器莫名奇妙经常抛NoSuchFileException异常，但是能看到编译好的文件里有这个文件
//                    if (stop)
//                        return;
                    LogFactory.get().info("=======变化的文件：" + fileName + "==============");
                    stop = true;
                    LogFactory.get().info(ContextFileChangeWatcher.this + "检测到了web应用下的重要文件变化{}", fileName);
                    context.reload();
                }
            }

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                System.out.println("-------------创建---------------");
                dealWith(watchEvent);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                System.out.println("-------------修改---------------");
                dealWith(watchEvent);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                System.out.println("-------------删除---------------");
                dealWith(watchEvent);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                System.out.println("-------------不知道---------------");
                dealWith(watchEvent);
            }
        });
//        this.monitor.setDaemon(true);
    }
    public void start() {
        monitor.start();
    }
    public void stop() {
        monitor.close();
    }
}
