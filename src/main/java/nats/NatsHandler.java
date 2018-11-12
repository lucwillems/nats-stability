package nats;

import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatsHandler implements MessageHandler {
    Logger logger=LoggerFactory.getLogger(NatsHandler.class);
    long prevMsg;
    long cnt;

    @Override
    public void onMessage(Message msg) throws InterruptedException {
        cnt++;
        if (prevMsg+1000 < System.currentTimeMillis()) {
            logger.info("{} msg/sec",cnt);
            cnt=0;
            prevMsg=System.currentTimeMillis();
        }
    }
}
