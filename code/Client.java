import java.net.Socket;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Client {
    public static void main(String[] args) {

        Account acc = new Account();


        try {
            Socket s = new Socket("localhost", 22347);
            Demultiplexer m = new Demultiplexer(new Connection(s));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down gracefully...");
                try {
                    if (s != null && !s.isClosed()) {
                        handle_logout(m, acc);
                        s.close();
                        System.out.println("Client socket closed.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            
            int option = -1;
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

            while (true) {

                if (!acc.isLoggedIn()) {
                    System.out.print("\n//////////// LOGIN/REGISTO /////////////\n"
                            + "\n1) Registar nova conta.\n"
                            + "2) Iniciar sessão.\n"
                            + "\n");

                    try {
                        option = Integer.parseInt(stdin.readLine().trim());

                    if (option == 1) {
                            handle_register_login(m, stdin, acc);
                        } else if (option == 2) {
                            handle_login(m, stdin, acc);
                        }
                        else {
                            System.out.println("Please input an integer that corresponds to one of the options.");
                            option = -1;
                        }
                    } catch (NumberFormatException | IOException e) {
                        System.out.println("Invalid input. Please enter a valid integer or string.");
                        e.printStackTrace();
                        option = -1;
                    }
                } else {
                    while (acc.isLoggedIn()) {
                        System.out.print("\n//////////// Pedido de execução / Consulta do Estado /////////////\n"
                            + "\n1) Pedido de execução.\n"
                            + "2) Consulta do estado.\n"
                            + "3) Logout.\n"
                            + "\n");  
                        option = Integer.parseInt(stdin.readLine().trim());

                        if (option == 1) {
                            System.out.print("Enter the path of the file: ");
                            String filePath = stdin.readLine().trim();
                            System.out.println("How much memory will the job take? (in bytes)");
                            int memory = Integer.parseInt(stdin.readLine().trim());
                            new Thread(() -> {
                                try {
                                    handle_execution(acc,filePath, m, memory);
                                } catch (IOException e) {
                                    System.out.println("Error executing job: " + e.getMessage());
                                }
                            }).start();
                        } else if (option == 2) {
                            handle_check(m);
                        }
                        else if (option == 3) {
                            handle_logout(m, acc);
                        }
                        else {
                            System.out.println("Please input an integer that corresponds to one of the options.");
                            option = -1;
                        }

                    }
                    
                }
            }
        } catch (IOException e) {
            System.out.println("Error creating connection or connection refused, exiting program");
            System.exit(1);
        }
    }




    private static boolean handle_check (Demultiplexer m) throws IOException {
        Message message = new Message((byte)3, new BytePayload((byte)0));
        m.send(message);
        CustomBlockingQueue queue = (CustomBlockingQueue) m.receive((byte)4);
        if (queue == null) {
            System.out.println("Error receiving message.");
            return false;
        }
        System.out.println(queue.toString());
        return false;
    }

    private static boolean handle_register_login (Demultiplexer m, BufferedReader stdin, Account acc) throws IOException {
        System.out.print("\n////////////REGISTAR NOVA CONTA/////////////\n"
                                            + "\n"
                                            + "Introduza o seu username: ");
                                    String username = stdin.readLine().trim();

                                    System.out.print("\nIntroduza a sua password: ");
                                    String password = stdin.readLine().trim();
                                    acc.setUsername(username);
                                    acc.setPassword(password);
                                    Message message = new Message((byte)0, acc);
                                    m.send(message);

                                    BytePayload reply = (BytePayload) m.receive((byte)127);
                                    byte payload = reply.getData();
                                    
                                    if (payload == 0) {
                                        System.out.println("Registo efetuado com sucesso.");
                                        acc.logIn();
                                        return true;
                                    } else if (payload == -1) {
                                        System.out.println("User já existe.");
                                    } else {
                                        System.out.println("Erro desconhecido.");
                                    }
        return false;                              
    }



    private static boolean handle_login (Demultiplexer m, BufferedReader stdin, Account acc) throws IOException {
        System.out.print("\n////////////INICIAR SESSÃO/////////////\n"
                                            + "\n"
                                            + "Introduza o seu username: ");
                                    String username = stdin.readLine().trim();

                                    System.out.print("\nIntroduza a sua password: ");
                                    String password = stdin.readLine().trim();
                                    acc.setUsername(username);
                                    acc.setPassword(password);
                                    
                                    Message message = new Message((byte)1, acc);

                                    m.send(message);

                                    BytePayload reply = (BytePayload) m.receive((byte)127);
                                    byte payload = reply.getData();

                                    if (payload == 0) {
                                        acc.logIn();
                                        System.out.println("Login efetuado com sucesso.");
                                        return true;
                                    } else if (payload == -1) {
                                        System.out.println("Conta já está logada.");
                                    } else if (payload == -2) {
                                        System.out.println("Username ou Passowrd incorretas.");
                                    } else {
                                        System.out.println("Erro desconhecido.");
                                    }
        return false;
    }


    private static void handle_execution (Account acc, String filePath, Demultiplexer m, int memory) throws IOException {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            int jobID = acc.getJobCounter();
    
            byte[] memoryBytes = new byte[4];
            memoryBytes[0] = (byte) memory;
            memoryBytes[1] = (byte) (memory >> 8);
            memoryBytes[2] = (byte) (memory >> 16);
            memoryBytes[3] = (byte) (memory >> 24);

            byte[] idBytes = new byte[4];
            idBytes[0] = (byte) jobID;
            idBytes[1] = (byte) (jobID >> 8);
            idBytes[2] = (byte) (jobID >> 16);
            idBytes[3] = (byte) (jobID >> 24);

    
            // Concatenate the 'memory' byte array with the original byte array
            byte[] combinedBytes = new byte[memoryBytes.length + idBytes.length + fileContent.length];
            System.arraycopy(memoryBytes, 0, combinedBytes, 0, memoryBytes.length);
            System.arraycopy(idBytes, 0, combinedBytes, memoryBytes.length, idBytes.length);
            System.arraycopy(fileContent, 0, combinedBytes, memoryBytes.length + idBytes.length, fileContent.length);

            // Create BytesPayload based on combinedData
            Message fileMessage = new Message((byte) 2, new BytesPayload(combinedBytes));

        if (fileMessage.getPayload() == null) {
            System.out.println("Error reading the file here.");
            return;
        }

        m.send(fileMessage);

        Payload reply = m.receive((byte) 127);
        BytePayload bytePayload = (BytePayload) reply;
        byte payload = bytePayload.getData();

        if (payload == 0) {
            System.out.println("Job executed successfully.");

            String usernameDirectory = acc.getUsername();
            Path directoryPath = Paths.get(usernameDirectory);

            if (!Files.exists(directoryPath)) {
                Files.createDirectory(directoryPath);
            }

            reply = m.receive((byte) 2);
            BytesPayload bytesPayload = (BytesPayload) reply;
            int id = bytesPayload.readFirstInt();

            Path outputPath = Paths.get(usernameDirectory, "output_" + id);  
            
            try (DataOutputStream dataOutputStream = new DataOutputStream(Files.newOutputStream(outputPath))) {
                dataOutputStream.write(bytesPayload.getData());
            } catch (IOException e) {
                System.out.println("Error writing to the file: " + e.getMessage());
            }

        } else if (payload == -1) {
            System.out.println("Error sending file.");
        } else {
            System.out.println("Unknown error.");
        }

    } catch (IOException e) {
        System.out.println("Error reading the file: " + e.getMessage());
    }
    }


    private static void handle_logout (Demultiplexer m, Account acc ) {
        try{
        Message message = new Message((byte)1, acc);
        m.send(message);

        Payload reply = m.receive((byte)127);

        BytePayload bytePayload = (BytePayload)reply;
        byte payload = bytePayload.getData();

        if (payload == 0) {
            System.out.println("Logout efetuado com sucesso.");
            acc.logOut();
        } else if (payload == -1) {
            System.out.println("Erro ao efetuar logout.");
        } else {
            System.out.println("Erro desconhecido.");
        }
        } catch (IOException e) {
            System.out.println("Error sending message.");
        }
    }

}