import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class test2 {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", 8888), 50);
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    //将缓冲区数据传给request对象
                    //发送给response对象
                    ByteBuffer buffer = ByteBuffer.wrap(new byte[1024]);
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    int read = clientChannel.read(buffer);

                    if (read == -1) {
                        key.cancel();
                        clientChannel.close();
                    } else {
                        buffer.flip();
                        clientChannel.write(buffer);
                    }

                    int buffer_size = 1024;
                    ByteBuffer bufferNIO = ByteBuffer.wrap(new byte[buffer_size]);
                    SocketChannel clientChannelNIO = (SocketChannel) key.channel();
                    List<Byte> result = new ArrayList<>();
                    while (true) {
                        int length = clientChannelNIO.read(bufferNIO);
                        if(length == -1) {
                            key.cancel();
                            clientChannel.close();
                            break;
                        }
                        else {
                            bufferNIO.flip();
                            for (int i = 0; i < length; i ++) {
                                result.add(bufferNIO.get());
                            }
                        }
                    }
                    byte[] resultByte = new byte[result.size()];
                    for(int i = 0; i < result.size(); i ++) {
                        resultByte[i] = result.get(i);
                    }

                }
            }
            iterator.remove();
        }
    }
}
