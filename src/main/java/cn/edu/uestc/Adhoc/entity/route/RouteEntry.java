package cn.edu.uestc.Adhoc.entity.route;

import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;
import cn.edu.uestc.Adhoc.utils.MessageUtils;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * AODV Node Routing Table Entry
 * 1. 目的节点IP地址：每个表项都具有不同的目的路由器IP地址，以此作为区别和查找网络路由的关键字；
 * 2. 目的节点序列号：
 * 可防止网络产生环路，只有当收到的RREP和RREQ中传送的序列号比本节点对应路由表项中目的序列号大时
 * 才能被接收并用于更新路由表项；
 * 3. 目的节点序列号是否正确的标志：显示路由表项中目的序列号是否有效；
 * 4. 网络接口；
 * 5. 下一跳：到达目的节点的路径上的下一跳节点的地址；
 * 6. 跳数：从本节点到达目的路由器所需要的跳数；
 * 7. 生存时间：路由过期或应当删除的时间；
 * 8. 先驱表：先驱链表指针指向使用此路由表项的所有可能的邻居节点地址；
 * 9. 其他状态和路由标志：如路由有效、无效、可修复、正在修复。
 */
public class RouteEntry {
    public static final long MAX_LIFETIME = 1000 * 60;

    //目标节点的IP地址
    private int destIP;
    //目标节点的序列号
    private int seqNum;
    //路由表项当前状态
    private StateFlags state;
    //去往目标节点的跳数
    private int hopCount;
    //去往目标节点的下一节点的IP地址
    private int nextHopIP;

//    //先驱列表，存储了本节点周围的节点地址，其存在的目的主要用于路由维护
//    private HashSet<Integer>  PrecursorIPs = new HashSet<Integer> ();

    //在这个时间内，该表项有效
    private volatile long lifeTime;

    //目标节点的系统信息
    private SystemInfo systemInfo;

    public long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    private long lastModifyTime;

    public RouteEntry(int destIP, int nextHopIP, int seqNum, StateFlags state, int hopCount, long lifeTime, SystemInfo systemInfo,long lastModifyTime) {
        this.destIP = destIP;
        this.seqNum = seqNum;
        this.state = state;
        this.hopCount = hopCount;
        this.nextHopIP = nextHopIP;
        this.lifeTime = lifeTime;
        this.systemInfo = systemInfo;
        this.lastModifyTime = lastModifyTime;
        // 方案一：在构造后就启动一个单独的线程来维护生存时间，
        // 直接缺点就是每一个表项都会开启一个线程，如果表项很多的话会创建很多线程，占用大量系统资源(也许可以这么干，也占不了多少资源)
    }

    public RouteEntry(int destIP, int nextHopIP, int seqNum, StateFlags state, int hopCount,  SystemInfo systemInfo,long lastModifyTime) {
        this(destIP,nextHopIP,seqNum,state,hopCount,MAX_LIFETIME,systemInfo,lastModifyTime);
    }
    public int getDestIP() {
        return destIP;
    }

    public void setDestIP(int destIP) {
        this.destIP = destIP;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public StateFlags getState() {
        return state;
    }

    public void setState(StateFlags state) {
        this.state = state;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public int getNextHopIP() {
        return nextHopIP;
    }

    public void setNextHopIP(int nextHopIP) {
        this.nextHopIP = nextHopIP;
    }

//    public HashSet<Integer>  getPrecursorIPs() {
//        return PrecursorIPs;
//    }
//
//    public void setPrecursorIPs(HashSet<Integer>  precursorIPs) {
//        PrecursorIPs = precursorIPs;
//    }

    public long getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(long lifeTime) {
        this.lifeTime = lifeTime;
    }

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    @Override
    public String toString() {
        return "RouteEntry{" +
                "destinationIP=" + MessageUtils.showHex(destIP) +
                ", seqNum=" + seqNum +
                ", state=" + state +
                ", hopCount=" + hopCount +
                ", nextHopIP=" + MessageUtils.showHex(nextHopIP) +
                ", lifeTime=" + lifeTime +
                ", systemInfo=" + systemInfo +
                ", lastModifyTime=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastModifyTime)) +
                '}';
    }

    public String printTable(){
        return "|"+MessageUtils.showHex(destIP)+"  |"+seqNum+"     |"+state+"|"+hopCount+"       |"+MessageUtils.showHex(nextHopIP)+"     |"+lifeTime+"       |"+systemInfo+"|"+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastModifyTime))+"|";
    }
    //根据生存时间，表项失效设置
    public void setInvalid() {
        while (true) {
            try {

                Thread.sleep(1000);
                this.lifeTime = this.lifeTime - 1000;
                if (lifeTime <= 0) {
                    this.state = StateFlags.INVALID;
                    break;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
