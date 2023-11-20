import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class BytePayload implements Payload {
    private byte data;

    public BytePayload () {
        this.data = -1;
    }

    public BytePayload(byte data) {
        this.data = data;
    }

    public byte getData() {
        return data;
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(data);
    }

    @Override
    public BytePayload deserialize(DataInputStream in) throws IOException {
        data = in.readByte();
        return new BytePayload(data);
    }
}
