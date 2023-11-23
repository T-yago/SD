import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import sd23.*;



public class Server {
                
    private static Accounts accounts = new Accounts();

    public static void main(String[] args) {
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

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Connection connection = new Connection(clientSocket);
                Thread clientThread = new Thread(() -> handleClient(connection));
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Error creating server socket.");
            System.exit(1);
        }
    }

    private static void handleClient(Connection conn) {
        try {
            while (true) {

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
                        try {
                        BytesPayload bytesPayload = (BytesPayload)message.getPayload();
                        if (bytesPayload == null) {
                            System.out.println("Error: Received message with null payload.");
                            continue;  // or handle accordingly
                        }                        
                        byte[] job = bytesPayload.getData();
                            byte[] output = JobFunction.execute(job);
                            System.err.println("success, returned "+output.length+" bytes");
                            conn.send(new Message((byte)127,new BytePayload((byte)0)));
                            conn.send(new Message((byte)2,new BytesPayload(output)));
                        } catch (JobFunctionException | IOException e) {
                            System.out.println("job failed");
                            e.printStackTrace();
                            conn.send(new Message((byte)127,new BytePayload((byte)-1)));
                        }
                    }

                    else {
                        System.out.println("Received invalid message type: " + type);
                    }
            }

            // Close the client socket
            //conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
