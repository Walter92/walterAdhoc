package cn.edu.uestc.Adhoc.utils;

/**
 * Created by walter on 15-12-14.
 */
public class MessageUtils {

    //注意，这两个方法只有在比如每8个字节首位都为0时才可用！！！！
    //上面的注意已经解决2015.12.23
    public static byte[] IntToBytes(int number) {
        byte[] b = new byte[2];
        b[0] = (byte) number;
        b[1] = (byte) ((0x00ff00 & number) >> 8);
        return b;
    }

    public static int BytesToInt(byte[] bytes) {
        int temp = 0x0000ff & bytes[1];
        int temp2 = 0x0000ff & bytes[0];
        temp <<= 8;
        int number = temp | temp2;
        return number;
    }
}
