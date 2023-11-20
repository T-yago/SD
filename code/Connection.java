import java.io.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class Connection {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.outputStream = new DataOutputStream (socket.getOutputStream());
        this.inputStream = new DataInputStream((socket.getInputStream()));
        
    }

    public void send(Message message) throws IOException {
        rwLock.writeLock().lock();
        try {
            this.outputStream.writeByte(message.getType());
            message.getPayload().serialize(this.outputStream);
            this.outputStream.flush();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // recebe uma mensagem em binário e deserializa-a, para um objecto mensagem em que o campo payload depende do tipo de mensagem
    public Message receive() throws IOException{
        rwLock.readLock().lock();
        byte type;
        Payload payload = null;

        try {
            type = this.inputStream.readByte();

            if (type == 0 | type == 1) {
                Account acc = new Account();
                payload = (Account) acc.deserialize(this.inputStream);
            } else if (type == 127) {
                BytePayload bytePayload = new BytePayload();
                payload = (BytePayload) bytePayload.deserialize(this.inputStream);
            }

           
            
        } finally {
            rwLock.readLock().unlock();
        }

        return new Message(type, payload);
    }

    public void close () {
        try {
            this.socket.close();
        } catch (IOException e) {
            System.out.println("Error closing socket.");
        }
    }
}
