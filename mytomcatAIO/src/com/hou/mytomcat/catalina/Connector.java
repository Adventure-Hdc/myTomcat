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
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Connector {

    static Charset charset = StandardCharsets.UTF_8;
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

    class AioServer implements Runnable {

        int port;
        AsynchronousChannelGroup group;
        AsynchronousServerSocketChannel serverSocketChannel;

        public AioServer(int port) {
            this.port = port;
            init();
        }

        public void init() {
            try {
                // 创建处理线程池
                group = AsynchronousChannelGroup.withCachedThreadPool(Executors.newCachedThreadPool(), 5);
                // 创建服务channel
                serverSocketChannel = AsynchronousServerSocketChannel.open(group);
                // 丙丁端口
                serverSocketChannel.bind(new InetSocketAddress(port));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // 接收请求
            // accept的第一个参数附件，第二个参数是收到请求后的接收处理器
            // 接收处理器AcceptHandler泛型的第一个参数的处理结果，这里是AsynchronousSocketChannel，即接收到的请求的channel
            // 第二个参数是附件，这里是AioServer，即其实例
            serverSocketChannel.accept(this, new AcceptHandler());
        }
    }

    class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AioServer> {

        @Override
        public void completed(AsynchronousSocketChannel result, AioServer attachment) {
            // 继续接收下一个请求，构成循环调用
            attachment.serverSocketChannel.accept(attachment, this);

            try {
//                System.out.println("接收到连接请求：" + result.getRemoteAddress().toString());

                // 定义数据读取缓存
                ByteBuffer buffer = ByteBuffer.wrap(new byte[1024]);
                // 读取数据，并传入数据到达时的处理器
                // read的第一个参数数据读取到目标缓存，第二个参数是附件，第三个传输的读取结束后的处理器
                // 读取处理器泛型的第一个参数是读取的字节数，第二个参数输附件对象
                result.read(buffer, buffer, new ReadHandler(result));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Throwable exc, AioServer attachment) {
            exc.printStackTrace();
        }
    }

    class ReadHandler implements CompletionHandler<Integer, ByteBuffer> {
        AsynchronousSocketChannel socketChannel;

        public ReadHandler(AsynchronousSocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            List<Byte> res = new ArrayList<>();
            attachment.flip();
            while(attachment.hasRemaining()) {
                res.add(attachment.get());
            }
            byte[] resBytes = new byte[res.size()];
            for(int i = 0; i < res.size(); i ++) {
                resBytes[i] = res.get(i);
            }
            String resMsg = new String(resBytes);
            ThreadPoolUtil.run(new Runnable() {
                @Override
                public void run() {
                    try {
                        Request request = new Request(Connector.this, resMsg);
                        com.hou.mytomcat.http.Response response = new Response();
                        com.hou.mytomcat.catalina.HttpProcessor processor = new HttpProcessor();
                        String reply = processor.execute(request, response);
                        // 发送数据
                        doWrite(socketChannel, reply);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            attachment.clear();
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            exc.printStackTrace();
        }
    }

    private void doWrite(AsynchronousSocketChannel channel, String reply){
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(charset.encode(reply + System.lineSeparator()));
        buffer.flip();
        channel.write(buffer);
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-aio-{}]", port);
    }

    //根据端口数量创建对应的线程监听
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-aio-{}]", port);
        new Thread(new AioServer(port)).start();
        try {
            TimeUnit.MINUTES.sleep(60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
