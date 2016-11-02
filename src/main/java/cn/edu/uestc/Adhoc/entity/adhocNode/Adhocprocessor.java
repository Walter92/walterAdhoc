package cn.edu.uestc.Adhoc.entity.adhocNode;

import cn.edu.uestc.Adhoc.entity.message.MessageData;
import cn.edu.uestc.Adhoc.entity.message.MessageRREP;
import cn.edu.uestc.Adhoc.entity.message.MessageRREQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by walter on 16-11-2.
 */
public class Adhocprocessor implements IAdhocNode {
    private static Logger logger = LoggerFactory.getLogger(Adhocprocessor.class);
    @Override
    public void sendRREQ(int destIP) {

    }

    @Override
    public void receiveRREQ(MessageRREQ messageRREQ) {

    }

    @Override
    public void forwardRREQ(MessageRREQ messageRREQ) {

    }

    @Override
    public void sendRREP(MessageRREP messageRREP) {

    }

    @Override
    public void receiveRREP(MessageRREP messageRREP) {

    }

    @Override
    public void forwardRREP(MessageRREP messageRREP) {

    }

    @Override
    public void dataParsing(byte[] bytes) {

    }

    @Override
    public void sendMessage(String context, int destIP) {

    }

    @Override
    public void receiveDATA(MessageData messageData) {

    }

    @Override
    public void forwardDATA(MessageData messageData) {

    }
}
