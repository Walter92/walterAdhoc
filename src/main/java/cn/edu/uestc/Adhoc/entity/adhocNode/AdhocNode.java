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
import cn.edu.uestc.Adhoc.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 系统信息的记录暂时还没有得到利用。
 * 初始想法：从一个节点到另一个节点会经过多个节点是吧？所以整个路由路径上性能最低的节点就是木桶中最低的一块木板,所以只要记录
 * 路径中性能最差的一个节点的信息,就可以知道这条路径上能发送多大的数据。因此在转发路由请求的过程中,每一个节点检查请求中的消
 * 息中的最低节点性能信息,通过和自身比较,如果自身低于该性能,则重新设置最低性能信息,之后再转发出去。ps：路由请求信息应该还
 * 要增加一个字段来描述整个路由路径中最低节点的性能信息。
 * <p/> 
 * 想要设置路由表项的有效时间,可以在新建该表项时增加一个时间戳,当访问该表项时带着时间戳去访问,如果当前时间大于了表项的时间戳
 * n（设置的失效时间）则该表项被判定为无效,应该从路由表中删除。
 */
public class AdhocNode implements IAdhocNode, SerialPortListener {
    private static final Logger logger = LoggerFactory.getLogger(AdhocNode.class);
    //轮训次数
    private final static int POLLING_COUNT = 5;

    //每次轮训的定时时间
    private static final int POLING_TIMER = 1000;

    //自主网节点使用的串口对象,同时也是要监听的时间源
    private AdhocTransfer adhocTransfer;

    //路由维护，维护hello报文队列
    private ReentrantLock lock = new ReentrantLock(true);
    // 节点IP地址
    private final int ip;

    //和开启的串口端口名字
    private String portName;

    //节点发出的序列号,该节点每发送出一次RREQ或者RREP时都会在该寻列号上加一,用以标识这是否是一次新的路由请求或者路由回复
    private byte seqNum;

    //节点的路由表,使用同步的路由表
    private Map<Integer, RouteEntry>  routeTable = new ConcurrentHashMap<Integer, RouteEntry> ();

    //先驱列表,存储了本节点周围的节点地址,其存在的目的主要用于路由维护
    private HashSet<Integer>  precursorIPs = new HashSet<Integer> ();

    //接收到的hello报文的发送者队列,当收到某hello报文时将其加入到队列中,路由维护线程从对列中取出数据,用于更新路由表项的生存时间
    private final Queue<Integer>  helloMessagesQueue = new ArrayDeque<Integer> ();

    // 节点的处理器个数以及最大内存
    private SystemInfo systemInfo ;

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

