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
