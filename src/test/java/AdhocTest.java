import cn.edu.uestc.Adhoc.entity.adhocNode.AdhocNode;
import cn.edu.uestc.Adhoc.entity.factory.AdhocNodeFactory;
import cn.edu.uestc.Adhoc.entity.message.MessageData;
import cn.edu.uestc.Adhoc.entity.message.MessageRREP;
import cn.edu.uestc.Adhoc.entity.message.MessageRREQ;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;

import java.io.IOException;
public class AdhocTest {

    public static void main(String[] args) throws IOException {
        AdhocNode adhocNode = AdhocNodeFactory.getInstance("usb1");
        try{
            Thread.sleep(5000);
            adhocNode.setIp(1);
            adhocNode.sendMessage("hello",2);
            //adhocNode.getAdhocTransfer().send(new MessageData(1,"hello".getBytes()));
        }catch (Exception e){

        }
//        ArrayList list=new ArrayList();
//        java.util.Collections.shuffle();


//       final MessageRREP messageRREP = new MessageRREP(2,2,2,new SystemInfo(2,41911));
//        messageRREP.setSrcIP(2);
//        messageRREP.setDestinationIP(1);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try{
//                    Thread.sleep(2000);
//                    adhocNode.getSerial().setMessage(messageRREP.getBytes());
//                }catch (Exception e){
//
//                }
//            }
//        }).start();
//
//        adhocNode.sendMessage("hello",2);
//
////        System.out.println(adhocNode);
//        MessageRREQ messageRREQ = new MessageRREQ(3,4,5,new SystemInfo(3,32321));
//        messageRREQ.setDestinationIP(6);
//        messageRREQ.setSrcIP(7);
//        int conut=0;
//
//        MessageData messageData = new MessageData(4,5,"hello".getBytes());
//        messageData.setSrcIP(3);
//        messageData.setDestinationIP(2);
//        while(true) {
//            try {
////                conut++;
//                Thread.sleep(1000);
//                adhocNode.getSerial().setMessage(messageData.getBytes());
//            } catch (Exception e) {
//            }
////            if(conut==3)
////                messageRREQ.setSeqNum(10);
////            adhocNode.getSerial().setMessage(messageRREQ.getBytes());
////            if(conut==6){
////                Map<Integer,RouteEntry> table=adhocNode.getRouteTable();
////                for(Integer i:table.keySet()){
////                    System.out.println(i+"::"+table.get(i));
////                }
////                System.exit(1);
////            }
////            if(conut==2){
////                Map<Integer,RouteEntry> table=adhocNode.getRouteTable();
////                for(Integer i:table.keySet()){
////                    System.out.println(i+"::"+table.get(i));
////                }
//            }
//            adhocNode.getSerial().setMessage(messageRREP.getBytes());

//        }
//        adhocNode.setIp(1);
//        adhocNode.writeThread.start();
//        int numberbefore=0xabcd;
//        adhocNode.sendRREQ(2);
//        byte[] b=AdhocUtils.IntToBytes(numberbefore);
//        int numberafter=AdhocUtils.BytesToint(b);
//        System.out.println(numberafter==numberbefore);
    }
}


