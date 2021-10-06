package com.hou.mytomcat.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {

    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1000, 5000, 100,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public static void run(Runnable r) {
        threadPool.execute(r);
    }
}
