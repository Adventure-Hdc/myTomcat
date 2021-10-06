package com.hou.mytomcat.catalina;

/**
 * 本意是将AIO/BIO/NIO的Connector抽象出来
 * 剩下的AIO/NIO的Connector直接继承就好
 * 没想到要合并三种IO方式的话需要改的东西还很多，罢了，改日再说
 */
public class BaseConnector {
    public int port;
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressibleMimeType;
    private com.hou.mytomcat.catalina.Service service;

    public BaseConnector(com.hou.mytomcat.catalina.Service service) {
        this.service = service;
    }

    public com.hou.mytomcat.catalina.Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressibleMimeType() {
        return compressibleMimeType;
    }

    public void setCompressibleMimeType(String compressibleMimeType) {
        this.compressibleMimeType = compressibleMimeType;
    }
}
