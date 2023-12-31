import java.util.concurrent.locks.Condition;

public class JobInfo {
    private Payload payload;
    private int size;
    private final int id;
    private final Connection c;
    private final Condition cond;
    private byte[] answer_job = null;

    public JobInfo(int id, Connection c, Condition cond, int size, Payload payload) {
        this.payload = payload;
        this.size = size;
        this.id = id;
        this.c = c;
        this.cond = cond;
    }

    public void answerJob(byte[] answer) {
        this.answer_job = answer;
        this.cond.signal();
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
}
