package cn.edu.uestc.Adhoc.entity.message;

import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.utils.MessageUtils;

/**
 * hello 信息类
 * Created by walter on 16-1-11.
 */
public class MessageHello extends Message {
    private static final int DEFAULT_BYTE = 8;
    public MessageHello(int srcIp) {
        this.type = RouteProtocol.HELLO;
        this.srcIP = srcIp;
    }

    //编码，将hello报文
    @Override
    public byte[] getBytes() {
        byte[] srcByte = MessageUtils.IntToBytes(getSrcIP());
        byte[] messageByte = new byte[DEFAULT_BYTE];
        messageByte[0] = RouteProtocol.frameHeader[0];
        messageByte[1] = RouteProtocol.frameHeader[1];//帧头,0,1
        messageByte[2] = DEFAULT_BYTE;//帧头,0,1

        messageByte[3] = RouteProtocol.HELLO;//数据类型,2
        messageByte[4] = srcByte[0];
        messageByte[5] = srcByte[1];//源节点,3,4
        messageByte[6] = RouteProtocol.frameEnd[0];
        messageByte[7] = RouteProtocol.frameEnd[1];//帧尾
        return messageByte;
    }

    //将字节恢复为HelloMessage
    public static MessageHello recoverMsg(byte[] bytes) {
        ///恢复byte数组中的数据
        int srcIP = MessageUtils.BytesToInt(new byte[]{bytes[4], bytes[5]});
        MessageHello message = new MessageHello(srcIP);
        message.setType(RouteProtocol.HELLO);
        return message;
    }
}
