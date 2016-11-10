package cn.edu.uestc.Adhoc.adhocThread;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.TooManyListenersException;

import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.entity.serial.Serial;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialReadThread implements Runnable, SerialPortEventListener {
    private static final Logger logger = LoggerFactory.getLogger(SerialReadThread.class);
    byte[] buff = new byte[0];
    private Serial serial;
    private InputStream is;
    private BufferedInputStream bis;
    private Thread readThread;

    public SerialReadThread(Serial serial) {
        this.serial = serial;
        this.is = serial.getIs();
        bis = new BufferedInputStream(this.is);
        try {
            // 在节点上注册事件监听器
            logger.debug("init input stream success！！");
            logger.debug("register listener to serial port...");
            serial.serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        } catch (Exception e) {
            logger.warn("init stream failed！！");
            e.printStackTrace();
        }
        // 通知数据可用，开始读数据
        serial.serialPort.notifyOnDataAvailable(true);
        readThread = new Thread(this);
        readThread.start();
        logger.debug("init reader thread done！");
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 节点事件
    @Override
    public void serialEvent(SerialPortEvent event) {

        switch (event.getEventType()) {// 根据时间类型做出相应反应
            case SerialPortEvent.BI:// 通讯中断
            case SerialPortEvent.OE:// 溢位错误
            case SerialPortEvent.FE:// 传帧错误
            case SerialPortEvent.PE:// 校验错误
            case SerialPortEvent.CD:// 载波检测
            case SerialPortEvent.CTS:// 清除发送
            case SerialPortEvent.DSR:// 数据设备准备就绪
            case SerialPortEvent.RI:// 响铃指示
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:// 输出缓冲区清空
                break;
            case SerialPortEvent.DATA_AVAILABLE:// 数据到达
                byte[] buf = new byte[1024];

                byte[] bytes = null;
                int lengthOfBuff = 0;
                try {
                    int numBytes = -1;
                    while ((numBytes = is.available()) > 0) {
                        is.read(buf, 0, numBytes);
                        bytes = Arrays.copyOfRange(buf, 0, numBytes);
                        //将读取到的数据输出到控制台


                        /**
                         * 用ZigBee模块发送数据的时候有一次性接收不全，自动换行的现象发生，为了解决这个问题，在这里
                         * 添加一个缓冲，当数据接收完整之后再交给下一步程序去处理。通过校验接收到的数据的帧尾，来判断数据是否完整。
                         */

                        lengthOfBuff = buff.length;

                        //将数组buff扩充容量为本身长度加bytes的长度
                        buff = Arrays.copyOf(buff, bytes.length + lengthOfBuff);
                        //将数组合并，把bytes的内容追加到buff
                        System.arraycopy(bytes, 0, buff, lengthOfBuff, bytes.length);

                        //logger.debug("收到数据:" + Arrays.toString(bytes));
//                        System.out.println("Buff::::" + Arrays.toString(buff));
                        if (buff[0] == RouteProtocol.frameHeader[0] && buff[1] == RouteProtocol.frameHeader[1]) {
//                            System.out.println("帧头校验成功！");
                            lengthOfBuff = buff.length;
                            if (buff[lengthOfBuff - 1] == RouteProtocol.frameEnd[1]
                                    && buff[lengthOfBuff - 2] == RouteProtocol.frameEnd[0]) {
//                                System.out.println("帧尾校验成功!");
//                                System.out.println("完整数据:" + new String(buff) + "::" + lengthOfBuff);

                                serial.setMessage(buff);
                                buff = new byte[0];
                            }
                        }


                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
