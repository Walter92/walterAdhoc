//package cn.edu.uestc.Adhoc.entity.message;
//
//import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
//import cn.edu.uestc.Adhoc.utils.MessageUtils;
//
///**
// * Created by walter on 17-2-13.
// */
//public class MessageTest extends Message {
//
//
//    @Override
//    public byte[] getBytes() {
//
//        byte[] messageByte = new byte[32];
//        messageByte[0] = RouteProtocol.frameHeader[0];
//        messageByte[1] = RouteProtocol.frameHeader[1];//帧头,0,1
//        messageByte[2] = len;
//        messageByte[3] = RouteProtocol.DATA;//数据类型,3
//
//        messageByte[4] = srcByte[0];
//        messageByte[5] = srcByte[1];//源节点,3,4
//
//        messageByte[6] = nextByte[0];
//        messageByte[7] = nextByte[1];//转发节点,6,7
//
//        messageByte[8] = destinationByte[0];
//        messageByte[9] = destinationByte[1];//目标节点8,9
//        messageByte[10] = (byte)dataLen;
//        System.arraycopy(content,0,messageByte,11,dataLen);
//
//        messageByte[len - 2] = RouteProtocol.frameEnd[0];
//        messageByte[len - 1] = RouteProtocol.frameEnd[1];//帧尾
//        return messageByte;
//    }
//}
