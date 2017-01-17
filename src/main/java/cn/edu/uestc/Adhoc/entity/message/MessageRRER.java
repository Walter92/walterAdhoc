package cn.edu.uestc.Adhoc.entity.message;

import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;
import cn.edu.uestc.Adhoc.utils.MessageUtils;

/**
 * Created by walter on 17-1-5.
 */
public class MessageRRER extends Message {

    private static final byte DEFAULT_BYTE = 10;


    public MessageRRER(Integer srcIP,Integer destIP){
        this.srcIP = srcIP;
        this.destinationIP = destIP;
    }
    @Override
    public byte[] getBytes() {
        byte[] srcByte = MessageUtils.IntToBytes(getSrcIP());
        byte[] destinationByte = MessageUtils.IntToBytes(getDestinationIP());
        byte[] messageByte = new byte[DEFAULT_BYTE];
        messageByte[0] = RouteProtocol.frameHeader[0];
        messageByte[1] = RouteProtocol.frameHeader[1];//帧头,0,1
        messageByte[2] = DEFAULT_BYTE;
        messageByte[3] = RouteProtocol.RRER;//数据类型,3

        messageByte[4] = srcByte[0];
        messageByte[5] = srcByte[1];//源节点,4,5

        messageByte[6] = destinationByte[0];
        messageByte[7] = destinationByte[1];//目标节点8,9

        messageByte[8] = RouteProtocol.frameEnd[0];
        messageByte[9] = RouteProtocol.frameEnd[1];//帧尾,32,33

        return messageByte;
    }



    //将byte数组转化为Message对象
    public static MessageRRER recoverMsg(byte[] bytes) {
        ///恢复byte数组中的数据
        int srcIP = MessageUtils.BytesToInt(new byte[]{bytes[4], bytes[5]});
        int destinationIP = MessageUtils.BytesToInt(new byte[]{bytes[6], bytes[7]});

        MessageRRER message = new MessageRRER(srcIP,destinationIP);

        return message;
    }
}
