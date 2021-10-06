package com.hou.mytomcat.catalina;

import cn.hutool.log.LogFactory;
import com.hou.mytomcat.util.ThreadPoolUtil;
import com.hou.mytomcat.http.Request;
import com.hou.mytomcat.http.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Connector implements Runnable {

    public int port;
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressibleMimeType;
    private com.hou.mytomcat.catalina.Service service;

    public Connector(com.hou.mytomcat.catalina.Service service) {
        this.service = service;
    }

    public com.hou.mytomcat.catalina.Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    @Override
    public void run() {

        Selector selector = null;
        ServerSocketChannel serverSocketChannel = null;
        try {
            //监听特定端口，用来接收客户端请求
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            //单线程接收处理请求
            while (true) {
                //TODO:调试多线程版本的NIO
                selector.select();
                //已经就绪的通道
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                if(selectionKeys.isEmpty()) {
                    continue;
                }
                while(iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    if(!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        //报空指针错，怀疑是线程没创建好，iterator就被remove了
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(key.selector(), SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        //读
                        SocketChannel clientChannelNIO = (SocketChannel) key.channel();
                        clientChannelNIO.configureBlocking(false);
                        byte[] httpInfo = readByteFromChannel(clientChannelNIO, key);

                        //业务处理
                        ThreadPoolUtil.run(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    com.hou.mytomcat.http.Request request = new Request(clientChannelNIO.socket(), Connector.this, httpInfo, clientChannelNIO);
                                    com.hou.mytomcat.http.Response response = new Response();
                                    com.hou.mytomcat.catalina.HttpProcessor processor = new HttpProcessor();
                                    processor.execute(clientChannelNIO, request, response);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
//                        if (key.isValid())
//                            key.cancel();
//                        if (clientChannelNIO.isConnected())
//                            clientChannelNIO.close();
                    }
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(selector != null) {
                    selector.close();
                }
                if(serverSocketChannel != null){
                    serverSocketChannel.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-nio-{}]", port);
    }

    //根据端口数量创建对应的线程监听
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-nio-{}]", port);
        new Thread(this).start();
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

    public byte[] readByteFromChannel(SocketChannel clientChannelNIO, SelectionKey key) throws Exception {
        int buffer_size = 1024;
        ByteBuffer bufferNIO = ByteBuffer.wrap(new byte[buffer_size]);

        List<Byte> result = new ArrayList<>();
        int bytesRead = clientChannelNIO.read(bufferNIO);
        while(bytesRead > 0) {
            bufferNIO.flip();
            while (bufferNIO.hasRemaining()) {
                result.add(bufferNIO.get());
            }
            bufferNIO.clear();
            bytesRead = clientChannelNIO.read(bufferNIO);
        }
        if(bytesRead == -1) {
            clientChannelNIO.close();
        }
        byte[] bytes = new byte[result.size()];
        for(int i = 0; i < result.size(); i ++) {
            bytes[i] = result.get(i);
        }
        return bytes;
    }
}
