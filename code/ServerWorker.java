import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sd23.*;






public class ServerWorker {

    private int max_MEM;
    private Lock memoryLock;
    private AtomicInteger currentMemory;
    private Condition memoryAvailable;


    private ServerWorker(int max)
    {
        this.max_MEM = max;
        this.currentMemory = new AtomicInteger(max);
        memoryLock = new ReentrantLock();
        memoryAvailable = memoryLock.newCondition();
    }


    private void register_Worker(Demultiplexer demultiplexer) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(this.max_MEM);
        byte[] b = buffer.array();
        Payload p = new BytesPayload(b);
        Message m = new Message((byte)11,p);

        demultiplexer.send(m);
        // esperar algum tipo de ack com uma determinada mensagem


    }
    private static void handle_logout (Demultiplexer m ) {
        try{
        Message message = new Message((byte)11,null);
        m.send(message);

        Payload reply = m.receive((byte)127);

        BytePayload bytePayload = (BytePayload)reply;
        byte payload = bytePayload.getData();

        if (payload == 0) {
            System.out.println("Servidor avisou o servidor principal com sucesso.");
        } else {
            System.out.println("Erro desconhecido.");
        }
        } catch (IOException e) {
            System.out.println("Error sending message.");
        }
    }

    private void handle_main_server(Connection conn) throws IOException{
    {
        try (conn) {
            while (true) {
                System.out.println("COMEçCoU!");
                Message message = conn.receive();
                byte type = message.getType();

                System.out.println("Received message type: " + type);
        


                    
                        
                if (type == 12)
                { // Executar código
                int mem = 0;
                try {
                    BytesPayload bytesPayload = (BytesPayload) message.getPayload();
                    mem = bytesPayload.readMemFirstInt();

                    System.out.println("\n\n Current MEM" + currentMemory + "\n\n");

                    memoryLock.lock();
                    if (bytesPayload == null) {
                            System.out.println("Error: Received message with null payload.");
                        }
                    else
                    {
                            if (mem > currentMemory.get()) {
                                while (mem > currentMemory.get()) {
                                    try {
                                        System.out.println("Ran out of memory");
                                        memoryAvailable.await(); // Handle InterruptedException
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            byte[] dados = bytesPayload.getData();
                            int id = 1; // TODO falta a parte de dar parse byte[]
                            byte[] job = dados; // fazer doutra forma
                            currentMemory.addAndGet(-mem);

                            // Release the lock before executing the job
                            memoryLock.unlock();
                            //Thread t = new Thread (()->{})
                            //t.start();
                            Thread t = new Thread(()->{
                                try{
                                    byte[] output = JobFunction.execute(dados);
                                    System.err.println("success, returned " + output.length + " bytes");
                                    /* TODO mandar uma mensagem ao servidor que indica que
                                        o job foi executado com sucesso(atraves da tag)
                                        payload: idJob e output

                                     */

                                }
                                catch (Exception e){
                                    System.out.println("job failed");
                                    e.printStackTrace();
                                    try {
                                        conn.send(new Message((byte) 127, new BytePayload((byte) -1)));

                                    }
                                    catch (Exception var)
                                    {
                                        var.printStackTrace();

                                    }
                                }
                                try {
                                    memoryLock.lock();
                                    currentMemory.addAndGet(mem);
                                    memoryAvailable.signalAll();
                                } finally {
                                    memoryLock.unlock();

                                }
                            });
                            t.start();

                        }
                    }catch (Exception erro) {
                    erro.printStackTrace();
                    }
                }
                else {
                System.out.println("Received invalid message type: " + type);
                }

            }
        }
        catch (IOException e) {
            System.out.println("Error handling main Server.");
            e.printStackTrace();
        }
    }




    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Server <max_MEM>");
            System.exit(1);
        }

        int max = Integer.parseInt(args[0]);
        ServerWorker worker = new ServerWorker(max);



        try {
                Socket s = new Socket("localhost", 22347);
                Demultiplexer m = new Demultiplexer(new Connection(s));

                // Thread que espera por um sinal de interrupção e fecha o servidor, caso este apareça
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down gracefully...");
                try {
                    if (s != null && !s.isClosed()) {
                        worker.handle_logout(m);
                        s.close();
                        System.out.println("ServerWorker socket closed.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            }
            catch(Exception e)
            {
                System.out.println("Erro ao establecer conexão com o servidor");
            }
        }
    }
}
