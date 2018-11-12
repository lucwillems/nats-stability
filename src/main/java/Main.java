import io.nats.client.MessageHandler;
import io.nats.client.Options;
import nats.ConnectionFactory;
import nats.NatsConnectionFacade;
import nats.NatsConnectionFactory;
import nats.NatsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final String serverUrl = "nats://localhost:4222";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final Listener listener = new Listener();
    private static final byte[] msgBytes = "this is a message".getBytes();
    private static final String subject = "me";

    private static NatsConnectionFacade createSender(ConnectionFactory factory, int i) {
        Options.Builder o = new Options.Builder();
        o.server(serverUrl);
        //o.supportUTF8Subjects();
        o.connectionName("sender-" + i);
        NatsConnectionFacade sender = factory.connect(o);
        return sender;
    }

    public static NatsConnectionFacade createReceiver(ConnectionFactory factory, MessageHandler handler, int i) {

        Options.Builder q = new Options.Builder();
        q.server(serverUrl);
        //q.supportUTF8Subjects();

        q.connectionName("receiver-" + i);
        NatsConnectionFacade receiver = factory.connect(q, handler);
        receiver.subscribe(subject, "queue");
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
        ConnectionFactory factory = NatsConnectionFactory.getInstance();
        MessageHandler handler = new NatsHandler();
        createReceiver(factory, handler, 1);
//        createReceiver(factory, handler, 2);
//        createReceiver(factory,handler,3);
        NatsConnectionFacade sender = createSender(factory, 1);
        while (true) {
            try {
                sender.publish(subject, msgBytes);
            } catch (IllegalStateException ex) {
                logger.error("main push exception: {}",ex.getMessage());
            }
        }
    }
}
