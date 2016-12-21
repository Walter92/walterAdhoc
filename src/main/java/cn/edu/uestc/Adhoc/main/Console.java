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
                logger.info("请输入端口名字:");
                portName = br.readLine();
                logger.info("请输入节点IP:");
                ip = br.readLine();
                int IP = Integer.parseInt(ip);
                adhocNode = AdhocNodeFactory.getInstance(portName, IP);
                break;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NumberFormatException n) {
                logger.warn("请输入一个正整数!!");
            } catch (NoSuchElementException nse) {
                logger.warn("端口名错误!");
            }
        }
        int  selected  = 0;
        outter:while (true){
            try {
                logger.info("功能选择:\n1.发送路由请求;\n2.查询路由表;\n3.发送文本;\n0.退出");
                selected = Integer.parseInt(br.readLine());
                switch (selected){
                    case 1:sendRREQ();break;
                    case 2:queryRouteTable();break;
                    case 3:sendTextMessage();break;
                    case 0:System.exit(0);
                    default:logger.warn("请从{0,1,2,3}中选择!");
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
                logger.info("请输入目标节点IP:");
                destIP = Integer.parseInt(br.readLine());
                adhocNode.sendRREQ(destIP);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("请输入正整数!");
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
                logger.info("请输入目标节点IP:");
                destIP = Integer.parseInt(br.readLine());
                logger.info("请输入要发送的文本:");
                message = br.readLine();
                adhocNode.sendMessage(message, destIP);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("请输入正整数!");
            }
        }
    }

}
