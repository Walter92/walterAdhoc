package cn.edu.uestc.Adhoc.entity.adhocNode;

import cn.edu.uestc.Adhoc.entity.message.MessageHello;
import cn.edu.uestc.Adhoc.entity.route.RouteEntry;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;
import cn.edu.uestc.Adhoc.entity.transfer.AdhocTransfer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by walter on 17-5-5.
 */
public abstract class AbstractAdhocNode implements IAdhocNode{
    // 节点IP地址
    protected final int ip;

    //和开启的串口端口名字
    protected String portName;
    public AbstractAdhocNode(String portName,int ip){
        this.portName = portName;
        this.ip = ip;
    }
    protected abstract void sendHello( MessageHello messageHello);
    //进行路由维护，从hello报文队列中取出报文进行
    protected abstract void maintainRouteTable();
    //定时周期性遍历路由表，是否有过期路由
    protected abstract void checkRouteTable();
    //根据目标节点获取路由表项
    protected abstract Set<Integer> getDestinationIPByNextIP(int ip);
}
