

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * AIO服务端
 *
 * @author zhoujunwen
 * @date 2019-11-08
 * @time 17:50
 * @desc
 */
class AIO_Server {
    static Charset charset = Charset.forName("UTF-8");

    public static void main(String[] args) throws InterruptedException {
        int port = 7890;

        new Thread(new AioServer(port)).start();
        TimeUnit.MINUTES.sleep(60);
    }

    static class AioServer implements Runnable {

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

    /**
     * 接收请求处理器
     * completionHandler泛型的第一个参数的处理结果，这里是AsynchronousSocketChannel，即接收到的请求的channel，
     * 第二个参数是附件，这里是AioServer，即其实例
     */
    static class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AioServer> {

        @Override
        public void completed(AsynchronousSocketChannel result, AioServer attachment) {
            // 继续接收下一个请求，构成循环调用
            attachment.serverSocketChannel.accept(attachment, this);

            try {
                System.out.println("接收到连接请求：" + result.getRemoteAddress().toString());

                // 定义数据读取缓存
                ByteBuffer buffer = ByteBuffer.wrap(new byte[1024]);
                // 读取数据，并传入数据到达时的处理器
                // read的第一个参数数据读取到目标缓存，第二个参数是附件，第三个传输的读取结束后的处理器
                // 读取处理器泛型的第一个参数是读取的字节数，第二个参数输附件对象
                result.read(buffer, buffer, new ReadHandler(result));

                // 新开新城发送数据
                new Thread(new WriteThread(result)).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Throwable exc, AioServer attachment) {

        }
    }

    /**
     * 读取数据处理器
     * completionHandler第一个参数是读取的字节数，第二个参数输附件对象
     */
    static class ReadHandler implements CompletionHandler<Integer, ByteBuffer> {
        AsynchronousSocketChannel socketChannel;

        public ReadHandler(AsynchronousSocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            if (result == -1) {
                attachment.clear();
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            attachment.flip();
            String readMsg = charset.decode(attachment).toString();
            System.out.println("服务端接收到的数据：" + readMsg);
            attachment.compact();

            // 继续接收数据，构成循环
            socketChannel.read(attachment, attachment, this);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {

        }
    }

    /**
     * 写出数据处理器
     */
    static class WriteHandler implements CompletionHandler<Integer, ByteBuffer> {
        AsynchronousSocketChannel socketChannel;
        Scanner scanner;

        public WriteHandler(AsynchronousSocketChannel socketChannel, Scanner scanner) {
            this.socketChannel = socketChannel;
            this.scanner = scanner;
        }


        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            attachment.compact();
            String msg = scanner.nextLine();

            System.out.println("服务端即将发送的数据：" + msg);
            attachment.put(charset.encode(msg));
            attachment.flip();

            // 继续写数据，构成循环
            socketChannel.write(attachment, attachment, this);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {

        }
    }

    static class WriteThread implements Runnable {

        private AsynchronousSocketChannel channel;

        public WriteThread(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            // 第一缓冲区
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            Scanner scanner = new Scanner(System.in);
            String msg = scanner.nextLine();
            System.out.println("服务端输入数据：" + msg);
            buffer.put(charset.encode(msg + System.lineSeparator()));
            buffer.flip();

            // 写入数据，并有写数据时的处理器
            // write的第一个参数是数据写入的缓存，第二个参数是附件，第三个参数写结束后的处理器
            // 读取处理器泛型的第一个参数是写入的字节数，第二个是附件类型
            channel.write(buffer, buffer, new WriteHandler(channel, scanner));
        }
    }
}