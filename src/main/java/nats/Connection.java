package nats;

import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class Connection implements NatsConnectionFacade {
    private final Logger logger = LoggerFactory.getLogger(Connection.class);
    private final long connectTime;
    private final String name;
    private final MessageHandler handler;
    private io.nats.client.Connection nc;
    private Dispatcher dispatcher;
    private Map<String,Subscription> subscriptions;

    public Connection(String name, long connectTimeout,MessageHandler handler) {
        this.connectTime = connectTimeout;
        this.name=name;
        this.handler=handler;
        this.subscriptions=new ConcurrentHashMap<>();
    }

    public String getName() {
        return name;
    }

    private io.nats.client.Connection getConnection() {
        if (nc != null) {
            return nc;
        } else {
            synchronized (this) {
                try {
                    //logger.trace("{} wait nc",name,connectTime);
                    this.wait(connectTime); //wait until connection factory has set the nc value.
                    if (nc==null) {
                        throw new RuntimeException("invalid nc state after wait!!!");
                    }
                    //logger.trace("{} nc exist",name);
                    return nc;
                } catch (InterruptedException e) {
                    logger.error("wait connection:", e);
                    throw new RuntimeException("wait eror",e);
                }
            }
        }
    }

    private synchronized Dispatcher getDispatcher() {
            if (this.dispatcher==null) {
                io.nats.client.Connection conn=getConnection();
                logger.debug("{} create msg dispatcher",name);
                this.dispatcher=conn.createDispatcher(handler);
            }
            return this.dispatcher;
    }

    private void resubscribe() {
        Dispatcher dispatcher=this.getDispatcher();
        logger.info("resubscribe {} subscriptions",this.subscriptions.size());
        for (Subscription s:subscriptions.values()) {
            if (s.getQueue()!=null) {
                dispatcher.subscribe(s.getSubject(),s.getQueue());
            } else {
                dispatcher.subscribe(s.getSubject());
            }
        }
    }

    public void prepareReConnect() throws InterruptedException {
        synchronized (this) {
            if (nc != null) {
                if (dispatcher != null) {
                    nc.closeDispatcher(dispatcher);
                    dispatcher = null;
                }
                nc.close();
                nc = null;
            }
        }
    }

    public void setNc(io.nats.client.Connection conn) {
            synchronized (this) {
                if (nc != null) {
                    return;
                }
                //logger.trace("{} set nc: {}",name,conn.getStatus());
                this.nc = conn;
                this.notifyAll();
                if (subscriptions.size()>0) {
                    this.resubscribe();
                }
            }
    }

    @Override
    public void publish(final String subject, final byte[] body) {
        io.nats.client.Connection nc = getConnection();
        nc.publish(subject, body);
    }

    @Override
    public void publish(final String subject, final String replyTo, final byte[] body) {
        io.nats.client.Connection nc = getConnection();
        nc.publish(subject, replyTo, body);
    }

    @Override
    public CompletableFuture<Message> request(final String subject, final byte[] data) {
        io.nats.client.Connection nc = getConnection();
        return nc.request(subject, data);
    }

    @Override
    public Message request(String subject, byte[] data, Duration timeout) throws InterruptedException {
        io.nats.client.Connection nc = getConnection();
        return nc.request(subject, data, timeout);
    }

    @Override
    public Consumer subscribe(final String subject) {
        Dispatcher dispatcher = getDispatcher();
        logger.debug("{} subscribe {}",name,subject);
        Consumer result = dispatcher.subscribe(subject);
        subscriptions.put(subject,new Subscription(subject,null));
        return result;
    }

    @Override
    public Consumer subscribe(String subject, String queueName) {
        Dispatcher dispatcher = getDispatcher();
        logger.debug("{} subscribe {} {}",name,subject,queueName);
        Consumer result = dispatcher.subscribe(subject,queueName);
        subscriptions.put(subject,new Subscription(subject,queueName));
        return result;
    }

    @Override
    public void flush(Duration timeout) throws TimeoutException, InterruptedException {
        io.nats.client.Connection nc = getConnection();
        nc.flush(timeout);
    }

    @Override
    public CompletableFuture<Boolean> drain(Duration timeout) throws TimeoutException, InterruptedException {
        io.nats.client.Connection nc = getConnection();
        return nc.drain(timeout);
    }

    @Override
    public io.nats.client.Connection.Status getStatus() {
        io.nats.client.Connection nc = getConnection();
        return nc.getStatus();
    }

    @Override
    public long getMaxPayload() {
        io.nats.client.Connection nc = getConnection();
        return nc.getMaxPayload();
    }

    @Override
    public Collection<String> getServers() {
        io.nats.client.Connection nc = getConnection();
        return nc.getServers();
    }

    @Override
    public Statistics getStatistics() {
        io.nats.client.Connection nc = getConnection();
        return nc.getStatistics();
    }

    @Override
    public Options getOptions() {
        io.nats.client.Connection nc = getConnection();
        return nc.getOptions();
    }

    @Override
    public String getConnectedUrl() {
        io.nats.client.Connection nc = getConnection();
        return nc.getConnectedUrl();
    }

    @Override
    public String getLastError() {
        io.nats.client.Connection nc = getConnection();
        return nc.getLastError();
    }

    public void close() throws InterruptedException {
        synchronized (this) {
            this.nc.closeDispatcher(this.dispatcher);
            this.nc.close();
            this.subscriptions.clear();
            this.dispatcher=null;
            this.nc=null;
        }
    }

    public Collection<Subscription>  getSubscriptions() {
        return subscriptions.values();
    }

    protected boolean isReady() {
        return nc!=null;
    }
    protected boolean isConnected() {
        return nc!=null && nc.getStatus().equals(io.nats.client.Connection.Status.CONNECTED);
    }
    protected boolean isClosed() {
        return nc!=null && nc.getStatus().equals(io.nats.client.Connection.Status.CLOSED);
    }

    public MessageHandler getMessageHandler() {
        return this.handler;
    }

    public io.nats.client.Connection getNC() {
        return nc;
    }
}
