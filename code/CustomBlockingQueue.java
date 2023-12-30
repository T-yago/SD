import java.io.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CustomBlockingQueue implements Payload {
    private int mem;
    private final Queue<String> queue;
    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public CustomBlockingQueue() {
        this.mem = 0;
        this.queue = new LinkedList<>();
        this.capacity = 0;
    }

    public CustomBlockingQueue(int capacity, int memory) {
        this.mem = memory;
        this.queue = new LinkedList<>();
        this.capacity = capacity;
    }

    public void setMem(int mem) {
        this.mem = mem;
    }

    public void enqueue(String str) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.offer(str);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public String dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            String item = queue.poll();
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    private int getQueueSizeInBytes() {
        int size = 0;
        for (String str : queue) {
            size += str.length(); 
        }
        return size;
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        lock.lock();
        try {
            // Write the total size of the message (mem + queue size)
            out.writeInt(4 + getQueueSizeInBytes());

            out.writeInt(this.mem);

            out.writeInt(queue.size());

            for (String str : queue) {
                out.writeUTF(str);
            }
        } finally {
            lock.unlock();
        }
    }


    @Override
    public CustomBlockingQueue deserialize(DataInputStream in) throws IOException {
            // Deserialize the total size of the message
            int totalSize = in.readInt();

            // Deserialize the memory value
            int mem = in.readInt();

            // Deserialize the queue size
            int size = in.readInt();

            // Create a new CustomBlockingQueue
            CustomBlockingQueue deserializedQueue = new CustomBlockingQueue(size, mem);

            // Deserialize each element and add it to the new queue
            for (int i = 0; i < size; i++) {
                String str = in.readUTF();
                deserializedQueue.queue.offer(str);
            }

            // Check if the totalSize matches the expected size
            if (totalSize != (4 + deserializedQueue.getQueueSizeInBytes())) {
                throw new IOException("Incorrect total size during deserialization");
            }

            // Return the deserialized object
            return deserializedQueue;
    }

    

    @Override
    public String toString() {
        lock.lock();
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Server Status \n{");
            stringBuilder.append("mem=").append(mem);
            stringBuilder.append(", queue=[");
            
            boolean first = true;
            for (String str : queue) {
                if (!first) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(str);
                first = false;
            }
            
            stringBuilder.append("]");
            stringBuilder.append(", Queue capacity=").append(capacity);
            stringBuilder.append('}');
            return stringBuilder.toString();
        } finally {
            lock.unlock();
        }
    }

}
