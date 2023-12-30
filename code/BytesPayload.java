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

    public int readMemFirstInt() {
        if (data.length < 4) {
            throw new IllegalArgumentException("Data array is too short to read an integer.");
        }
    
        int mem = (data[0] & 0xFF) |
                   ((data[1] & 0xFF) << 8) |
                   ((data[2] & 0xFF) << 16) |
                   ((data[3] & 0xFF) << 24);
    
        return mem;
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
