import java.util.Queue;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.locks.Condition;

public class PayloadsDequeue {
    private Queue<Payload> queue;
    private Condition condition;

    public PayloadsDequeue(ReentrantLock l) {
        this.queue = new ArrayDeque<Payload>();
        this.condition = l.newCondition();
    }

    public void add(Payload payload) {
        this.queue.add(payload);



        
        try{
            this.condition.signal();
        } catch (Exception e) {
            this.condition.signalAll();
        }
        this.condition.signal();
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public Payload poll() {
        return this.queue.poll();
    }


    public void await() {
        try {
            this.condition.await();
        } catch (InterruptedException e) {
            System.out.println("Error receiving message.");
        }
    }

    
}