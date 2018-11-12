package nats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NatsConnectionRegister {
    private final Map<String, NatsConnectionFacade> connections;

    public NatsConnectionRegister() {
        connections=new ConcurrentHashMap<>();
    }

    public NatsConnectionFacade get(String name) {
        return connections.get(name);
    }

    public boolean contains(String name) {
        return connections.containsKey(name);
    }
    public NatsConnectionFacade remove(String name) {
        return connections.remove(name);
    }

    public int size() {
        return connections.size();
    }

    public void put(String name, NatsConnectionFacade connection) {
        this.connections.put(name,connection);
    }
}
