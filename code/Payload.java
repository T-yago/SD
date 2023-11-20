import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

public interface Payload extends Serializable {
    void serialize(DataOutputStream out) throws IOException;
    Payload deserialize(DataInputStream in) throws IOException;
}