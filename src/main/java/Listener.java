import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

public class Listener implements ConnectionListener, ErrorListener  {
    Logger logger= LoggerFactory.getLogger(Listener.class);
    long prevTime;

    private String getConnectionName(Connection conn) {
        if (conn!=null) {
            return conn.getOptions().getConnectionName();
        }
        return "NULL";
    }

    @Override
    public void connectionEvent(Connection conn, Events type) {
        logger.info("conn: {}  event={}",getConnectionName(conn),type);
    }

    @Override
    public void errorOccurred(Connection conn, String error) {
        logger.warn("conn: {} error={}",getConnectionName(conn),error);
    }

    @Override
    public void exceptionOccurred(Connection conn, Exception exp) {

        logger.error("conn: {} {}",getConnectionName(conn),exp.getMessage());
        logger.error("exception:",exp);
    }

    @Override
    public void slowConsumerDetected(Connection conn, Consumer consumer) {
        if (System.currentTimeMillis()>prevTime+1000) {
            logger.info("conn: {} slow: consumer={} pending={}", getConnectionName(conn), consumer.getClass().getSimpleName(),consumer.getPendingMessageCount());
            prevTime = System.currentTimeMillis();
        }
    }
}

