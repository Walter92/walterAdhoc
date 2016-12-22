package cn.edu.uestc.Adhoc.entity.adhocNode;

import cn.edu.uestc.Adhoc.entity.message.*;
import cn.edu.uestc.Adhoc.entity.route.RouteEntry;
import cn.edu.uestc.Adhoc.entity.route.RouteProtocol;
import cn.edu.uestc.Adhoc.entity.route.StateFlags;
import cn.edu.uestc.Adhoc.entity.serial.Serial;
import cn.edu.uestc.Adhoc.entity.serial.SerialPortEvent;
import cn.edu.uestc.Adhoc.entity.serial.SerialPortListener;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;
import cn.edu.uestc.Adhoc.entity.transfer.AdhocTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统信息的记录暂时还没有得到利用。
 * 初始想法：从一个节点到另一个节点会经过多个节点是吧？所以整个路由路径上性能最低的节点就是木桶中最低的一块木板，所以只要记录
 * 路径中性能最差的一个节点的信息，就可以知道这条路径上能发送多大的数据。因此在转发路由请求的过程中，每一个节点检查请求中的消
 * 息中的最低节点性能信息，通过和自身比较，如果自身低于该性能，则重新设置最低性能信息，之后再转发出去。ps：路由请求信息应该还
 * 要增加一个字段来描述整个路由路径中最低节点的性能信息。
 * <p/> 
 * 想要设置路由表项的有效时间，可以在新建该表项时增加一个时间戳，当访问该表项时带着时间戳去访问，如果当前时间大于了表项的时间戳
 * n（设置的失效时间）则该表项被判定为无效，应该从路由表中删除。
 */
public class AdhocNode implements IAdhocNode, SerialPortListener {
    private static final Logger logger = LoggerFactory.getLogger(AdhocNode.class);
    //轮训次数
    private final static int POLLING_COUNT = 5;

    //每次轮训的定时时间
    private static final int POLING_TIMER = 1000;

    //自主网节点使用的串口对象，同时也是要监听的时间源
    private AdhocTransfer adhocTransfer;

    // 节点IP地址
    private final int ip;

    //和开启的串口端口名字
    private String portName;

    //节点发出的序列号，该节点每发送出一次RREQ或者RREP时都会在该寻列号上加一，用以标识这是否是一次新的路由请求或者路由回复
    private byte seqNum;

    //节点的路由表,使用同步的路由表
    private Map<Integer, RouteEntry>  routeTable = new ConcurrentHashMap<Integer, RouteEntry> ();

    //先驱列表，存储了本节点周围的节点地址，其存在的目的主要用于路由维护
    private HashSet<Integer>  precursorIPs = new HashSet<Integer> ();

    //接收到的hello报文的发送者队列，当收到某hello报文时将其加入到队列中，路由维护线程从对列中取出数据，用于更新路由表项的生存时间
    private final Queue<Integer>  helloMessagesQueue = new ArrayDeque<Integer> ();

    // 节点的处理器个数以及最大内存
    private SystemInfo systemInfo = new SystemInfo();

    //获取路由表,测试用
    public Map<Integer, RouteEntry>  getRouteTable() {
        return this.routeTable;
    }

    // 获取节点IP
    public int getIp() {
        return ip;
    }

    //设置节点ip

    public AdhocTransfer getAdhocTransfer() {
        return this.adhocTransfer;
    }

    // 节点系统信息
    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public HashSet<Integer>  getPrecursorIPs() {
        return precursorIPs;
    }

    public void setPrecursorIPs(HashSet<Integer>  precursorIPs) {
        this.precursorIPs = precursorIPs;
    }


    // 通过串口名字构造一个结点
    public AdhocNode(String portName, int ip) {
        // 设置通信的串口
        this.ip = ip;
        this.portName = portName;
        this.seqNum = 0;
        adhocTransfer =  Serial.getInstance(this.portName);

        //节点对串口进行监听
        adhocTransfer.addReceiveListener(this);
        logger.debug("<{}> 节点监听串口状态...", this.ip);
        adhocTransfer.receive();

        logger.debug("<{}> 启动读线程，初始化完成，等待数据到达...", this.ip);
        logger.debug("\n*******************************************");
    }

    //当串口中数据被更新后执行的方法
    //adhocTransfer中message属性被更新后执行
    @Override
    public void doSerialPortEvent(SerialPortEvent serialPortEvent) {
        this.dataParsing(adhocTransfer.getMessage());
    }

