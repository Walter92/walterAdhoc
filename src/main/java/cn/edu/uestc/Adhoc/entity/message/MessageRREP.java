package cn.edu.uestc.Adhoc.entity.message;


import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;
import cn.edu.uestc.Adhoc.utils.MessageUtils;

import java.util.Arrays;

public class MessageRREP extends Message {
    private static final int DEFAULT_BYTE = 36;
    //转发节点的IP
    private int routeIP;
    //发送的数据长度
    //系统信息，包含了处理器个数以及内存大小
    private SystemInfo systemInfo;

    //下一跳ip
    private int nextHopIp;
    //跳数
    private int hop;
    /**
     * 数据序列号，如果收到的数据小于或者等于节点存储的序列号时，则抛弃该数据，不做处理，避免形成广播风暴
     * 比如说A节点广播了某一次路由请求，B节点收到该请求再次广播，则A节点就会收到该广播，所以通过该属性来判断这次数据帧是同一次广播，进而不做理会
     */
    protected int seqNum;

    public MessageRREP() {
    }

    public MessageRREP(int routeIP, int hop, int seqNum, SystemInfo systemInfo) {
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

    public void setHop(byte hop) {
        this.hop = hop;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(byte seqNum) {
        this.seqNum = seqNum;
    }

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    public int getNextHopIp() {
        return nextHopIp;
    }

    public void setNextHopIp(int nextHopIp) {
        this.nextHopIp = nextHopIp;
    }

    //信息编码
    public byte[] getBytes() {
        byte[] srcByte = MessageUtils.IntToBytes(getSrcIP());
        byte[] routeByte = MessageUtils.IntToBytes(routeIP);
        byte[] nextHopByte = MessageUtils.IntToBytes(nextHopIp);
        byte[] destinationByte = MessageUtils.IntToBytes(getDestinationIP());
        byte[] sysByte = systemInfo.getBytes();
        byte[] messageByte = new byte[DEFAULT_BYTE];
        messageByte[0] = RouteProtocol.frameHeader[0];
        messageByte[1] = RouteProtocol.frameHeader[1];//帧头,0,1
        messageByte[2] = DEFAULT_BYTE;
        messageByte[3] = RouteProtocol.RREP;//数据类型,3

        messageByte[4] = srcByte[0];
        messageByte[5] = srcByte[1];//源节点,4,5

        messageByte[6] = routeByte[0];
        messageByte[7] = routeByte[1];//转发节点,5,6

        messageByte[8] = nextHopByte[0];
        messageByte[9] = nextHopByte[1];//目标节点8,9

        messageByte[10] = destinationByte[0];
        messageByte[11] = destinationByte[1];//目标节点8,9

        messageByte[12] = (byte) seqNum;//序列号,10
        messageByte[13] = (byte) hop;//跳数,11
        System.arraycopy(sysByte,0,messageByte,14,sysByte.length);//系统信息,12-31

        messageByte[34] = RouteProtocol.frameEnd[0];
        messageByte[35] = RouteProtocol.frameEnd[1];//帧尾,32,33

        return messageByte;
    }

    //将byte数组转化为Message对象，解码
    public static MessageRREP recoverMsg(byte[] bytes) {
        ///恢复byte数组中的数据
        int srcIP = MessageUtils.BytesToInt(new byte[]{bytes[4], bytes[5]});
        int routeIP = MessageUtils.BytesToInt(new byte[]{bytes[6], bytes[7]});
        int nextHop = MessageUtils.BytesToInt(new byte[]{bytes[8], bytes[9]});
        int destinationIP = MessageUtils.BytesToInt(new byte[]{bytes[10], bytes[11]});
        byte seqNum = bytes[12];
        byte hop = bytes[13];
        byte[] sysInfoByte = new byte[SystemInfo.DEFAULT_BYTE];
        System.arraycopy(bytes,14,sysInfoByte,0,SystemInfo.DEFAULT_BYTE);
        SystemInfo sysInfo = SystemInfo.recoverSysInfo(sysInfoByte);

        MessageRREP message = new MessageRREP(routeIP, hop, seqNum, sysInfo);
        message.setSrcIP(srcIP);
        message.setNextHopIp(nextHop);
        message.setDestinationIP(destinationIP);
        message.setType(RouteProtocol.RREP);
        return message;
    }

    @Override
    public String toString() {
        return "MessageRREP{" +
                "routeIP=" + MessageUtils.showHex(routeIP) +
                ", systemInfo=" + systemInfo +
                ", hop=" + hop +
                ", seqNum=" + seqNum +
                '}';
    }


    public static void main(String[] args){
        MessageRREP messageRREP = new MessageRREP(2,3,4,new SystemInfo((byte)5));
        System.out.println(messageRREP);
        byte[] bytes = messageRREP.getBytes();
        System.out.println(bytes.length+":::"+ Arrays.toString(bytes));
        MessageRREP messageRREP1 = recoverMsg(bytes);
        System.out.println(messageRREP1);
    }
}
