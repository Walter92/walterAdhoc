package cn.edu.uestc.Adhoc.entity.factory;

import cn.edu.uestc.Adhoc.entity.adhocNode.AdhocNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * 自组网节点工厂，传入串口名字创建一个自组网节点
 * 读取配置文件，构建windows和linux下串口的节点
 *
 * @author walter
 */
public class AdhocNodeFactory {
    private static final Logger logger = LoggerFactory.getLogger(AdhocNodeFactory.class);

    //禁止创建工厂对象
    private AdhocNodeFactory() {
    }

    private static Properties props;

    //载入端口映射的配置文件
    static {
        InputStream in = AdhocNodeFactory.class.getClassLoader().getResourceAsStream("portMapping.properties");
        props = new Properties();
        try {
            if (in != null) {
                logger.debug("load config file of port mapping...");
                //载入配置文件
                props.load(in);
            } else {
                logger.warn("not exist config file!");
                throw new NullPointerException();
            }
        } catch (IOException e) {
            logger.warn("load config file exception:");
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

//    //生产一个节点实例
//    public static AdhocNode getInstance(String portName) {
//        //获取配置文件中对应key，去掉key的多余空格和转化为小写
//        portName = props.getProperty(portName.trim().toLowerCase());
//        return new AdhocNode(portName);
////        return null;
//    }

    //生产一个节点实例
    public static AdhocNode getInstance(String portName, int ip) {
        //获取配置文件中对应key，去掉key的多余空格和转化为小写
        portName = props.getProperty(portName.trim().toLowerCase());
        return new AdhocNode(portName, ip);
//        return null;
    }

    public static boolean closeAdhocNode(AdhocNode adhocNode) {
        return true;
    }
}
