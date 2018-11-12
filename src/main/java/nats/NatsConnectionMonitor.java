package nats;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatsConnectionMonitor implements ConnectionListener, ErrorListener {
    private Logger logger= LoggerFactory.getLogger(NatsConnectionMonitor.class);
    private final nats.NatsConnectionFactory factory;
    private nats.Connection facade;
    private long prevSlowTime;

    public NatsConnectionMonitor(nats.NatsConnectionFactory factory) {
        this.factory=factory;
    }
    protected void setFacade(nats.Connection facade) {
        this.facade=facade;
    }
    private String getConnectionName(Connection connection) {
        return connection==null ? null : connection.getOptions().getConnectionName();
    }


    @Override
    public void connectionEvent(Connection conn, Events type) {
        String name=getConnectionName(conn);
        logger.info("conn: {}  event={}",name,type);
        if (!facade.isReady()) {
            facade.setNc(conn);
        }
    }

    @Override
    public void errorOccurred(Connection conn, String error) {
        String name=getConnectionName(conn);
        logger.warn("conn: {} error={}",name,error);
    }

    @Override
    public void exceptionOccurred(Connection conn, Exception exp) {
        String name=getConnectionName(conn);
        logger.error("conn: {}",name,exp);
        //if (conn!=null && !(exp instanceof IOException)) {
        //    factory.reconnect(conn);
        //}
    }

    @Override
    public void slowConsumerDetected(Connection conn, Consumer consumer) {
        String name=getConnectionName(conn);
        if (System.currentTimeMillis()>prevSlowTime+1000) {
            logger.info("conn: {} slow: consumer={} pending={}", name, consumer.getClass().getSimpleName(),consumer.getPendingMessageCount());
            prevSlowTime = System.currentTimeMillis();
        }
    }
}
