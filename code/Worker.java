import sd23.JobFunction;
import sd23.JobFunctionException;

import java.io.IOException;
import java.net.Socket;

public class Worker {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java Server <max_MEM>");
            System.exit(1);
        }

        int max = Integer.parseInt(args[0]);

        // Liga-se ao servidor principal
        Socket s = new Socket("localhost", 22347);
        Connection c = new Connection(s);

        byte[] mem = new byte[4];
        mem[0] = (byte) max;
        mem[1] = (byte) (max >> 8);
        mem[2] = (byte) (max >> 16);
        mem[3] = (byte) (max >> 24);

        Payload payload = new BytesPayload(mem);
        Message message = new Message((byte) 10, payload);
        c.send(message);

        // Recebe pedidos
        while (true) {
            new Thread (() -> {
                try {
                    Message m = c.receive();
                    byte id = m.getType();
                    BytesPayload bytesPayload = (BytesPayload) m.getPayload();
                    byte[] job = bytesPayload.getData();

                    // Executa o job
                    byte[] output = JobFunction.execute(job);

                    // Envia o resultado
                    Payload payload1 = new BytesPayload(output);
                    Message answer = new Message((byte) id, payload1);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JobFunctionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

}
