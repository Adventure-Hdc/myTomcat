package com.hou.mytomcat.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import com.hou.mytomcat.catalina.Host;
import com.hou.mytomcat.catalina.Connector;
import com.hou.mytomcat.catalina.Context;
import com.hou.mytomcat.catalina.Engine;
import com.hou.mytomcat.catalina.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析xml
 */
public class ServerXMLUtil {
    public static List<Connector> getConnectors(Service service) {
        List<Connector> connectors = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Connector");

        for(Element e : es) {
            int port = Convert.toInt(e.attr("port"));
            String compression = e.attr("compression");
            int compressionMinSize = Convert.toInt(e.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = e.attr("noCompressionUserAgents");
            String compressibleMimeType = e.attr("compressibleMimeType");
            Connector conn = new Connector(service);
            conn.port = port;
            conn.setCompression(compression);
            conn.setCompressibleMimeType(compressibleMimeType);
            conn.setCompressionMinSize(compressionMinSize);
            conn.setNoCompressionUserAgents(noCompressionUserAgents);
            connectors.add(conn);
        }
        return connectors;
    }

    public static List<Context> getContexts(Host host) {
        List<Context> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Context");
        for(Element e : es) {
            String path = e.attr("path");
            String docBase = e.attr("docBase");
            boolean reloadable = Convert.toBool(e.attr("reloadable"), true);
            Context context = new Context(path, docBase, host, reloadable);
            result.add(context);
        }

        return result;
    }

    /**
     * 最后获取出localhost字符串
     * @return
     */
    public static String getHostName() {
        //把文件解析成字符串
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        //解析xml
        Document d = Jsoup.parse(xml);
        //选择第一个Host?
        Element host = d.select("Host").first();
        return host.attr("name");
    }

    /**
     * 拿到引擎默认的host
     */
    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element host = d.select("Engine").first();
        return host.attr("defaultHost");
    }

    /**
     * 获取一个engine下的全部host
     */
    public static List<Host> getHosts(Engine engine) {
        List<Host> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Elements hosts = d.select("Host");
        for(Element e : hosts) {
            String name = e.attr("name");
            System.out.println("[hostName] ==========" + name + "==========");
            Host host = new Host(name, engine);
            result.add(host);
        }
        return result;
    }
    /**
     * 获取ServiceName
     */
    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element host = d.select("Service").first();
        return host.attr("name");
    }
}
