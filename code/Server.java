import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sd23.*;



public class Server {


    private static Accounts accounts = new Accounts();
    private static int max_MEM;
    private static CustomBlockingQueue programQueue = new CustomBlockingQueue(20,max_MEM);            
    private static Lock memoryLock = new ReentrantLock();
    private static SimpleAtomicInteger currentMemory;
    private static Condition memoryAvailable = memoryLock.newCondition();


    static class JobMemoryException extends Exception {
        public JobMemoryException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Server <max_MEM>");
            System.exit(1);
        }
        max_MEM = Integer.parseInt(args[0]);
        currentMemory = new SimpleAtomicInteger(max_MEM);
        int numThreads = Integer.parseInt(args[1]);


        try {
            final ServerSocket serverSocket = new ServerSocket(22347);

            // Thread que espera por um sinal de interrupção e fecha o servidor, caso este apareça
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down gracefully...");
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                        System.out.println("Server socket closed.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            for (int i = 0; i < numThreads; i++) {
                Thread clientThread = new Thread(() -> {
                    while (true) {
                        Socket clientSocket;
                        try {
                            clientSocket = serverSocket.accept();
                            Connection connection = new Connection(clientSocket);
                            handleClient(connection, currentMemory);
                        } catch (IOException | JobMemoryException e) {
                            System.out.println("Error handling client");;
                        }
                    }
                });
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Error creating server socket.");
            System.exit(1);
        }
    }

    private static void handleClient(Connection conn, SimpleAtomicInteger currentMemory) throws JobMemoryException{
        try (conn) {
            while (true) {
                System.out.println("COMEçCoU!");
                Message message = conn.receive();
                byte type = message.getType();

                System.out.println("Received message type: " + type);
        
                    if (type == 0) { // Registo
                        Account account = (Account) message.getPayload();
                        try {
                            accounts.addAccount(account.getUsername(), account.getPassword());
                            System.out.println("Account created successfully.");
                            accounts.printAccounts();
                            conn.send(new Message((byte)127,new BytePayload((byte)0)));
                            System.out.println("Sent confirmation.");


                        } catch (Exception e) {
                            conn.send(new Message((byte)127,new BytePayload((byte)-1)));
                            System.out.println("Error creating account.");
                        }

                    } else if (type == 1) { // Login / Logout, dependendo do estado da conta
                        Account account = (Account) message.getPayload();
                        if (!account.isLoggedIn()) {
                            try{
                                accounts.logInUser(account.getUsername(), account.getPassword());
                                System.out.println("Account login successfull.");
                                conn.send(new Message((byte)127,new BytePayload((byte)0)));

                            } catch (Accounts.AccountAlreadyLogged e) { // caso em que a conta já está logada
                                conn.send(new Message((byte)127,new BytePayload((byte)-1)));

                            } catch (Accounts.IncorrectLoginDataException e) { // caso em que a password ou user estão errados  
                                conn.send(new Message((byte)127,new BytePayload((byte)-2)));

                            } catch (Exception e) { // erro desconhecido
                                conn.send(new Message((byte)127,new BytePayload((byte)-3)));
                            }
                        }
                        else {
                            try{
                                accounts.logOutUser(account.getUsername());
                                System.out.println("Account logout successfull.");
                                conn.send(new Message((byte)127,new BytePayload((byte)0)));
                            } catch (Exception e) {
                                conn.send(new Message((byte)127,new BytePayload((byte)-1)));
                                e.printStackTrace();
                            }
                        }
                        
                    } else if (type == 2) { // Executar código
                        int mem = 0;
                        boolean exception = false;
                        try {
                            BytesPayload bytesPayload = (BytesPayload) message.getPayload();
                            mem = bytesPayload.readFirstInt();
                            
                            if (mem > max_MEM) {
                                exception = true;
                                throw new JobMemoryException("Job requires more memory than the maximum allowed.");
                            }

                            int id = bytesPayload.readSecondInt();
        
                            System.out.println("\n\n Current MEM" + currentMemory.get() + "\n\n");
        
                            memoryLock.lock();
                            try {
                                if (mem > currentMemory.get()) {
                                    while (mem > currentMemory.get()) {
                                        try {
                                            System.out.println("Ran out of memory");
                                            System.out.println(programQueue.toString());
                                            programQueue.enqueue("Job" + id);
                                            memoryAvailable.await(); // espera até que haja memória disponível
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
        
                                if (bytesPayload == null) {
                                    System.out.println("Error: Received message with null payload.");
                                }
        
                                byte[] job = bytesPayload.getData();
                                currentMemory.addAndGet(-mem);
        
                                memoryLock.unlock();
        
                                try {
                                    byte[] output = JobFunction.execute(job);

                                    byte[] outputWithId = new byte[output.length + 4];
                                    outputWithId[0] = (byte) id;
                                    outputWithId[1] = (byte) (id >> 8);
                                    outputWithId[2] = (byte) (id >> 16);
                                    outputWithId[3] = (byte) (id >> 24);

                                    System.arraycopy(output, 0, outputWithId, 4, output.length);

                                    System.err.println("success, returned " + output.length + " bytes");
                                    conn.send(new Message((byte) 127, new BytePayload((byte) 0)));
                                    conn.send(new Message((byte) 2, new BytesPayload(outputWithId)));
                                } catch (JobFunctionException | IOException e) {
                                    System.out.println("job failed");
                                    e.printStackTrace();
                                    conn.send(new Message((byte) 127, new BytePayload((byte) -1)));
                                }
                            } finally {
                                try {
                                    String program = programQueue.dequeue();
                                    System.out.println("Dequeued program: " + program);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                memoryLock.lock();
                                currentMemory.addAndGet(mem);
                                memoryAvailable.signalAll();
                            }
                        } finally {
                            if (!exception) memoryLock.unlock();
                        }
                    } else if (type == 3) { // Develver memória e jobs em queue
                        System.out.println("Received request for memory and queue.");
                        int mem = currentMemory.get();
                        programQueue.setMem(mem);
                        
                        byte[] memoryBytes = new byte[4];
                        memoryBytes[0] = (byte) mem;
                        memoryBytes[1] = (byte) (mem >> 8);
                        memoryBytes[2] = (byte) (mem >> 16);
                        memoryBytes[3] = (byte) (mem >> 24);

                        System.out.println(programQueue.toString());

                        conn.send(new Message((byte) 4, programQueue));
                        System.out.println("Sent memory and queue.");                        
                    } else {
                        System.out.println("Received invalid message type: " + type);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client.");
                e.printStackTrace();
            }
        }
}

