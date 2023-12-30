import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BytesPayload implements Payload {
    private byte[] data;

    public BytesPayload() {
        this.data = new byte[0];
    }

    public BytesPayload(byte data) {
        this.data = new byte[]{data};
    }

    public BytesPayload(byte[] data) {
    if (data == null) {
        this.data = new byte[0];
    } else {
        this.data = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            this.data[i] = data[i];
        }
      }
    }

    public byte[] getData() {
        return this.data;
    }

    public int readFirstInt() { // memória, no caso de um pedido, oud id, no caso de uma resposta.
        if (data.length < 4) {
            throw new IllegalArgumentException("Data array is too short to read an integer.");
        }
    
        int mem = (data[0] & 0xFF) |
                   ((data[1] & 0xFF) << 8) |
                   ((data[2] & 0xFF) << 16) |
                   ((data[3] & 0xFF) << 24);
    
        return mem;
    }

    public int readSecondInt() { // id, no caso de um pedido.
        if (data.length < 8) {
            throw new IllegalArgumentException("Data array is too short to read the second integer.");
        }
    
        int id = (data[4] & 0xFF) |
                 ((data[5] & 0xFF) << 8) |
                 ((data[6] & 0xFF) << 16) |
                 ((data[7] & 0xFF) << 24);
    
        return id;
    }
    


    @Override
    public void serialize(DataOutputStream out) throws IOException {
        System.out.println("BytesPayload: " + data.length);
        out.writeInt(data.length);
        out.write(data);
    }

    @Override
    public BytesPayload deserialize(DataInputStream in) throws IOException {
        int length = in.readInt();
        data = new byte[length];
        in.readFully(data);
        return new BytesPayload(data);
    }


}
