package cn.edu.uestc.Adhoc.entity.message;


import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.utils.MessageUtils;

import java.util.Arrays;

/**
 * Created by walter on 15-12-11.
 */
public class MessageData extends Message {
    //去往目标节点的下一跳节点地址
    private int nextIP;
    //数据长度
    private int dataLen;
    //数据的内容
    private byte[] content;

    public MessageData() {
    }

    public MessageData(int destinationIP, byte[] content) {
        this.destinationIP = destinationIP;
        this.dataLen = content.length;
        this.content = content;
    }

    public int getNextIP() {
        return nextIP;
    }

    public void setNextIP(int nextIP) {
        this.nextIP = nextIP;
    }

    public int getDataLen() {
        return dataLen;
    }

    public void setDataLen(int dataLen) {
        this.dataLen = dataLen;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public byte[] getBytes() {
        byte[] srcByte = MessageUtils.IntToBytes(getSrcIP());
        byte[] nextByte = MessageUtils.IntToBytes(nextIP);
        byte[] destinationByte = MessageUtils.IntToBytes(getDestinationIP());
        int IPAndSoOnLen = 3 * 2 + 1 + 1;//三个IP长度加上数据类型，加上数据长度变量所占长度
        int len = RouteProtocol.PROTOCOL_LEN * 2 + dataLen + IPAndSoOnLen;
        byte[] messageByte = new byte[len];
        messageByte[0] = RouteProtocol.frameHeader[0];
        messageByte[1] = RouteProtocol.frameHeader[1];//帧头,0,1

        messageByte[2] = RouteProtocol.DATA;//数据类型,2

        messageByte[3] = srcByte[0];
        messageByte[4] = srcByte[1];//源节点,3,4

        messageByte[5] = nextByte[0];
        messageByte[6] = nextByte[1];//转发节点,5,6

        messageByte[7] = destinationByte[0];
        messageByte[8] = destinationByte[1];//目标节点7,8

        messageByte[9] = (byte) dataLen;
        //待发送的字节数组
        for (int i = 10; i < len - 2; i++) {
            messageByte[i] = content[i - 10];
        }

        messageByte[len - 2] = RouteProtocol.frameEnd[0];
        messageByte[len - 1] = RouteProtocol.frameEnd[1];//帧尾
        return messageByte;
    }

    @Override
    public String toString() {
        return "MessageData{" +
                "nextIP=" + nextIP +
                ", dataLen=" + dataLen +
                ", content=" + new String(content) +
                '}';
    }

    //将byte数组转化为Message对象
    public static MessageData recoverMsg(byte[] bytes) {
        ///恢复byte数组中的数据
        int srcIP = MessageUtils.BytesToInt(new byte[]{bytes[3], bytes[4]});
        int nextIP = MessageUtils.BytesToInt(new byte[]{bytes[5], bytes[6]});
        int destinationIP = MessageUtils.BytesToInt(new byte[]{bytes[7], bytes[8]});
        byte dataLength = bytes[9];
        byte[] information = Arrays.copyOfRange(bytes, 10, bytes.length - 2);

        MessageData message = new MessageData(nextIP, information);
        message.setSrcIP(srcIP);
        message.setDataLen(dataLength);
        message.setDestinationIP(destinationIP);
        message.setType(RouteProtocol.DATA);

        return message;
    }
}
