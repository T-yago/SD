import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class SimpleAtomicInteger {
    private int value;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public SimpleAtomicInteger(int initialValue) {
        this.value = initialValue;
    }

    public int get() {
        lock.lock();
        try {
            return value;
        } finally {
            lock.unlock();
        }
    }

    public void set(int newValue) {
        lock.lock();
        try {
            value = newValue;
            condition.signalAll(); // Notify waiting threads on set
        } finally {
            lock.unlock();
        }
    }

    public int getAndIncrement() {
        lock.lock();
        try {
            int oldValue = value;
            value++;
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    public int incrementAndGet() {
        lock.lock();
        try {
            value++;
            condition.signalAll(); // Notify waiting threads on increment
            return value;
        } finally {
            lock.unlock();
        }
    }

    public int addAndGet(int delta) {
        lock.lock();
        try {
            value += delta;
            condition.signalAll(); // Notify waiting threads on add
            return value;
        } finally {
            lock.unlock();
        }
    }

    public boolean compareAndSet(int expect, int update) {
        lock.lock();
        try {
            if (value == expect) {
                value = update;
                condition.signalAll(); // Notify waiting threads on compareAndSet
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