    //发起对某节点的路由请求
    @Override
    public void sendRREQ(int destinationIP) {
        logger.info("<{}> 准备发送路由请求到 <{}>...", this.ip, destinationIP);
        //本节点对目标节点发出一次RREQ，发出后把seqNum参数加一，以便下次在发出RREQ时为最新请求
        if(destinationIP==this.ip)return;
        MessageRREQ messageRREQ = new MessageRREQ();
        messageRREQ.setType(RouteProtocol.RREQ);
        messageRREQ.setRouteIP(ip);
        messageRREQ.setDestinationIP(destinationIP);
        messageRREQ.setSeqNum(++seqNum);
        messageRREQ.setSrcIP(ip);
        messageRREQ.setSystemInfo(systemInfo);
        messageRREQ.setHop((byte) 0);
        //节点想要给目的节点发送消息，首先查询本节点中路由表是否有可用的有效路由，如果没有就发起路由请求
        RouteEntry routeEntry = queryRouteTable(destinationIP);
        if (routeEntry == null) {
            logger.debug("<{}> 没有有效路由到 <{}>,首先发送路由请求...", this.getIp(), destinationIP);
            try {
                adhocTransfer.send(messageRREQ);
                logger.debug("<{}> 发送路由请求到 <{}> 成功，等待路由回复...", this.ip, destinationIP);
            } catch (IOException e) {
                logger.error("<{}> 发送路由请求到 <{}> 失败!", this.ip, destinationIP);
            }
            //需要等待路由回复.....轮番查询五次，每次等待1秒，如果五次查询都失败，则宣布路由寻找失败
            try {
                for (int i = 1; i <= POLLING_COUNT; i++) {
                    logger.debug("<{}> 正在等待 <{}> 的路由回复...", this.ip, destinationIP, i);
                    Thread.sleep(POLING_TIMER);
                    routeEntry = queryRouteTable(destinationIP);
                    if (routeEntry == null) {
                        if (i == POLLING_COUNT) {
                            logger.warn("<{}> 对 <{}> 的路由发现失败!", this.ip, destinationIP);
                            //System.exit(1);
                        }
                        continue;
                    } else {
                        logger.debug("<{}> 对 <{}> 的路由发现成功!", this.ip, destinationIP);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            logger.warn("<{}> 已经存在到 <{}> 的有效路由.",ip,destinationIP);
        }
    }

    /**
     * @param messageRREQ 收到的信息对象
     *                    首先判断本机路由表中是否有该信息中源地址的表项，如果有并且比收到的信息中的序列号大，则丢弃该信息不做处理
     *                    否则新建一个路由表项，以源地址为键，如果是直接收到源节点的请求，信息中转发节点就是源节点，可以直接用于建立去往源节点
     *                    的下一跳节点，建立反向路由
     */
    //在数据类型方法解析后调用，开始解析数据中内容，判断是否为发送给自己的RREQ
    @Override
    public void receiveRREQ(MessageRREQ messageRREQ) {
        logger.debug("<{}>  收到 <{}> 发送给 <{}> 的路由请求,正在处理中...", this.ip, messageRREQ.getSrcIP(), messageRREQ.getDestinationIP());
        //将转发该消息的节点地址加入到先驱列表中
        precursorIPs.add(messageRREQ.getRouteIP());
        int key = messageRREQ.getSrcIP();
        if(key==this.ip)return;
        //如果收到的信息里面，请求的序列号的键存在，并且小于等于本机所存，则抛弃
        RouteEntry entry = routeTable.get(key);
        if (entry!=null && entry.getSeqNum() >= messageRREQ.getSeqNum()) {
            logger.warn("<{}> 抛弃 <{}> 的路由请求中的旧的序列号 <{}> , 路由表中的最新序列号为 <{}> ,the ",ip,messageRREQ.getSrcIP(),messageRREQ.getSeqNum(),entry.getSeqNum());
            return;
        } else {
            RouteEntry routeEntry = new RouteEntry(key, messageRREQ.getRouteIP(),
                    messageRREQ.getSeqNum(), StateFlags.VALID, messageRREQ.getHop()+1, 0, messageRREQ.getSystemInfo());
            logger.debug("<{}> 添加新的路由表项： {}",this.ip,routeEntry);
            //更新自己的路由表，路由表项的信息为该信息的源节点为目的地址，去往该目的地址的下一跳节点即为转发该信息的节点
            //RouteEntry(String destinationIP, int seqNum, StateFlags state, int hopCount, String nextHopIP, int lifetime)
            routeTable.put(key, routeEntry);
        }
        //如果收到的信息中是寻找本机，则回复路由响应
        if (ip == (messageRREQ.getDestinationIP())) {
            logger.debug("<{}> 收到来自 <{}> 的对自身路由请求，它的系统信息为：<{}> ",ip, messageRREQ.getSrcIP(),messageRREQ.getSystemInfo().toString());
            RouteEntry routeEntry = queryRouteTable(messageRREQ.getSrcIP());
            MessageRREP messageRREP = new MessageRREP(ip, 0, ++seqNum, systemInfo);

            messageRREP.setSrcIP(this.ip);
            messageRREP.setDestinationIP(messageRREQ.getSrcIP());
            messageRREP.setRouteIP(this.ip);
            messageRREP.setNextHopIp(routeEntry.getNextHopIP());
            sendRREP(messageRREP);
            return;
        }
        RouteEntry routeEntry = queryRouteTable(messageRREQ.getDestinationIP());
        //如果本机有到要寻找的目的节点的路由，则回复路由请求
        if (routeEntry != null) {
            logger.debug("<{}> 有对 <{}> 的有效路由，将代替它提前回复..." ,this.ip,
                    messageRREQ.getDestinationIP());
            MessageRREP messageRREP = new MessageRREP(ip, 0, seqNum++, routeEntry.getSystemInfo());
            messageRREP.setDestinationIP(messageRREQ.getSrcIP());
            messageRREP.setSrcIP(messageRREQ.getDestinationIP());
            messageRREP.setRouteIP(this.ip);
            messageRREP.setNextHopIp(queryRouteTable(messageRREQ.getSrcIP()).getNextHopIP());
            sendRREP(messageRREP);
            return;
        }
        //如果信息中不是在寻找本机，则给跳数加一和更新转发节点ip后转发该请求
        messageRREQ.setHop((byte) (messageRREQ.getHop() + 1));
        messageRREQ.setRouteIP(this.ip);
        //转发
        forwardRREQ(messageRREQ);

    }

    //如果不是发送给自己的RREQ则将其转发出去
    @Override
    public void forwardRREQ(MessageRREQ messageRREQ) {
        logger.debug("<{}> 转发 <{}> 发送给 <{}> 的路由请求...", this.ip, messageRREQ.getSrcIP(), messageRREQ.getDestinationIP());
        try {
            adhocTransfer.send(messageRREQ);
            logger.debug("<{}> 转发 <{}>  发送给 <{}> 的路由请求成功!", this.ip, messageRREQ.getSrcIP(), messageRREQ.getDestinationIP());
        } catch (IOException e) {
            logger.error("<{}> 转发 <{}>  发送给 <{}> 的路由请求失败!", this.ip, messageRREQ.getSrcIP(), messageRREQ.getDestinationIP());
        }
    }

    //对请求自己路由回复路由请求
    @Override
    public void sendRREP(MessageRREP messageRREP) {
        logger.debug("<{}> 发送路由回复给 <{}> ...", this.ip, messageRREP.getDestinationIP());
        try {
            adhocTransfer.send(messageRREP);
            logger.debug("<{}> 发送给 <{}> 的路由回复成功!", this.ip, messageRREP.getDestinationIP());
        } catch (IOException e) {
            logger.warn("<{}> 发送给 <{}> 的路由回复失败!", this.ip, messageRREP.getDestinationIP());

        }
    }

    //在数据类型方法解析后调用，开始解析数据中内容，判断是否为发送给自己的RREP

    /**
     * 目前处理RREP的方式和RREQ一样，都是广播发送，然后再判断处理。
     * RREP应该按照反向路径回复路由，采用单播的方式发送（已经实现）
     *
     * @param messageRREP
     */
    @Override
    public void receiveRREP(MessageRREP messageRREP) {
        logger.debug("<{}> 收到 <{}> 发送给 <{}> 的路由回复, 正在处理中...", this.ip, messageRREP.getSrcIP(), messageRREP.getDestinationIP());
        //将转发该消息的节点地址加入到先驱列表中
        int key = messageRREP.getSrcIP();
        if(key==this.getIp())return;
        precursorIPs.add(messageRREP.getRouteIP());
        //如果收到的信息里面，请求的序列号的键存在，并且小于等于本机所存，则抛弃
        if (routeTable.containsKey(key) && routeTable.get(key).getSeqNum() >= messageRREP.getSeqNum()) {
            logger.warn("<{}> 抛弃旧的序列号 <{}> ...", this.ip,messageRREP.getSeqNum());
            return;
        } else {
            RouteEntry routeEntry = new RouteEntry(key, messageRREP.getRouteIP()
                    , messageRREP.getSeqNum(), StateFlags.VALID, messageRREP.getHop()+1, 0, messageRREP.getSystemInfo());
            logger.debug("<{}> 添加新的路由表项： {}",this.ip,routeEntry);
            routeTable.put(key, routeEntry);
        }
        //如果收到的信息中是寻找本机，则回复路由响应
        if (ip == messageRREP.getDestinationIP()) {
            logger.debug("<{}> 收到来自 <{}> 对自身的路由回复，路由短板为：<{}> ",
                    this.ip, messageRREP.getSrcIP(), messageRREP.getSystemInfo().toString());
            return;
        }
        if(this.ip==messageRREP.getNextHopIp()) {
            logger.debug("<{}> 收到来自 <{}> 发送给 <{}> 的路由回复, 转发中。。。");
            RouteEntry routeEntry = queryRouteTable(messageRREP.getDestinationIP());
            //如果信息中不是回复寻找本机，则给跳数加一和更新转发节点ip后转发该请求
            messageRREP.setHop((byte) (messageRREP.getHop() + 1));
            messageRREP.setRouteIP(this.ip);
            messageRREP.setNextHopIp(routeEntry.getNextHopIP());
            //转发
            forwardRREP(messageRREP);
        }
    }

    //如果不是发送给自己的RREP则将其转发出去
    @Override
    public void forwardRREP(MessageRREP messageRREP) {
        logger.debug("<{}> 转发 <{}> 发送给 <{}> 的路由回复...", this.ip, messageRREP.getSrcIP(), messageRREP.getDestinationIP());
        try {
            adhocTransfer.send(messageRREP);
            logger.debug("<{}> 转发 <{}> 发送给 <{}> 的路由回复成功!", this.ip, messageRREP.getSrcIP(), messageRREP.getDestinationIP());
        } catch (IOException e) {
            logger.warn("<{}> 转发 <{}> 发送给 <{}> 失败!", this.ip, messageRREP.getSrcIP(), messageRREP.getDestinationIP());
        }
    }

    //在接收线程接收到数据后调用，主要解析数据类型，再恢复为相应的Message对象,并传递给相应的接收方法
    @Override
    public void dataParsing(byte[] bytes) {
        byte type = bytes[3];
        //Message message = null;
        //如果是数据类型则恢复为数据MessageData，并且交给数据类型接收方法
        if (type == RouteProtocol.DATA) {
            MessageData message = MessageData.recoverMsg(bytes);
            //  System.out.println(message);
            receiveDATA(message);
        }
        //如果是路由回复类型则恢复为数据MessageRREP，并且交给数据类型接收方法
        else if (type == RouteProtocol.RREP) {
            MessageRREP message = MessageRREP.recoverMsg(bytes);
            receiveRREP(message);
        }
        //如果是路由请求类型则恢复为数据MessageRREQ，并且交给数据类型接收方法
        else if (type == RouteProtocol.RREQ) {
            MessageRREQ message = MessageRREQ.recoverMsg(bytes);
            receiveRREQ(message);
        } else if (type == RouteProtocol.HELLO) {
            //交给处理hello报文的处理函数
            MessageHello message = MessageHello.recoverMsg(bytes);
            helloHandler(message);
        } else {
            logger.warn("非法数据格式!");
        }
    }

    @Override
    public void sendMessage(String context, int destinationIP) {
        //节点想要给目的节点发送消息，首先查询本节点中路由表是否有可用的有效路由，如果没有就发起路由请求
        logger.debug("<{}> 准备发送文本数据包给 <{}>",this.ip,destinationIP);
        RouteEntry routeEntry = queryRouteTable(destinationIP);
        if (routeEntry == null) {
            logger.debug("<{}> 没有到 <{}> 的有效路由,首先发送路由请求 ...", this.getIp(), destinationIP);
            sendRREQ(destinationIP);
        }
        routeEntry = queryRouteTable(destinationIP);
        if (routeEntry != null) {
            //如果路由表中有可用路由则可以向其发送数据
            MessageData messageData = new MessageData(destinationIP, context.getBytes());
            messageData.setSrcIP(ip);
            messageData.setNextIP(routeEntry.getNextHopIP());
            try {
                adhocTransfer.send(messageData);
                logger.debug("<{}> 发送数据包到 <{}> 成功!", this.ip, destinationIP);
            } catch (IOException e) {
                logger.warn("<{}> 发送数据包到 <{}> 失败!", this.ip, destinationIP);
            }
        }else {
            logger.warn("<{}> 寻找对 <{}> 的路由失败, 取消发送文本数据. ",ip,destinationIP);
        }
    }

    @Override
    public void receiveDATA(MessageData messageData) {
        //收到数据类型的信息时不需要检查序列号
        int nextIP = messageData.getNextIP();

        int destinationIP = messageData.getDestinationIP();
        if (destinationIP == ip) {
            logger.debug("<{}> 收到来自 <{}> 的文本数据包, 内容：<{}> ", this.ip, messageData.getSrcIP(), new String(messageData.getContent()));
            return;
        }

        if (nextIP != ip) {
            logger.debug("<{}> 收到来自 <{}> 要发送给 <{}> 的数据包，自身不为中转节点，抛弃!", this.ip,messageData.getSrcIP(), messageData.getDestinationIP());
            return;
        }

        logger.debug("<{}> 收到来自 <{}> 要发送给 <{}> 的数据包，自身为中转节点,正在处理中!", this.ip, messageData.getSrcIP(), messageData.getDestinationIP());

        //查询本节点的路由表，得到去往目的节点的下一跳节点，并改变消息中的下一跳节点地址后转发出去
        int next = queryRouteTable(destinationIP).getNextHopIP();
        messageData.setNextIP(next);

        /**
         * 还要完成的一件事就是在转之前检查SystemInfo，如果本机的SystemInfo评分低于message中的，需要将其重置
         */
        forwardDATA(messageData);
    }

    /**
     * 这个几个forwardXXXX方法的日志记录完全可以用AOP实现，采用动态代理即可，但是劳资现在不想干!!!!
     *
     * @param messageData
     */
    @Override
    public void forwardDATA(MessageData messageData) {
        logger.debug("<{}> 转发 <{}> 发送给 <{}> 的文本数据包...", this.ip, messageData.getSrcIP(), messageData.getDestinationIP());
        try {
            adhocTransfer.send(messageData);
            logger.debug("<{}> 转发文本数据包成功!", this.ip);
        } catch (IOException e) {
            logger.warn("<{}> 发文本数据包失败!", this.ip);
        }
    }

    public RouteEntry queryRouteTable(int destinationIP) {
        RouteEntry routeEntry = routeTable.get(destinationIP);
        if (routeEntry != null && routeEntry.getState() == StateFlags.VALID)
            return routeEntry;
        return null;
    }

    //将接收到的hello报文的源节点IP加入到队列中
    public void helloHandler(Message message) {
        int srcIP = message.getSrcIP();
        //保证线程安全
        synchronized (helloMessagesQueue) {
            int size = helloMessagesQueue.size();
            helloMessagesQueue.add(srcIP);
            if(size==0)
                helloMessagesQueue.notify();
        }
    }


    //路由表维护函数，根据helloIP队列中的IP来维护路由表，在一定时间内没有收到某一节节点的hello报文，则将以该节点为下一中转节点的路由表
    //可用状态设置为不可用，并发送RRER
    private void maintainRouteTable() {

        //路由表维护线程，匿名内部类
        Thread maintainRouteThread = new Thread(new Runnable() {
            //路由维护线程
            @Override
            public void run() {
                int ip1 = 0;
                Set<Integer>  DestinationSet;
                Iterator<Integer>  it;
                while (true) {
                    synchronized (helloMessagesQueue) {
                        while (helloMessagesQueue.size() == 0) {
                            //移除操作，保证线程安全
                            try {
                                helloMessagesQueue.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        ip1 = helloMessagesQueue.remove();
                    }
                    if (ip1 != 0) {
                        DestinationSet = getDestinationIPByNextIP(ip1);
                        it = DestinationSet.iterator();
                        while (it.hasNext()) {
                            routeTable.get(it.next()).setLifeTime(RouteEntry.MAX_LIFETIME);
                        }
                    }
                }
            }
        });

        //将维护线程设置为守护线程
        maintainRouteThread.setDaemon(true);

        maintainRouteThread.start();

    }

    //根据下一跳节点的ip获取目的节点的ip集合，根据该set查找route有效可用的表项
    private Set<Integer>  getDestinationIPByNextIP(int ip) {
        Set<Integer>  sets = new HashSet<Integer> ();
        Iterator<Integer>  it = routeTable.keySet().iterator();
        RouteEntry routeEntry = null;
        int destinationIP;
        while (it.hasNext()) {
            destinationIP = it.next();
            routeEntry = routeTable.get(destinationIP);
            if (routeEntry.getNextHopIP() == ip && routeEntry.getState() == StateFlags.VALID) {
                sets.add(destinationIP);
            }
        }
        return sets;
    }

    public Map<Integer,RouteEntry>  queryAllRoute(){
        return this.getRouteTable();
    }
}
