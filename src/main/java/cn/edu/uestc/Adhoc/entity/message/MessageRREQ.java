package cn.edu.uestc.Adhoc.entity.message;


import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;
import cn.edu.uestc.Adhoc.utils.MessageUtils;

import java.util.Arrays;

public class MessageRREQ extends Message {
    private static final int DEFAULT_BYTE = 34;
    //转发节点的IP
    private int routeIP;
    //系统信息，包含了处理器个数以及内存大小
    private SystemInfo systemInfo;
    //跳数
    private int hop;
    /**
     * 数据序列号，如果收到的数据小于或者等于节点存储的序列号时，则抛弃该数据，不做处理，避免形成广播风暴
     * 比如说A节点广播了某一次路由请求，B节点收到该请求再次广播，则A节点就会收到该广播，所以通过该属性来判
     * 断这次数据帧是同一次广播，进而不做理会
     */
    protected int seqNum;

    public MessageRREQ() {
    }

    public MessageRREQ(int routeIP, int hop, int seqNum, SystemInfo systemInfo) {
        this.routeIP = routeIP;
        this.systemInfo = systemInfo;
        this.hop = hop;
        this.seqNum = seqNum;
    }

    public int getRouteIP() {
        return routeIP;
    }

    public void setRouteIP(int routeIP) {
        this.routeIP = routeIP;
    }

    public int getHop() {
        return hop;
    }

    public void setHop(int hop) {
        this.hop = hop;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    //将Message对象转化为byte数组，便于发送
    public byte[] getBytes() {
        byte[] srcByte = MessageUtils.IntToBytes(getSrcIP());
        byte[] routeByte = MessageUtils.IntToBytes(routeIP);
        byte[] destinationByte = MessageUtils.IntToBytes(getDestinationIP());
        byte[] sysByte = systemInfo.getBytes();
        byte[] messageByte = new byte[DEFAULT_BYTE];
        messageByte[0] = RouteProtocol.frameHeader[0];
        messageByte[1] = RouteProtocol.frameHeader[1];//帧头,0,1
        messageByte[2] = DEFAULT_BYTE;
        messageByte[3] = (byte)this.type;//数据类型,3

        messageByte[4] = srcByte[0];
        messageByte[5] = srcByte[1];//源节点,4,5

        messageByte[6] = routeByte[0];
        messageByte[7] = routeByte[1];//转发节点,5,6

        messageByte[8] = destinationByte[0];
        messageByte[9] = destinationByte[1];//目标节点8,9

        messageByte[10] = (byte) seqNum;//序列号,10
        messageByte[11] = (byte) hop;//跳数,11
        System.arraycopy(sysByte,0,messageByte,12,sysByte.length);//系统信息,12-31

        messageByte[32] = RouteProtocol.frameEnd[0];
        messageByte[33] = RouteProtocol.frameEnd[1];//帧尾,32,33

        return messageByte;
    }

    //将byte数组转化为Message对象
    public static MessageRREQ recoverMsg(byte[] bytes) {
        ///恢复byte数组中的数据
        int srcIP = MessageUtils.BytesToInt(new byte[]{bytes[4], bytes[5]});
        int routeIP = MessageUtils.BytesToInt(new byte[]{bytes[6], bytes[7]});
        int destinationIP = MessageUtils.BytesToInt(new byte[]{bytes[8], bytes[9]});
        byte seqNum = bytes[10];
        byte hop = bytes[11];
        byte[] sysInfoByte = new byte[SystemInfo.DEFAULT_BYTE];
        System.arraycopy(bytes,12,sysInfoByte,0,SystemInfo.DEFAULT_BYTE);
        SystemInfo sysInfo = SystemInfo.recoverSysInfo(sysInfoByte);

        MessageRREQ message = new MessageRREQ(routeIP, hop, seqNum, sysInfo);
        message.setSrcIP(srcIP);
        message.setDestinationIP(destinationIP);
        message.setType(RouteProtocol.RREQ);

        return message;
    }

    @Override
    public String toString() {
        return "MessageRREQ{" +
                "routeIP=" + MessageUtils.showHex(routeIP) +
                ", systemInfo=" + systemInfo +
                ", hop=" + hop +
                ", seqNum=" + seqNum +
                '}';
    }

    public static void main(String[] args){
        MessageRREQ messageRREQ = new MessageRREQ(2,3,4,new SystemInfo((byte)3));

        byte[] bytes = messageRREQ.getBytes();
        System.out.println(bytes.length+":::"+ Arrays.toString(bytes));
        MessageRREQ messageRREQ1 = recoverMsg(bytes);
        System.out.println(messageRREQ1);
     }
}
