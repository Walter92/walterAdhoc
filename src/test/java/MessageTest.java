import cn.edu.uestc.Adhoc.entity.message.MessageData;
import cn.edu.uestc.Adhoc.entity.message.MessageRREQ;

import java.util.Arrays;

/**
 * Created by walter on 16-1-21.
 */
public class MessageTest {
    public static void main(String[] args){
//        MessageData messageData = new MessageData(2,"hello".getBytes());
//        messageData.setNextIP(43);
//        System.out.println(messageData);
//        byte[] bytes = messageData.getBytes();
//        System.out.println(Arrays.toString(bytes));
//        MessageData messageData1 = MessageData.recoverMsg(bytes);
//        System.out.println(messageData1);


        byte[] bytesRREQ = {-2, -5, 34, 1, -23, 0, -23, 0, -45, 0, 5, 0, 1, -8, 19, 5, 76, 105, 110, 117, 120, 95, 97, 114, 109, 0, 0, 0, 0, 0, 0, 0, -2, -6};
        MessageRREQ messageRREQ = MessageRREQ.recoverMsg(bytesRREQ);
        System.out.println(messageRREQ);
    }
}
