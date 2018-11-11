# nats stability
test code to test nats JAVA client implementation stability under high sender/receiver load and see that it can recover from this.

## setup :
- running local nats in docker , version nats:1.3.0-linux
- running java test application which launches 1 sender thread and 1 receiver thread
- small message "this is a message" 

## branches

- main : basic example application of the problem with no workaround.
- workaround-xxxx : workaround implementations for testing

## Stability definition
in general , receiving messager speed is lower than sender speed, so when sender can reach
1M msg/sec on my i7 system , receiver can not handle this amount of messages.

the receiving application can be a server application which has a 24x7 uptime requirment.
so from the point of view of this application , it's crucial that the connection to the nats server is always up,
and in case of errors or network issues , the connection should be reestablished as soon as possible.

Nats server includes protection agains high volume float of messages which can not be handled by
the clients. in that case a consuming connection is marked as "slow consumer".

https://nats.io/documentation/server/gnatsd-slow-consumers/

and could be disconnected after some time by the nats server.

The reciever application, should than reestablish the connection and try to recover from this error.
In case of long period of overload, the client application should try to recover as mutch as possible.
messages can and will be dropped in case there is not enough "consuming" speed and this is to be expected.

in that case , more consumers and queues must be used to distribute the load over multible servers.
auto scaling features can be used to handle the launch of extra consumers to handle the peek load.

the problem :
-------------
in case we have a "fixed" environment where the incoming rate > consuming rate for a long period of time.
no new consumers can be launched, so we can expected "dropped" messages and thats ok because we aming to stability and to
process more messages, we need to launch more consumers.

during some testing with this application, we overload the receiving thread , and we see it getting disconnected by the nats server and reconnect to reestablishe the consuming service and collect messages again.

under long duration of high load , the receiver thread gets into a "dead" state where no messages are consumed anymore.
this is caused by a "Connection Timeout" during the reconnect.

```sh
12:14:47.903 [pool-1-thread-1] ERROR Listener - conn: receiver-1
java.io.IOException: Read channel closed.
	at io.nats.client.impl.NatsConnectionReader.run(NatsConnectionReader.java:138)
	at java.lang.Thread.run(Thread.java:748)
12:14:47.903 [pool-1-thread-1] INFO Listener - conn: receiver-1  event=nats: connection disconnected
12:14:48.674 [sender-0  ] INFO Main - sender  : 587897 msg/sec CONNECTED
12:14:50.039 [pool-1-thread-1] ERROR Listener - conn: receiver-1
java.util.concurrent.TimeoutException: null
	at java.util.concurrent.CompletableFuture.timedGet(CompletableFuture.java:1771)
	at java.util.concurrent.CompletableFuture.get(CompletableFuture.java:1915)
	at io.nats.client.impl.NatsConnection.tryToConnect(NatsConnection.java:319)
	at io.nats.client.impl.NatsConnection.reconnect(NatsConnection.java:225)
	at io.nats.client.impl.NatsConnection.closeSocket(NatsConnection.java:471)
	at io.nats.client.impl.NatsConnection.lambda$handleCommunicationIssue$2(NatsConnection.java:428)
	at java.lang.Thread.run(Thread.java:748)
12:14:50.039 [pool-1-thread-1] INFO Listener - conn: receiver-1  event=nats: connection disconnected

```
once the timeout occures,  the receiving connection is not able to reconnect anymore and becomes in a "stalled" reconnecting state.

```java
       // Wait for the INFO message manually
            // all other traffic will use the reader and writer
            readInitialInfo();
            checkVersionRequirements();
            upgradeToSecureIfNeeded();

            // Start the reader and writer after we secured the connection, if necessary
            this.reader.start(this.dataPortFuture);
            this.writer.start(this.dataPortFuture);

            this.sendConnect(serverURI);
            Future<Boolean> pongFuture = sendPing();

            if (pongFuture != null) {
                pongFuture.get(connectTimeout.toNanos(), TimeUnit.NANOSECONDS);   <=== timeout location
            }
```

# Current workaround

to workaround this stalled "reconnect" state, we can close the connection when receiving a TimeoutException.
this will cancel ALL subscriptions and make the nats Connection invalid.

this how ever has some disadvantiges :
- the Server application need to keep recreate a new nats Connection to connect to the server
- ALL subscriptions need to be recreated again.

this is normaly handled by the reconnect feature but as this is failing we have todo this outside Connection class.
so duplicate reconnect handling outside the Connection class. Any reference to the old Connection object will become invalid
and need to be replaced

    @Override
    public void exceptionOccurred(Connection conn, Exception exp) {
        logger.error("conn: {} {}",getConnectionName(conn),exp.getMessage());
        logger.error("exception:",exp);
        if (conn!=null) {
            if (!(exp instanceof IOException)) {
                logger.error("FATAL: recover connection {}",getConnectionName(conn));
                logger.info(conn.getStatistics().toString());//show some stats
                try {
                    conn.close();
                } catch (InterruptedException e) {
                    logger.error("FATAL: recovery fault");
                    logger.error("exception",e);
                }
                logger.error("FATAL: recover connection {} done");
            }
        }
    }

