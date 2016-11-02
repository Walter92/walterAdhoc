package cn.edu.uestc.Adhoc.entity.message;

import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.utils.MessageUtils;

/**
 * hello 信息类
 * Created by walter on 16-1-11.
 */
public class MessageHello extends Message {

    public MessageHello(int srcIp, int destinationIp){
        this.type= RouteProtocol.HELLO;
        this.srcIP=srcIp;
        this.destinationIP =destinationIp;
    }

    //编码，将hello报文
    @Override
    public byte[] getBytes() {
        byte[] srcByte = MessageUtils.IntToBytes(getSrcIP());
        byte[] destinationByte = MessageUtils.IntToBytes(getDestinationIP());
        byte[] messageByte = new byte[9];
        messageByte[0] = RouteProtocol.frameHeader[0];
        messageByte[1] = RouteProtocol.frameHeader[1];//帧头,0,1
        messageByte[2] = RouteProtocol.HELLO;//数据类型,2
        messageByte[3] = srcByte[0];
        messageByte[4] = srcByte[1];//源节点,3,4
        messageByte[5] = destinationByte[0];
        messageByte[6] = destinationByte[1];//目标节点5,6
        messageByte[7] = RouteProtocol.frameEnd[0];
        messageByte[8] = RouteProtocol.frameEnd[1];//帧尾
        return messageByte;
    }

    //将字节恢复为HelloMessage
    public static MessageHello recoverMsg(byte[] bytes){
        ///恢复byte数组中的数据
        int srcIP = MessageUtils.BytesToInt(new byte[]{bytes[3], bytes[4]});
        int destinationIP = MessageUtils.BytesToInt(new byte[]{bytes[5], bytes[6]});

        MessageHello message = new MessageHello(srcIP, destinationIP);
        message.setType(RouteProtocol.HELLO);
        return message;
    }
}
