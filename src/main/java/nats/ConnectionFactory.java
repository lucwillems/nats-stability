package nats;

import io.nats.client.MessageHandler;
import io.nats.client.Options;

public interface ConnectionFactory {
    NatsConnectionFacade connect(Options.Builder builder, MessageHandler handler);
    NatsConnectionFacade connect(Options.Builder builder);
    void close(String name);

    public NatsConnectionRegister getRegister();
}