        init();
        byte performanceLevel = evaluateLevel();
        this.systemInfo = new SystemInfo(performanceLevel);
    }


    private void init() {
        adhocTransfer =  Serial.getInstance(this.portName);

        //节点对串口进行监听
        adhocTransfer.addReceiveListener(this);
        logger.debug("<{}> listens status about serial...", MessageUtils.showHex(this.ip));
        adhocTransfer.receive();

        logger.debug("<{}> started reader thread,waiting for data...", MessageUtils.showHex(this.ip));
        logger.debug("<{}> init done!",MessageUtils.showHex(this.ip));
        logger.debug("*************************************");
    }

    //通过计算发送大量数据所消耗的时间来对该节点的性能进行评级
    private byte evaluateLevel(){
        int count=10;
        long start = System.currentTimeMillis();
        Message testMessage = new MessageData(0,"testLevel".getBytes());
        testMessage.setType(RouteProtocol.TEST);
        while (count>0){
            --count;
            try {
                adhocTransfer.send(testMessage);
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("io exception in evaluating level.");
            }
        }
        long costTime = System.currentTimeMillis()-start;
        System.out.println("costTime = "+costTime);
        if(costTime<500){
            return (byte)5;
        }else if(costTime<1000){
            return (byte)4;
        }else if(costTime<1500){
            return (byte)3;
        }else if(costTime<2000){
            return (byte)2;
        }else {
            return (byte)1;
        }

    }

    //当串口中数据被更新后执行的方法
    //adhocTransfer中message属性被更新后执行
    @Override
    public void doSerialPortEvent(SerialPortEvent serialPortEvent) {

        byte[] bytes = adhocTransfer.getMessage();
        //logger.debug("parsing data..."+Arrays.toString(bytes));
        this.dataParsing(bytes);
    }

    //发起对某节点的路由请求
    @Override
    public void sendRREQ(int destinationIP) {
        logger.info("<{}> begin to send RREQ to <{}> ...", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP));
        //本节点对目标节点发出一次RREQ,发出后把seqNum参数加一,以便下次在发出RREQ时为最新请求
        if(destinationIP==this.ip)return;
        MessageRREQ messageRREQ = new MessageRREQ();
        messageRREQ.setType(RouteProtocol.RREQ);
        messageRREQ.setRouteIP(ip);
        messageRREQ.setDestinationIP(destinationIP);
        messageRREQ.setSeqNum(++seqNum);
        messageRREQ.setSrcIP(ip);
        messageRREQ.setSystemInfo(systemInfo);
        messageRREQ.setHop((byte) 0);
        //节点想要给目的节点发送消息,首先查询本节点中路由表是否有可用的有效路由,如果没有就发起路由请求
        RouteEntry routeEntry = queryRouteTable(destinationIP);
        if (routeEntry == null) {
            logger.debug("<{}> has no routing about <{}> ,sent RREQ firstly...", MessageUtils.showHex(this.getIp()), MessageUtils.showHex(destinationIP));
            try {
                adhocTransfer.send(messageRREQ);
                logger.debug("<{}> sent RREQ to <{}> successfully and waiting for RREP...", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP));
            } catch (IOException e) {
                logger.error("<{}> sent RREQ to <{}> failed!", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP));
            }
            //需要等待路由回复.....轮番查询五次,每次等待1秒,如果五次查询都失败,则宣布路由寻找失败
            try {
                for (int i = 1; i <= POLLING_COUNT; i++) {
                    logger.debug("<{}> waiting RREP from <{}>...", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP), i);
                    Thread.sleep(POLING_TIMER);
                    routeEntry = queryRouteTable(destinationIP);
                    if (routeEntry == null) {
                        if (i == POLLING_COUNT) {
                            logger.warn("<{}> searched for <{}> failed!", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP));
                            //System.exit(1);
                        }
                        continue;
                    } else {
                        logger.debug("<{}> searched for <{}> successfully!", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP));
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            logger.warn("<{}> has route to <{}>.",MessageUtils.showHex(ip),MessageUtils.showHex(destinationIP));
        }
    }

    /**
     * @param messageRREQ 收到的信息对象
     *                    首先判断本机路由表中是否有该信息中源地址的表项,如果有并且比收到的信息中的序列号大,则丢弃该信息不做处理
     *                    否则新建一个路由表项,以源地址为键,如果是直接收到源节点的请求,信息中转发节点就是源节点,可以直接用于建立去往源节点
     *                    的下一跳节点,建立反向路由
     */
    //在数据类型方法解析后调用,开始解析数据中内容,判断是否为发送给自己的RREQ
    @Override
    public void receiveRREQ(MessageRREQ messageRREQ) {
        logger.debug("<{}>  received RREQ about <{}> sent to <{}>,processing...", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREQ.getSrcIP()), MessageUtils.showHex(messageRREQ.getDestinationIP()));
        //将转发该消息的节点地址加入到先驱列表中
        precursorIPs.add(messageRREQ.getRouteIP());
        int key = messageRREQ.getSrcIP();
        if(key==this.ip)return;
        //如果收到的信息里面,请求的序列号的键存在,并且小于等于本机所存,则抛弃
        RouteEntry entry = routeTable.get(key);
        if (entry!=null && entry.getSeqNum() >= messageRREQ.getSeqNum()) {
            logger.warn("<{}> drops the old seq <{}> of <{}>  RREQ, last seq is <{}> ",MessageUtils.showHex(ip),messageRREQ.getSeqNum(),MessageUtils.showHex(messageRREQ.getSrcIP()), entry.getSeqNum());
            return;
        } else {
            int hop = messageRREQ.getHop();
            hop++;
            RouteEntry routeEntry = new RouteEntry(key, messageRREQ.getRouteIP(),
                    messageRREQ.getSeqNum(), StateFlags.VALID, hop, 0, messageRREQ.getSystemInfo(),System.currentTimeMillis());
            logger.debug("<{}> adds new route entry {}",MessageUtils.showHex(this.ip),routeEntry);
            //更新自己的路由表,路由表项的信息为该信息的源节点为目的地址,去往该目的地址的下一跳节点即为转发该信息的节点
            //RouteEntry(String destinationIP, int seqNum, StateFlags state, int hopCount, String nextHopIP, int lifetime)
            routeTable.put(key, routeEntry);
        }
        //如果收到的信息中是寻找本机,则回复路由响应
        if (ip == (messageRREQ.getDestinationIP())) {
            logger.debug("<{}> received RREQ from <{}> successfully and its system info<{}> ",MessageUtils.showHex(ip), MessageUtils.showHex(messageRREQ.getSrcIP()), messageRREQ.getSystemInfo().toString());
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
        //如果本机有到要寻找的目的节点的路由,则回复路由请求
        if (routeEntry != null) {
            logger.debug("<{}>  has routing to<{}> " ,MessageUtils.showHex(this.ip),
                    MessageUtils.showHex(messageRREQ.getDestinationIP()));
            MessageRREP messageRREP = new MessageRREP(ip, routeEntry.getHopCount(), seqNum++, routeEntry.getSystemInfo());
            messageRREP.setDestinationIP(messageRREQ.getSrcIP());
            messageRREP.setSrcIP(messageRREQ.getDestinationIP());
            messageRREP.setRouteIP(this.ip);
            messageRREP.setNextHopIp(queryRouteTable(messageRREQ.getSrcIP()).getNextHopIP());
            sendRREP(messageRREP);
            return;
        }
        //如果信息中不是在寻找本机,则给跳数加一和更新转发节点ip后转发该请求
        messageRREQ.setHop((byte) (messageRREQ.getHop() + 1));
        messageRREQ.setRouteIP(this.ip);
        //如果本节点的性能等级低于路由请求中的节点性能等级，则将路由请求中的短板信息更新为自身
        if(this.systemInfo.getPerformanceLevel()<messageRREQ.getSystemInfo().getPerformanceLevel()){
            messageRREQ.setSystemInfo(this.systemInfo);
        }
        //转发路由请求
        forwardRREQ(messageRREQ);

    }

    //如果不是发送给自己的RREQ则将其转发出去
    @Override
    public void forwardRREQ(MessageRREQ messageRREQ) {
        logger.debug("<{}> forward RREQ of <{}> sent to<{}> ...", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREQ.getSrcIP()), MessageUtils.showHex(messageRREQ.getDestinationIP()));
        try {
            adhocTransfer.send(messageRREQ);
            logger.debug("<{}> forward RREQ about <{}> sent to <{}> successfully!", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREQ.getSrcIP()), MessageUtils.showHex(messageRREQ.getDestinationIP()));
        } catch (IOException e) {
            logger.error("<{}> forward RREQ about <{}> sent to <{}> failed!", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREQ.getSrcIP()), MessageUtils.showHex(messageRREQ.getDestinationIP()));
        }
    }

    //对请求自己路由回复路由请求
    @Override
    public void sendRREP(MessageRREP messageRREP) {
        logger.debug("<{}> sending RREP to <{}>...", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREP.getDestinationIP()));
        try {
            adhocTransfer.send(messageRREP);
            logger.debug("<{}> sending RREP to <{}> successfully!", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREP.getDestinationIP()));
        } catch (IOException e) {
            logger.warn("<{}> sending RREP to <{}> failed!", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREP.getDestinationIP()));

        }
    }

    //在数据类型方法解析后调用,开始解析数据中内容,判断是否为发送给自己的RREP

    /**
     * 目前处理RREP的方式和RREQ一样,都是广播发送,然后再判断处理。
     * RREP应该按照反向路径回复路由,采用单播的方式发送（已经实现）
     *
     * @param messageRREP
     */
    @Override
    public void receiveRREP(MessageRREP messageRREP) {
        logger.debug("<{}>  received RREP about <{}> sent to <{}>, processing...", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREP.getSrcIP()), MessageUtils.showHex(messageRREP.getDestinationIP()));
        //将转发该消息的节点地址加入到先驱列表中
        int key = messageRREP.getSrcIP();
        if(key==this.getIp())return;
        precursorIPs.add(messageRREP.getRouteIP());
        //如果收到的信息里面,请求的序列号的键存在,并且小于等于本机所存,则抛弃
        if (routeTable.containsKey(key) && routeTable.get(key).getSeqNum() >= messageRREP.getSeqNum()) {
            logger.warn("<{}> drops the old seq <{}>...", MessageUtils.showHex(this.ip),messageRREP.getSeqNum());
            return;
        } else {
            int hop = messageRREP.getHop();
            hop++;
            RouteEntry routeEntry = new RouteEntry(key, messageRREP.getRouteIP()
                    , messageRREP.getSeqNum(), StateFlags.VALID, hop, 0, messageRREP.getSystemInfo(),System.currentTimeMillis());
            logger.debug("add new route entry {}",routeEntry);
            routeTable.put(key, routeEntry);
        }
        //如果收到的信息中是寻找本机,则回复路由响应
        if (ip == messageRREP.getDestinationIP()) {
            logger.debug("<{}> received RREP from <{}> successfully and its system info<{}> ",
                    MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREP.getSrcIP()), messageRREP.getSystemInfo().toString());
            return;
        }
        if(this.ip==messageRREP.getNextHopIp()) {
            logger.debug("<{}> received RREP from <{}> sent to <{}>, forwarding...",MessageUtils.showHex(this.ip),MessageUtils.showHex(messageRREP.getSrcIP()),MessageUtils.showHex(messageRREP.getDestinationIP()));
            RouteEntry routeEntry = queryRouteTable(messageRREP.getDestinationIP());
            //如果信息中不是回复寻找本机,则给跳数加一和更新转发节点ip后转发该请求
            messageRREP.setHop((byte) (messageRREP.getHop() + 1));
            messageRREP.setRouteIP(this.ip);
            messageRREP.setNextHopIp(routeEntry.getNextHopIP());
            //转发
            if(this.systemInfo.getPerformanceLevel()<messageRREP.getSystemInfo().getPerformanceLevel()){
                messageRREP.setSystemInfo(this.systemInfo);
            }
            forwardRREP(messageRREP);
        }
    }

    //如果不是发送给自己的RREP则将其转发出去
    @Override
    public void forwardRREP(MessageRREP messageRREP) {
        logger.debug("<{}> forward RREP of <{}> sent to <{}>...", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREP.getSrcIP()), MessageUtils.showHex(messageRREP.getDestinationIP()));
        try {
            adhocTransfer.send(messageRREP);
            logger.debug("<{}> forward RREQ of <{}> sent to<{}> successfully!", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREP.getSrcIP()), MessageUtils.showHex(messageRREP.getDestinationIP()));
        } catch (IOException e) {
            logger.warn("<{}> forward RREQ of <{}> sent to<{}> failed!", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageRREP.getSrcIP()), MessageUtils.showHex(messageRREP.getDestinationIP()));
        }
    }

    //在接收线程接收到数据后调用,主要解析数据类型,再恢复为相应的Message对象,并传递给相应的接收方法
    @Override
    public void dataParsing(byte[] bytes) {
        byte type = bytes[3];
        //logger.debug("the type of message is {}",type);
        //Message message = null;
        //如果是数据类型则恢复为数据MessageData,并且交给数据类型接收方法
        if (type == RouteProtocol.DATA) {
            MessageData message = MessageData.recoverMsg(bytes);
            //  System.out.println(message);
            receiveDATA(message);
        }
        //如果是路由回复类型则恢复为数据MessageRREP,并且交给数据类型接收方法
        else if (type == RouteProtocol.RREP) {
            MessageRREP message = MessageRREP.recoverMsg(bytes);
            receiveRREP(message);
        }
        //如果是路由请求类型则恢复为数据MessageRREQ,并且交给数据类型接收方法
        else if (type == RouteProtocol.RREQ) {
            MessageRREQ message = MessageRREQ.recoverMsg(bytes);
            receiveRREQ(message);
        } else if (type == RouteProtocol.HELLO) {
            //交给处理hello报文的处理函数
            MessageHello message = MessageHello.recoverMsg(bytes);
            helloHandler(message);
        } else if(type == RouteProtocol.RRER){
            MessageRRER message = MessageRRER.recoverMsg(bytes);
            receiveRRER(message);
        }else if (type == RouteProtocol.TEST){

        }else {
            logger.warn("Invalid data format!");
        }
    }

    @Override
    public void sendMessage(String context, int destinationIP) {
        //节点想要给目的节点发送消息,首先查询本节点中路由表是否有可用的有效路由,如果没有就发起路由请求
        RouteEntry routeEntry = queryRouteTable(destinationIP);
        if (routeEntry == null) {
            logger.debug("<{}> has no routing about <{}>,sent RREQ firstly...", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP));
            sendRREQ(destinationIP);
        }
        routeEntry = queryRouteTable(destinationIP);
        if (routeEntry != null) {
            //如果路由表中有可用路由则可以向其发送数据
            MessageData messageData = new MessageData(destinationIP, context.getBytes());
            messageData.setSrcIP(ip);
            messageData.setNextIP(routeEntry.getNextHopIP());
            try {
                logger.debug("<{}> send message to <{}>,message : <{}>",MessageUtils.showHex(this.ip),MessageUtils.showHex(destinationIP),messageData);
                adhocTransfer.send(messageData);
                logger.debug("<{}> sent datagram to <{}> successfully!", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP));
            } catch (IOException e) {
                logger.warn("<{}> sent datagram to <{}> failed!", MessageUtils.showHex(this.ip), MessageUtils.showHex(destinationIP));
            }
        }else {
            logger.warn("<{}> search for route to <{}> failed, canceling send message. ",MessageUtils.showHex(ip),MessageUtils.showHex(destinationIP));
        }
    }

    @Override
    public void receiveDATA(MessageData messageData) {
        logger.debug("<{}> received message data from <{}> to <{}>,Message meta:<{}>",MessageUtils.showHex(this.ip),MessageUtils.showHex(messageData.getSrcIP()),MessageUtils.showHex(messageData.getDestinationIP()),messageData);
        //收到数据类型的信息时不需要检查序列号
        int nextIP = messageData.getNextIP();

        int destinationIP = messageData.getDestinationIP();
        if (destinationIP == ip) {
            logger.debug("<{}> received datagram from <{}>, content:<{}> ", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageData.getSrcIP()), new String(messageData.getContent()));
            return;
        }

        if (nextIP != ip) {
            logger.debug("<{}> received data from <{}> want sent to <{}> but next ip is <{}>,drops the datagram!", MessageUtils.showHex(this.ip),MessageUtils.showHex(messageData.getSrcIP()), MessageUtils.showHex(messageData.getDestinationIP()),MessageUtils.showHex(nextIP));
            return;
        }

        logger.debug("<{}> received data from <{}> want sent to <{}>,and it is interchange node,processing the datagram!!", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageData.getSrcIP()), MessageUtils.showHex(messageData.getDestinationIP()));

        //查询本节点的路由表,得到去往目的节点的下一跳节点,并改变消息中的下一跳节点地址后转发出去
        int next = queryRouteTable(destinationIP).getNextHopIP();
        messageData.setNextIP(next);

        /**
         * 还要完成的一件事就是在转之前检查SystemInfo,如果本机的SystemInfo评分低于message中的,需要将其重置
         */
        forwardDATA(messageData);
    }

    /**
     * 这个几个forwardXXXX方法的日志记录完全可以用AOP实现,采用动态代理即可,但是劳资现在不想干!!!!
     *
     * @param messageData
     */
    @Override
    public void forwardDATA(MessageData messageData) {
        logger.debug("<{}> forward a datagram that <{}> sent to <{}>...", MessageUtils.showHex(this.ip), MessageUtils.showHex(messageData.getSrcIP()), MessageUtils.showHex(messageData.getDestinationIP()));
        try {
            adhocTransfer.send(messageData);
            logger.debug("<{}> forward datagram successfully!", MessageUtils.showHex(this.ip));
        } catch (IOException e) {
            logger.warn("<{}> forward datagram failed!", MessageUtils.showHex(this.ip));
        }
    }

    @Override
    public void sendRRER(MessageRRER messageRRER) {
        logger.debug("<{}> prepare to send RRER, cant link to <{}>.",this.getIp(),messageRRER.getDestinationIP());
        try {
            adhocTransfer.send(messageRRER);
            logger.debug("<{}> send RRER successfully!", MessageUtils.showHex(this.ip));
        }catch (IOException e){
            logger.warn("<{}> send RRER failed!", MessageUtils.showHex(this.ip));
        }
    }

    @Override
    public void receiveRRER(MessageRRER messageRRER) {
        logger.debug("<{}> receive RRER from <{}>",this.ip,messageRRER.getSrcIP());
        RouteEntry routeEntry = routeTable.get(messageRRER.getDestinationIP());
        routeEntry.setState(StateFlags.INVALID);
        MessageRRER message = new MessageRRER(this.getIp(),messageRRER.getDestinationIP());
        sendRRER(message);
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
        logger.debug("<{}> receive hello from <{}>",MessageUtils.showHex(this.ip),MessageUtils.showHex(srcIP));
        lock.lock();
        try {
            int size = helloMessagesQueue.size();
            helloMessagesQueue.add(srcIP);
        }finally {
            lock.unlock();
        }

    }


    //路由表维护函数,根据helloIP队列中的IP来维护路由表,在一定时间内没有收到某一节节点的hello报文,则将以该节点为下一中转节点的路由表
    //可用状态设置为不可用,并发送RRER
    private void maintainRouteTable() {

        //路由表维护线程,匿名内部类
        Thread maintainRouteThread = new Thread(new Runnable() {
            //路由维护线程
            @Override
            public void run() {
                int ip1 = 0;
                //根据目标地址获取
                Set<Integer>  DestinationSet;
                Iterator<Integer>  it;
                boolean locked = false;
                while (true) {
                    try {
                        locked = lock.tryLock(1, TimeUnit.SECONDS);
                        if(locked) {
                            if (helloMessagesQueue.size() > 0) {
                                ip1 = helloMessagesQueue.remove();
                            }
                        }
                    }catch (InterruptedException e){

                    }finally {
                        if(locked)
                            lock.unlock();
                    }
                    if (ip1 != 0) {
                        DestinationSet = getDestinationIPByNextIP(ip1);
                        it = DestinationSet.iterator();
                        while (it.hasNext()) {
                            routeTable.get(it.next()).setLastModifyTime(System.currentTimeMillis());
                        }
                    }
                    for(Integer integer : routeTable.keySet()){
                        RouteEntry routeEntry = routeTable.get(integer);
                        if((System.currentTimeMillis()-routeEntry.getLastModifyTime())>routeEntry.getLifeTime()){
                            routeEntry.setState(StateFlags.INVALID);
                            sendRRER(new MessageRRER(getIp(),routeEntry.getDestIP()));
                        }
                    }
                }
            }
        });

        //将维护线程设置为守护线程
       // maintainRouteThread.setDaemon(true);

        maintainRouteThread.start();

    }

    //根据下一跳节点的ip获取目的节点的ip集合,根据该set查找route有效可用的表项
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
