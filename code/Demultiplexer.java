import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Demultiplexer implements AutoCloseable {
    private final Connection conn;
    private final ReentrantLock l = new ReentrantLock();
    private final Map<Byte, PayloadsDequeue> queues;

    public Demultiplexer(Connection conn) {
        this.conn = conn;
        this.queues = new HashMap<>();
        this.start();
    }

    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    Message message = conn.receive();
                    byte type = message.getType();
                    this.l.lock();
                    try {
                        PayloadsDequeue queue = queues.get(type);
                        if (queue == null) {
                            queue = new PayloadsDequeue(this.l);
                            queues.put(type, queue);
                        }
                        queue.add(message.getPayload());
                    } catch (Exception e) {
                        System.out.println("Error receiving message.");
                        e.printStackTrace();
                    } finally {
                        this.l.unlock();
                    }
                }
            } catch (IOException e) {
                System.out.println("Error receiving message.");
            }
        }).start();
    }

    public void send(Message message) throws IOException {
        conn.send(message);
    }

    public Payload receive(byte type) {
        try {
            this.l.lock();
            PayloadsDequeue queue = queues.get(type);
            if (queue == null) {
                queue = new PayloadsDequeue(this.l);
                queues.put(type, queue);
            }
            while (true) {
                if (!queue.isEmpty()) {
                    return queue.poll();
                }
                queue.await();
            }
        } finally {
            this.l.unlock();
        }
    }

    public void close() {
        conn.close();
    }
}
