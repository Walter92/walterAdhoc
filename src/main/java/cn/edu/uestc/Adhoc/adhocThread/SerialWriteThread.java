package cn.edu.uestc.Adhoc.adhocThread;

import cn.edu.uestc.Adhoc.entity.message.Message;
import cn.edu.uestc.Adhoc.entity.serial.Serial;

import java.io.*;
import java.util.Arrays;

public class SerialWriteThread implements Runnable {

    // 数据输出流
    private OutputStream os;
    private BufferedOutputStream bos;
    //    private ObjectOutputStream objectOutputStream;
    private Message message;

    public SerialWriteThread(Serial serial, Message message) throws IOException {
        this.os = serial.getOs();
        this.bos = new BufferedOutputStream(this.os);
        this.message = message;
    }

    @Override
    public void run() {
//        System.out.println("开始发送数据...");
//        while (true) {
            try {
                byte[] bytes=message.getBytes();
                System.out.println("发送数据::"+ Arrays.toString(bytes));
                bos.write(bytes);
//                bos.write("hello world...".getBytes());
                bos.flush();
                Thread.sleep(1000);
            } catch (IOException e) {
                System.out.println("发送异常...");
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//        }
    }
}
