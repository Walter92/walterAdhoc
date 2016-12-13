package cn.edu.uestc.Adhoc.entity.route;

/**
 * Created by walter on 15-12-11.
 */
public interface RouteProtocol {
    public static final int PROTOCOL_LEN = 2;
    byte[] frameHeader = {(byte) 0xFE, (byte) 0xFB};//帧头
    byte[] frameEnd = {(byte) 0xFE, (byte) 0xFA};//帧尾
    public static final int RREQ = 0x01;
    public static final int RREP = 0x10;
    public static final int DATA = 0x20;
    public static final int RRER = 0x40;
    public static final int HELLO = 0x50;
}
