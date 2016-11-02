package cn.edu.uestc.Adhoc.entity.serial;

import java.util.EventListener;

/**
 * Created by walter on 15-12-18.
 * 定义串口事件监听器，当串口对象中的message被更新则会执行响应的方法
 */
public interface SerialPortListener extends EventListener {
    /**
     *
     * @param serialPortEvent 串口中message被更新的事件
     */
    public void doSerialPortEvent(SerialPortEvent serialPortEvent);
}
