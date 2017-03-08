package cn.edu.uestc.Adhoc.main;

import cn.edu.uestc.Adhoc.entity.adhocNode.AdhocNode;
import cn.edu.uestc.Adhoc.entity.factory.AdhocNodeFactory;
import cn.edu.uestc.Adhoc.entity.route.RouteEntry;
import cn.edu.uestc.Adhoc.entity.route.StateFlags;
import cn.edu.uestc.Adhoc.entity.systemInfo.SystemInfo;
import cn.edu.uestc.Adhoc.utils.MessageUtils;
import org.apache.logging.log4j.core.appender.routing.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Created by walter on 16-12-20.
 */
public class Console {
    private static final Logger logger = LoggerFactory.getLogger(Console.class);
    String portName;
    String ip;
    AdhocNode adhocNode ;
    BufferedReader br;
    public static void main(String[] args) {
        new Console();
    }

    private Console() {
        br = new BufferedReader(new InputStreamReader(System.in));
        while (true){
            try {
                logger.info("please input port name:");
                portName = br.readLine();
                logger.info("please input ip:");
                ip = br.readLine();
                int IP = Integer.valueOf(ip, 16);
                adhocNode = AdhocNodeFactory.getInstance(portName, IP);
                break;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NumberFormatException n) {
                logger.warn("please input a number!!");
            } catch (NoSuchElementException nse) {
                logger.warn("port name error!");
            }
        }

        while (true){
            try {
                System.out.println("select:\n1.send RREQ;\n2.query route table;" +
                        "\n3.send text message;" +"\n4.clean route table;" +
                        "\n5.send big data;\n0.quit");
                int selected = Integer.parseInt(br.readLine());
                switch (selected){
                    case 1:sendRREQ();break;
                    case 2:queryRouteTable();break;
                    case 3:sendTextMessage();break;
                    case 4:cleanRouteTable();break;
                    case 5:sendBigData();break;
                    case 0:System.exit(0);
                    default:logger.warn("select from 0,1,2,3!!");
                }
            } catch (Exception e) {
                e.printStackTrace();

            }

        }

    }

    private void sendRREQ(){
        int destIP = 0 ;
        while (true) {
            try {
                logger.info("please input destination node IP:");
                destIP = Integer.valueOf(br.readLine(),16);
                adhocNode.sendRREQ(destIP);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("please input a number!");
            }
        }
    }

    private void cleanRouteTable(){
        adhocNode.getRouteTable().clear();
    }
    private void queryRouteTable() throws Exception{
        Map<Integer,RouteEntry>  map = adhocNode.getRouteTable();
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        SystemInfo systemInfoA1 = new SystemInfo(1,5093,(byte)2);
//            systemInfoA1.setOsArch("arm");
//            systemInfoA1.setOsName("Linux");
//            RouteEntry routeEntryE3 = new RouteEntry(227,213,3, StateFlags.VALID,3,60000,systemInfoA1,sdf.parse("2017-02-22 17:51:01").getTime());

//            SystemInfo systemInfoA2 = new SystemInfo(1,5096,(byte)2);
//            systemInfoA2.setOsArch("arm");
//            systemInfoA2.setOsName("Linux");
           // RouteEntry routeEntryA2 = new RouteEntry(162,213,1, StateFlags.VALID,3,60000,systemInfoA1,sdf.parse("2017-02-22 17:51:00").getTime());




//        map.put(227,routeEntryE3);
//       // map.put(162,routeEntryA2);
//

        Set<Map.Entry<Integer,RouteEntry>>  entrySet = map.entrySet();
        logger.debug("---------------------------------------------------------------------------------------------------------------------------------------------");
        logger.debug("|destIP|seqNum|state  |hopCount|nextHopIP|lifeTime    |systemInfo\t\t\t\t\t\t\t\t\t\t\t\t  |lastModifyTime");
        for(Map.Entry<Integer,RouteEntry> entryEntry : entrySet){
            logger.debug(entryEntry.getValue().printTable());
        }
        logger.debug("---------------------------------------------------------------------------------------------------------------------------------------------");

    }


    private void sendTextMessage(){
        int destIP = 0 ;
        String message=null;
        while (true) {
            try {
                logger.info("please input destination node IP:");
                destIP = Integer.valueOf(br.readLine(),16);
                logger.info("please input message what you want to send:");
                message = br.readLine();
                adhocNode.sendMessage(message, destIP);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("please input a number!");
            }
        }
    }


    private void  sendBigData(){
        int destIP = 0 ;
        int dataLength=0;
        while (true) {
            try {
                logger.info("please input destination node IP:");
                destIP = Integer.valueOf(br.readLine(),16);
                logger.info("how many data want to send:(Kb)");
                dataLength = Integer.valueOf(br.readLine());
                byte[] bytes = new byte[dataLength*1024];
                adhocNode.sendMessage(new String(bytes), destIP);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("please input a number!");
            }
        }
    }

}
