import cn.edu.uestc.Adhoc.entity.message.MessageData;

import java.util.Arrays;

/**
 * Created by walter on 16-1-21.
 */
public class MessageTest {
    public static void main(String[] args){
        MessageData messageData = new MessageData(2,"hello".getBytes());
        byte[] bytes = messageData.getBytes();
        System.out.println(Arrays.toString(bytes));
        MessageData messageData1 = MessageData.recoverMsg(bytes);
        System.out.println(new String(messageData1.getContent()));
    }
}
