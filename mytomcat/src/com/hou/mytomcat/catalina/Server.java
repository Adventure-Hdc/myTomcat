package com.hou.mytomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import com.hou.mytomcat.util.InitLogRecord;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 架构中的老大，所有http服务的管理者
 */
public class Server {
    private com.hou.mytomcat.catalina.Service service;

    public Server() {
        this.service = new Service(this);
    }

    public void start() throws IOException {
        TimeInterval timeInterval = DateUtil.timer();
        InitLogRecord.initLog();
        logJVM();
        init();
        LogFactory.get().info("Server startup in {} ms", timeInterval.intervalMs());
    }

    public void init() throws IOException {
        service.start();
    }
    //打印JVM信息
    private static void logJVM() {
        Map<String, String> infos = new LinkedHashMap<>();
        infos.put("Server version", "myTomcat/1.0.1");
        infos.put("Server built", "2021-09-10 09:00:00");
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));

        Set<String> keys = infos.keySet();
        for(String key : keys) {
            LogFactory.get().info(key + ":\t\t" + infos.get(key));
        }
    }


}
