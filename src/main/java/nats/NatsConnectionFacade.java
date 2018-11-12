package nats;

import io.nats.client.Connection;
import io.nats.client.*;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public interface NatsConnectionFacade {

    /**
     * Send a message to the specified subject. The message body <strong>will
     * not</strong> be copied. The expected usage with string content is something
     * like:
     *
     * <pre>
     * nc = Nats.connect()
     * nc.publish("destination", "message".getBytes("UTF-8"))
     * </pre>
     *
     * where the sender creates a byte array immediately before calling publish.
     *
     * @param subject the subject to send the message to
     * @param body the message body
     */
    public void publish(String subject, byte[] body);

    /**
     * Send a request to the specified subject, providing a replyTo subject. The
     * message body <strong>will not</strong> be copied. The expected usage with
     * string content is something like:
     *
     * <pre>
     * nc = Nats.connect()
     * nc.publish("destination", "reply-to", "message".getBytes("UTF-8"))
     * </pre>
     *
     * where the sender creates a byte array immediately before calling publish.
     *
     * @param subject the subject to send the message to
     * @param replyTo the subject the receiver should send the response to
     * @param body the message body
     */
    public void publish(String subject, String replyTo, byte[] body);

    /**
     * Send a request. The returned future will be completed when the
     * response comes back.
     *
     * @param subject the subject for the service that will handle the request
     * @param data the content of the message
     * @return a Future for the response, which may be cancelled on error or timed out
     */
    public CompletableFuture<Message> request(String subject, byte[] data);

    /**
     * Send a request and returns the reply or null. This version of request is equivalent
     * to calling get on the future returned from {@link #request(String, byte[]) request()} with
     * the timeout and handling the ExecutionException and TimeoutException.
     *
     * @param subject the subject for the service that will handle the request
     * @param data the content of the message
     * @param timeout the time to wait for a response
     * @return the reply message or null if the timeout is reached
     * @throws InterruptedException if one is thrown while waiting, in order to propogate it up
     */
    public Message request(String subject, byte[] data, Duration timeout) throws InterruptedException;

    /**
     * Create a synchronous subscription to the specified subject.
     *
     * <p>Use the {@link io.nats.client.Subscription#nextMessage(Duration) nextMessage}
     * method to read messages for this subscription.
     *
     * @param subject the subject to subscribe to
     * @return an object representing the subscription
     */
    public Consumer subscribe(String subject);

    /**
     * Create a synchronous subscription to the specified subject and queue.
     *
     * <p>Use the {@link Subscription#nextMessage(Duration) nextMessage} method to read
     * messages for this subscription.
     *
     * @param subject the subject to subscribe to
     * @param queueName the queue group to join
     * @return an object representing the subscription
     */
    public Consumer subscribe(String subject, String queueName);

     /**
     * Flush the connection's buffer of outgoing messages, including sending a
     * protocol message to and from the server. Passing null is equivalent to
     * passing 0, which will wait forever.
     *
     * If called while the connection is closed, this method will immediately
     * throw a TimeoutException, regardless of the timeout.
     *
     * If called while the connection is disconnected due to network issues this
     * method will wait for up to the timeout for a reconnect or close.
     *
     * @param timeout The time to wait for the flush to succeed, pass 0 to wait
     *                    forever.
     * @throws TimeoutException if the timeout is exceeded
     * @throws InterruptedException if the underlying thread is interrupted
     */
    public void flush(Duration timeout) throws TimeoutException, InterruptedException;

    /**
     * Drain tells the connection to process in flight messages before closing.
     *
     * Drain initially drains all of the consumers, stopping incoming messages.
     * Next, publishing is halted and a flush call is used to insure all published
     * messages have reached the server.
     * Finally the connection is closed.
     *
     * In order to drain subscribers, an unsub protocol message is sent to the server followed by a flush.
     * These two steps occur before drain returns. The remaining steps occur in a background thread.
     * This method tries to manage the timeout properly, so that if the timeout is 1 second, and the flush
     * takes 100ms, the remaining steps have 900ms in the background thread.
     *
     * The connection will try to let all messages be drained, but when the timeout is reached
     * the connection is closed and any outstanding dispatcher threads are interrupted.
     *
     * A future is used to allow this call to be treated as synchronous or asynchronous as
     * needed by the application. The value of the future will be true if all of the subscriptions
     * were drained in the timeout, and false otherwise. The future is completed after the connection
     * is closed, so any connection handler notifications will happen before the future completes.
     *
     * @param timeout The time to wait for the drain to succeed, pass 0 to wait
     *                    forever. Drain involves moving messages to and from the server
     *                    so a very short timeout is not recommended. If the timeout is reached before
     *                    the drain completes, the connection is simply closed, which can result in message
     *                    loss.
     * @return A future that can be used to check if the drain has completed
     * @throws InterruptedException if the thread is interrupted
     * @throws TimeoutException if the initial flush times out
     */
    public CompletableFuture<Boolean> drain(Duration timeout) throws TimeoutException, InterruptedException;

    /**
     * Returns the connections current status.
     *
     * @return the connection's status
     */
    public Connection.Status getStatus();

    /**
     * MaxPayload returns the size limit that a message payload can have. This is
     * set by the server configuration and delivered to the client upon connect.
     *
     * @return the maximum size of a message payload
     */
    public long getMaxPayload();

    /**
     * Return the list of known server urls, including additional servers discovered
     * after a connection has been established.
     *
     * @return this connection's list of known server URLs
     */
    public Collection<String> getServers();

    /**
     * @return a wrapper for useful statistics about the connection
     */
    public Statistics getStatistics();

    /**
     * @return the read-only options used to create this connection
     */
    public Options getOptions();

    /**
     * @return the url used for the current connection, or null if disconnected
     */
    public String getConnectedUrl();

    /**
     * @return the error text from the last error sent by the server to this client
     */
    public String getLastError();

}
