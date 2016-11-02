package cn.edu.uestc.Adhoc.adhocThread;

import cn.edu.uestc.Adhoc.entity.adhocNode.AdhocNode;
import cn.edu.uestc.Adhoc.entity.message.MessageHello;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 路由维护机制，循环的向先驱列表中的节点发送hello报文
 * 如果某先驱列表中的节点在一定时间内没有收到该节点的hello报文，那么以该节点为下一转发节点的路由将会断链，
 * 因此在该先驱列表中的这一节点的该路由会被删除，同时该先驱节点就会发送RRER的路由错误信息
 *
 */
public class HelloThread implements Runnable{

    private AdhocNode adhocNode;
    private OutputStream os;

    public HelloThread(AdhocNode adhocNode){
        this.adhocNode=adhocNode;
        this.os=adhocNode.getAdhocTransfer().getOs();
    }


    @Override
    public void run() {
        while(true){
            for(Integer integer:adhocNode.getPrecursorIPs()){
                try {
                    os.write(new MessageHello(adhocNode.getIp(),integer).getBytes());
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
