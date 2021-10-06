package com.hou.mytomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import com.hou.mytomcat.util.ServerXMLUtil;

import java.util.List;

public class Service {
    private String name;
    private Engine engine;
    private Server server;
    private List<Connector> connectorList;

    public Service(Server server) {
        this.server = server;
        this.name = ServerXMLUtil.getServiceName();
        this.engine = new Engine(this);
        this.connectorList = com.hou.mytomcat.util.ServerXMLUtil.getConnectors(this);
    }

    public Engine getEngine() {
        return engine;
    }

    public Server getServer() {
        return server;
    }

    public void start() {
        init();
    }

    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for(Connector c : connectorList) {
            c.init();
        }
        LogFactory.get().info("Initialization processed int {} ms", timeInterval.intervalMs());
        for(Connector c : connectorList) {
            c.start();
        }
    }
}
