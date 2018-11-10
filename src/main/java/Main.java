import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

public class Main {
    private static String serverUrl = "nats://localhost:4222";
    private static Logger logger= LoggerFactory.getLogger(Main.class);
    private static int senderCnt=0;
    private static Listener listener=new Listener();

    private static Thread createSender() {
        Thread sender=new Thread(new Runnable() {
            long cnt=0;
            long prevTime=0;
            @Override
            public void run() {
                logger.info("start reciever");
                prevTime=System.currentTimeMillis();
                while (true) {
                    logger.info("connect");
                    Options.Builder o = new Options.Builder();
                    o.server(serverUrl);
                    o.supportUTF8Subjects();
                    o.errorListener(listener);
                    o.connectionListener(listener);
                    try (Connection sender = Nats.connect(o.build())) {
                        while(true) {
                            sender.publish("me", "this is a message".getBytes());
                            cnt++;
                            if (prevTime+5000<System.currentTimeMillis()) {
                                logger.info("sender  : {} msg/sec {}",cnt/5,sender.getStatus());
                                cnt=0;
                                prevTime=System.currentTimeMillis();
                            }
                        }
                    } catch (Throwable t) {
                        logger.error("oeps",t);
                    }
                    logger.info("done");
                    ;
                }
            }

        });
        sender.setDaemon(true);
        sender.setName("sender-"+senderCnt+"  ");
        senderCnt++;
        return sender;
    }

    public static Thread createReceiver(int i) {
        Thread receiver=new Thread(new Runnable() {
            private long cnt = 0;
            private long prevTime = 0;
            private long prevDrop=0;
            private int id=i;

            public void run() {

                while (true) {
                    logger.info("connect {}",id);
                    prevTime = System.currentTimeMillis();
                    Options.Builder o = new Options.Builder();
                    o.server(serverUrl);
                    o.supportUTF8Subjects();
                    o.reconnectWait(Duration.ofSeconds(5));
                    o.maxReconnects(-1);//infinity
                    o.connectionName("receiver-"+id);
                    o.errorListener(listener);
                    o.connectionListener(listener);
                    try (Connection reciever = Nats.connect(o.build())) {
                        Subscription sub = reciever.subscribe("me","queue");
                        sub.setPendingLimits(5000,-1);
                        while (true) {
                            Message message = sub.nextMessage(Duration.ofSeconds(10));
                            if (message!=null) {
                                cnt++;
                                if (prevTime + 1000 < System.currentTimeMillis()) {
                                    long drop = reciever.getStatistics().getDroppedCount() - prevDrop;
                                    prevDrop = reciever.getStatistics().getDroppedCount();
                                    logger.info("receiver: {} msg/sec {} dropped {} reconnected: {}", cnt, reciever.getStatus(), drop, reciever.getStatistics().getReconnects());
                                    cnt = 0;
                                    prevTime = System.currentTimeMillis();
                                }
                            } else {
                                logger.info("no messages...");
                            }
                        }
                    } catch (Throwable t) {
                        logger.error("oeps",t);
                    };
                    logger.info("done");
                }
            }
        });
        receiver.setName("reciever-"+i);
        receiver.setDaemon(true);
        return receiver;
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("hello");

        Thread sender=createSender();
        Thread reciever1=createReceiver(1);
        //Thread reciever2=createReceiver(2);
        //Thread reciever3=createReceiver(3);
        reciever1.start();
        //reciever2.start();
        //reciever3.start();
        Thread.sleep(100);
        sender.start();
        while (true) {
            Thread.sleep(10000);
        }
    }
}
