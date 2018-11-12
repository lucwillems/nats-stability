import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final String serverUrl = "nats://localhost:4222";
    private static final Logger logger= LoggerFactory.getLogger(Main.class);
    private static final Listener listener=new Listener();
    private static final byte[] msgBytes="this is a message".getBytes();
    private static final String subject="me";

    /*
      run the application with following JAVA 8 VM settings :
         -Xms1G -Xmx1G -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseG1GC

      for max performance
      beside TimeoutException , we also see , sometimes, OutOfMemory exception during recovering when lowering memory

     */
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("hello");
        Options.Builder o = new Options.Builder();
        o.server(serverUrl);
        o.connectionName("sender");
        o.errorListener(listener);
        o.connectionListener(listener);
        try (Connection sender = Nats.connect(o.build())) {
            while(true) {
                try {
                    sender.publish(subject, msgBytes);
                } catch (IllegalStateException ex) {
                    logger.info("exception: {}",ex.getMessage());
                }
            }
        } catch (Throwable t) {
            logger.error("oeps",t);
        }
    }
}
