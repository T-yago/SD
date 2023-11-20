import java.io.*;
import java.util.concurrent.locks.*;
import java.net.*;


public class Message implements Serializable {
    
    private final byte type;
    private final Payload payload;
    
    public Message(byte type, Payload payload) {
        this.type = type;
        this.payload = payload;
    }

    public byte getType() {
        return type;
    }

    public Payload getPayload() {
        return payload;
    }

    public String toString() {
        return "Type: " + type + "\nPayload: " + payload;
    }

    public boolean equals(Object o) {
        if (o instanceof Message) {
            Message m = (Message) o;
            return type == m.type && payload.equals(m.payload);
        } else {
            return false;
        }
    }


}