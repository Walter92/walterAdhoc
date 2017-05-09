package cn.edu.uestc.Adhoc.adhocThread;

import cn.edu.uestc.Adhoc.entity.message.Message;
import cn.edu.uestc.Adhoc.entity.message.MessageData;
import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.entity.serial.Serial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SerialWriteThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SerialWriteThread.class);
    // 数据输出流
    private OutputStream os;
    private BufferedOutputStream bos;
    //    private ObjectOutputStream objectOutputStream;
    private Message message;
    private int level;
    public SerialWriteThread(Serial serial, Message message) throws IOException {
        this(0,serial,message);
    }

    public SerialWriteThread(int level,Serial serial,Message message)throws IOException{
        this.level = level;
        this.os = serial.getOs();
        this.bos = new BufferedOutputStream(this.os);
        this.message = message;
    }

    @Override
    public void run() {
        try {
            byte[] bytes = message.getBytes();
            if(message.getType()== RouteProtocol.DATA)
                limitSpeed(level);//根据路由短板性能等级进行速度限制
            bos.write(bytes);
            bos.flush();
        } catch (IOException e) {
            logger.warn("sent exception,failed!");
            e.printStackTrace();
        }
    }

    private void limitSpeed(int level){
       // int level = this.level;
        try {
            TimeUnit.MILLISECONDS.sleep(0x010000>>level);
            logger.debug("speed limit...............................................................................................");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
