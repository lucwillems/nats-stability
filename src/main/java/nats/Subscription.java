package nats;

public class Subscription {
    private String subject;
    private String queue;

    public Subscription(String subject,String queue) {
        this.subject=subject;
        this.queue=queue;
    }

    public String getSubject() {
        return subject;
    }

    public String getQueue() {
        return queue;
    }
}
