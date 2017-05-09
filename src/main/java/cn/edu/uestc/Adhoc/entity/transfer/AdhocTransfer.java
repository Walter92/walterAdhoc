package cn.edu.uestc.Adhoc.entity.transfer;

import cn.edu.uestc.Adhoc.entity.message.Message;
import cn.edu.uestc.Adhoc.entity.route.RouteEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EventListener;

/**
 * adhoc 的传输层接口,实现该接口可以为adhoc节点提供传输
 */
public interface AdhocTransfer {
    //接收信息
    void receive();

    //发送一种数据类型的信息
    void send(Message message) throws IOException;
    void send(int level ,Message message) throws IOException;

    //添加信息监听器
    void addReceiveListener(EventListener listener);

    byte[] getMessage();

    void setMessage(byte[] message);

    OutputStream getOs();

    InputStream getIs();

}
