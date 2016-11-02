package cn.edu.uestc.Adhoc.entity.adhocNode;

import cn.edu.uestc.Adhoc.entity.message.MessageData;
import cn.edu.uestc.Adhoc.entity.message.MessageRREP;
import cn.edu.uestc.Adhoc.entity.message.MessageRREQ;
import cn.edu.uestc.Adhoc.entity.route.RouteEntry;

public interface IAdhocNode {
    //路由请求，当要给某一个节点发送数据时没有到该节点的路由时广播路由请求
    //路由回应，当找到路由节点时回复寻找节点的节点
    //路由错误，没有找到路由，可能是节点中没有要找的节点
    //数据发送，表示该帧是一串数据，不是任何控制信息，按照路由途径发送即可
    //发送路由请求RRQ
    void sendRREQ(int destIP);

    //接收路由请求RREQ
    void receiveRREQ(MessageRREQ messageRREQ);

    //转发路由请求RREQ
    void forwardRREQ(MessageRREQ messageRREQ);

    //回复路由响应RREP
    void sendRREP(MessageRREP messageRREP);

    //接收路由响应RREP
    void receiveRREP(MessageRREP messageRREP);

    //转发路由响应RREP
    void forwardRREP(MessageRREP messageRREP);

    //信息分发，当收到一个数据帧时判断该数据帧的类型然后在交给对应的处理函数
    void dataParsing(byte[] bytes);

    //发送数据消息
    void sendMessage(String context,int destIP);

    //接收数据消息
    void receiveDATA(MessageData messageData);

    //判断数据包中的源地址是否为自己如果是则接收，如果不是，根据路由表查询去往源地址的下一节点从而转发，转发数据消息
    void forwardDATA(MessageData messageData);

    //查询路由表，若存在目标节点的有效路由则返回
    //RouteEntry queryRouteTable(int destIP);
}
