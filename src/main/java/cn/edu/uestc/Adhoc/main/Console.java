package cn.edu.uestc.Adhoc.main;

import cn.edu.uestc.Adhoc.entity.adhocNode.AdhocNode;
import cn.edu.uestc.Adhoc.entity.factory.AdhocNodeFactory;
import cn.edu.uestc.Adhoc.entity.route.RouteEntry;
import org.apache.logging.log4j.core.appender.routing.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.NoSuchElementException;

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
                int IP = Integer.valueOf(ip,16);
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
        int  selected  = 0;
        outter:while (true){
            try {
                logger.info("select:\n1.send RREQ;\n2.query route table;\n3.send text message;\n0.quit");
                selected = Integer.parseInt(br.readLine());
                switch (selected){
                    case 1:sendRREQ();break;
                    case 2:queryRouteTable();break;
                    case 3:sendTextMessage();break;
                    case 0:System.exit(0);
                    default:logger.warn("select from 0,1,2,3!!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue outter;
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

    private void queryRouteTable(){
        Map<Integer,RouteEntry>  map = adhocNode.getRouteTable();
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

}
