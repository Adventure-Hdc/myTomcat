package com.hou.mytomcat.catalina;

import cn.hutool.log.LogFactory;
import com.hou.mytomcat.util.ThreadPoolUtil;
import com.hou.mytomcat.http.RequestBIO;
import com.hou.mytomcat.http.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO的连接方式，每连接，每线程
 */
public class Connector extends BaseConnector implements Runnable {

    public Connector(Service service) {
        super(service);
    }

    @Override
    public void run() {
        try {
            //监听特定端口，用来接收客户端请求
            ServerSocket ss = new ServerSocket();
            ss.bind(new InetSocketAddress("localhost", port));
            //单线程接收处理请求
            while (true) {
                Socket s = ss.accept();

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // http服务器的业务逻辑
                            RequestBIO requestBIO = new RequestBIO(s, Connector.this);
                            com.hou.mytomcat.http.Response response = new Response();
                            HttpProcessorBIO processor = new HttpProcessorBIO();
                            processor.execute(s, requestBIO, response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if(!s.isClosed()){
                                try {
                                    s.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                };
                //每一个端口上来一条请求就启动一条线程
                ThreadPoolUtil.run(r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]", port);
    }

    //根据端口数量创建对应的线程监听
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]", port);
        new Thread(this).start();
    }

}
