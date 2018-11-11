import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

public class Main {
    private static String serverUrl = "nats://localhost:4222";
    private static Logger logger= LoggerFactory.getLogger(Main.class);
    private static final Listener listener=new Listener();
    private static final String subject="me";
    private static final byte[] msgBytes="This is a message".getBytes();

    private static Thread createSender(int i) {
        Thread sender=new Thread(new Runnable() {
            long cnt=0;
            long connectCnt=0;
            long prevTime=0;
            int id=i;

            @Override
            public void run() {
                logger.info("start sender");
                prevTime=System.currentTimeMillis();
                while (true) {
                    connectCnt++;
                    logger.info("connect id={} cnt={}",id,cnt);
                    Options.Builder o = new Options.Builder();
                    o.server(serverUrl);
                    o.supportUTF8Subjects();
                    o.turnOnAdvancedStats();
                    o.connectionName("sender-"+id);
                    o.errorListener(listener);
                    o.connectionListener(listener);
                    try (Connection sender = Nats.connect(o.build())) {
                        while(true) {
                            sender.publish(subject, msgBytes);
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
                    logger.info("done id={}",id);
                    ;
                }
            }

        });
        sender.setDaemon(true);
        sender.setPriority(10); //max prio to overload receiver
        sender.setName("sender-"+i+"  ");
        return sender;
    }

    public static Thread createReceiver(int i) {
        Thread receiver=new Thread(new Runnable() {
            private long cnt = 0;
            long connectCnt=0;
            private long prevTime = 0;
            private long prevDrop=0;
            private int id=i;

            public void run() {
                logger.info("start receiver");
                while (true) {
                    connectCnt++;
                    logger.info("connect id={} {}",id,connectCnt);
                    prevTime = System.currentTimeMillis();
                    Options.Builder o = new Options.Builder();
                    o.server(serverUrl);
                    o.supportUTF8Subjects();
                    o.maxReconnects(-1);//infinity
                    o.connectionName("receiver-"+id);
                    o.turnOnAdvancedStats();
                    o.errorListener(listener);
                    o.connectionListener(listener);
                    try (Connection reciever = Nats.connect(o.build())) {
                        Subscription sub = reciever.subscribe(subject,"queue");
                        //sub.setPendingLimits(5000,-1);
                        while (true) {
                            Message message = sub.nextMessage(Duration.ofSeconds(10));
                            if (message!=null) {
                                cnt++;
                                if (prevTime + 5000 < System.currentTimeMillis()) {
                                    long drop = reciever.getStatistics().getDroppedCount() - prevDrop;
                                    prevDrop = reciever.getStatistics().getDroppedCount();
                                    logger.info("receiver: {} msg/sec {} dropped {} reconnected: {}", cnt/5, reciever.getStatus(), drop, reciever.getStatistics().getReconnects());
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
    /*
      run the application with following JAVA 8 VM settings :
         -Xms1G -Xmx1G -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseG1GC

      for max performance
      beside TimeoutException , we also see , sometimes, OutOfMemory exception during recovering when lowering memory

     */
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("hello");

        createReceiver(1).start();
        Thread.sleep(100);
        createSender(1).start();
        while (true) {
            Thread.sleep(10000);
        }
    }
}
