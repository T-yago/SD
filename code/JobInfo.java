import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JobInfo {
    private Payload payload;
    private int size;
    private final int original_id;
    private final int id;
    private final Connection c;
    private final Condition cond;
    private final Lock lock;
    private byte[] answer_job = null;

    public JobInfo(int id, int original_id, Connection c, Lock lock, Condition cond, int size, Payload payload) {
        this.payload = payload;
        this.size = size;
        this.id = id;
        this.original_id = original_id;
        this.c = c;
        this.cond = cond;
        this.lock = lock;
    }

    public void answerJob(byte[] answer) {
        this.answer_job = answer;
        this.lock.lock();
        this.cond.signal();
        this.lock.unlock();
    }

    public byte[] getAnswer_job() {
        return this.answer_job;
    }

    public Payload getPayload() {
        return this.payload;
    }

    public int getId() {
        return this.id;
    }

    public int getSize() {
        return this.size;
    }
    public byte getOriginalId() {
        return (byte) this.original_id;
    }
}
