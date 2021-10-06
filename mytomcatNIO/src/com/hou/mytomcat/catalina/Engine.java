package com.hou.mytomcat.catalina;

import com.hou.mytomcat.util.ServerXMLUtil;

import java.util.List;

public class Engine {

    private String defaultHost;
    private List<com.hou.mytomcat.catalina.Host> hosts;
    private com.hou.mytomcat.catalina.Service service;

    public Engine(com.hou.mytomcat.catalina.Service service) {
        this.service = service;
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hosts = com.hou.mytomcat.util.ServerXMLUtil.getHosts(this);
        checkDefault();
    }

    private void checkDefault() {
        if(null == getDefaultHost()){
            throw new RuntimeException("the defaultHost" + defaultHost + "does not exist!");
        }
    }

    public Service getService() {
        return service;
    }

    public com.hou.mytomcat.catalina.Host getDefaultHost() {
        for (Host host : hosts) {
            if (host.getName().equals(defaultHost)) {
                return host;
            }
        }
        return null;
    }

}
