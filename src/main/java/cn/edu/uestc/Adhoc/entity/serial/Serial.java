package cn.edu.uestc.Adhoc.entity.serial;

import cn.edu.uestc.Adhoc.entity.message.Message;
import cn.edu.uestc.Adhoc.entity.transfer.AdhocTransfer;
import cn.edu.uestc.Adhoc.adhocThread.SerialReadThread;
import cn.edu.uestc.Adhoc.adhocThread.SerialWriteThread;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Enumeration;
import java.util.EventListener;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by walter on 15-12-18.
 * 串口的事件监听机制,串口为事件源,一旦串口中的message发生改变(事件),就会出发自组网节点对象调用dataParsing()
 * 通过串口实现传输层接口,为adhoc节点提供传输
 */
public class Serial implements AdhocTransfer {
    private static final Logger logger = LoggerFactory.getLogger(Serial.class);
    private Vector<EventListener>  repository = new Vector<EventListener> ();
    private SerialPortListener serialPortListener;
    private static final String ADHOC = "Adhoc";
    private static final int BAUD_RATE = 9600;
    //串口的名字
    private String portName;
    private static volatile Serial instance;

    //串口接收到的字节数组,事件机制,当该字段更新时就会触发Adhoc的数据解析方法dataParsing()
    private byte[] message;

    //串口的线程池
    private ExecutorService executorService;

    public SerialPort serialPort;
    public CommPortIdentifier portId;
    @SuppressWarnings("rawtypes")
    //枚举到的本机的串口和并口列表
    public Enumeration portList;

    //串口输入输出流
    private InputStream is;

    //串口输出流
    private OutputStream os;

    private Serial(String portName) {
        this.portName = portName;
        //根据处理器的内核数创建一个具有和内核数一样的固定线程数的线程池
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            init();
        } catch (UnsupportedCommOperationException uce) {
            uce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (PortInUseException pie) {
            pie.printStackTrace();
        }
    }

    public InputStream getIs() {
        return this.is;
    }

    public OutputStream getOs() {
        return this.os;
    }

    public byte[] getMessage() {
        return message;
    }

    //注册监听器,如果这里没有使用Vector而是使用ArrayList那么要注意同步问题
    public void addReceiveListener(EventListener listener) {
        repository.addElement(listener);//这步要注意同步问题
    }

    //如果这里没有使用Vector而是使用ArrayList那么要注意同步问题
    public void notifyAdhocNode(SerialPortEvent event) {
        Enumeration enumeration = repository.elements();//这步要注意同步问题
        while (enumeration.hasMoreElements()) {
            serialPortListener = (SerialPortListener) enumeration.nextElement();
            serialPortListener.doSerialPortEvent(event);
        }
    }

    //删除监听器,如果这里没有使用Vector而是使用ArrayList那么要注意同步问题
    public void removeDemoListener(SerialPortListener serialPortListener) {
        repository.remove(serialPortListener);//这步要注意同步问题
    }

    //更新serial对象的message,该动作将会引发事件
    public void setMessage(byte[] message) {
        boolean bool = false;
        if (message == null && this.message != null)
            bool = true;
        else if (message != null && this.message == null)
            bool = true;
        else if (!this.message.equals(message))
            bool = true;
        this.message = message;
        if (bool)
            notifyAdhocNode(new SerialPortEvent(this));
    }

    // 节点初始化
    private void init() throws UnsupportedCommOperationException,
            IOException, PortInUseException {
        // 首先对电脑的可用端口进行枚举
        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            // 判断端口的类型以及名字,打开需要打开的端口
            portId = (CommPortIdentifier) portList.nextElement();
            System.out.println(portId.getName());
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portName.equals(portId.getName())) {
                    try {
                        // 打开端口,超时时间为2000
                        serialPort = (SerialPort) portId.open(ADHOC, 2000);
                        logger.debug("<{}>  serial port was opened successfully.", portName);
                        break;
                    } catch (PortInUseException e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        }

        try {
            // 初始化输入输出流,为创建收发线程准备
            is = serialPort.getInputStream();
            os = serialPort.getOutputStream();
            //logger.debug("初始化端口IO流成功!");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        try {
            // 设置初始化参数
            logger.debug("init parameters of serial port(baud rate,data bit,stop bit,parity)...");
            serialPort.setSerialPortParams(BAUD_RATE, // 波特率
                    SerialPort.DATABITS_8, // 数据位
                    SerialPort.STOPBITS_1, // 停止位
                    SerialPort.PARITY_NONE);// 校验位
            logger.debug("init parameters of serial port successfully");
        } catch (UnsupportedCommOperationException e) {
            logger.warn("init parameters of serial port failed!");
            throw e;
        }
    }

    @Override
    //串口的写方法
    public void send(Message message) throws IOException {
        try {
            Runnable writer = new SerialWriteThread(this, message);
            executorService.submit(writer);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw ioe;
        }

    }


    @Override
    //串口对象的读方法
    public void receive() {
        Runnable reader = new SerialReadThread(this);
        executorService.submit(reader);
    }

    public static Serial getInstance(String portName){

        if(instance==null)
            synchronized (Serial.class) {
                if(instance==null)
                    instance = new Serial(portName);
            }
        return instance;
    }
}
