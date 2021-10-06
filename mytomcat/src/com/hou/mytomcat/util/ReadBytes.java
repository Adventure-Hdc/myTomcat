package com.hou.mytomcat.util;

import java.io.*;

public class ReadBytes {

    public static byte[] readBytes(InputStream is, boolean fully) throws IOException {
        //设置缓冲区大小
        int buffer_size = 1024;
        //建立字节缓冲区
        byte buffer[] = new byte[buffer_size];
        //字节输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //为了避免信息丢失，一致读取输入流的内容
        while(true) {
            int length = is.read(buffer);
            //字节输入流结束了
            if(-1==length)
                break;
            //将缓冲区里面的内容读取出来
            baos.write(buffer, 0, length);
            if(!fully && length!=buffer_size)
                break;
        }
        byte[] result =baos.toByteArray();
        return result;
    }
}