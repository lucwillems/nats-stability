package nats;

import io.nats.client.MessageHandler;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatsConnectionFactory implements ConnectionFactory {
    private static ConnectionFactory singleton;

    private final Logger logger = LoggerFactory.getLogger(NatsConnectionFactory.class);
    private final NatsConnectionRegister register;

    private NatsConnectionFactory() {
        this.register = new NatsConnectionRegister();
    }

    @Override
    public synchronized NatsConnectionFacade connect(Options.Builder builder, MessageHandler handler) {
        NatsConnectionMonitor monitor=new NatsConnectionMonitor(this);
        builder.connectionListener(monitor);
        builder.errorListener(monitor);
        Options options = builder.build();
        String name = options.getConnectionName();
        if (register.contains(name)) {
            NatsConnectionFacade connectionFacade = register.get(name);
            if (!connectionFacade.getStatus().equals(io.nats.client.Connection.Status.CLOSED)) {
                return connectionFacade;
            }
            register.remove(name);
        }
        //create new connection
        Connection connection = new Connection(name, options.getConnectionTimeout().getSeconds() * 2000, handler);
        monitor.setFacade(connection);
        register.put(name, connection);
        launchConnection(options);
        return connection;
    }

    @Override
    public NatsConnectionFacade connect(Options.Builder builder) {
        return this.connect(builder, null);
    }

    private void launchConnection(Options options) {
        try {
            logger.info("connect async {}", options.getConnectionName());
            Nats.connectAsynchronously(options, true);
            Thread.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException("can not create new connection", e);
        }
    }

    @Override
    public void close(String name) {
        Connection connectionFacade = (Connection) register.get(name);
        if (name != null) {
            try {
                connectionFacade.close();
            } catch (InterruptedException e) {
                throw new RuntimeException("can not close connection", e);
            }
        }
    }

    @Override
    public NatsConnectionRegister getRegister() {
        return register;
    }

    protected Connection getConnection(String name, io.nats.client.Connection conn) {
        Connection connectionFacade = (Connection) register.get(name);
        if (connectionFacade != null && !connectionFacade.isReady()) {
            connectionFacade.setNc(conn);
        }
        return connectionFacade;
    }

    public NatsConnectionFacade getConnection(String name) {
        return register.get(name);
    }


    protected void reconnect(io.nats.client.Connection conn) {
        final Options options = conn.getOptions();
        final String name = options.getConnectionName();
        final Connection connection = (Connection) register.get(name);

        logger.info("reconnecting {} ...", name);
        if (connection != null) {
            try {
                connection.prepareReConnect();
                //create new nc connection using same options
                launchConnection(options);
                logger.info("reconnect {} launched", name);
            } catch (InterruptedException ex) {
                logger.error("prepare reconnect exception: {}", ex);
            }
        } else {
            logger.error("no connection facade to reconnect");
        }
    }

    public static synchronized ConnectionFactory getInstance() {
        if (singleton == null) {
            singleton = new NatsConnectionFactory();
        }
        return singleton;
    }

}
