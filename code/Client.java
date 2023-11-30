import java.net.Socket;
import java.net.UnknownHostException;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Arrays;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Client {
    public static void main(String[] args) {

            Account acc = new Account();
            boolean loggedIn = false;

        try {
            Socket s = new Socket("localhost", 22347);
            Demultiplexer m = new Demultiplexer(new Connection(s));
            
            int option = -1;
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

            while (true) {

                if (!loggedIn) {
                    System.out.print("\n//////////// LOGIN/REGISTO /////////////\n"
                            + "\n1) Registar nova conta.\n"
                            + "2) Iniciar sessão.\n"
                            + "\n");

                    try {
                        option = Integer.parseInt(stdin.readLine().trim());

                    if (option == 1) {
                            loggedIn = handle_register_login(m, stdin, acc);
                        } else if (option == 2) {
                            loggedIn = handle_login(m, stdin, acc);
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
                    while (loggedIn) {
                        System.out.print("\n//////////// Pedido de execução / Consulta do Estado /////////////\n"
                            + "\n1) Pedido de execução.\n"
                            + "2) Consulta do estado.\n"
                            + "3) Logout.\n"
                            + "\n");  
                        option = Integer.parseInt(stdin.readLine().trim());

                        if (option == 1) {
                            System.out.print("Enter the path of the file: ");
                            String filePath = stdin.readLine().trim();
                            new Thread(() -> {
                                try {
                                    handle_execution(filePath, m);
                                } catch (IOException e) {
                                    System.out.println("Error executing job: " + e.getMessage());
                                }
                            }).start();
                        } else if (option == 2) {
                        //   
                        }
                        else if (option == 3) {

                            Message message = new Message((byte)1, acc);
                            System.out.println(acc.toString());
                            m.send(message);

                            Payload reply = m.receive((byte)127);

                            BytePayload bytePayload = (BytePayload)reply;
                            byte payload = bytePayload.getData();

                            if (payload == 0) {
                                System.out.println("Logout efetuado com sucesso.");
                                acc.logOut();
                                loggedIn = false;
                            } else if (payload == -1) {
                                System.out.println("Erro ao efetuar logout.");
                            } else {
                                System.out.println("Erro desconhecido.");
                            }
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

                                    System.out.println(acc.toString());
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


    private static void handle_execution (String filePath, Demultiplexer m) throws IOException {
                                try {
                                    byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

                                    Message fileMessage = new Message((byte) 2, new BytesPayload(fileContent));
                                    if (fileMessage.getPayload() == null) {
                                        System.out.println("Error reading the file heee.");
                                        return;
                                    }

                                    m.send(fileMessage);

                                    Payload reply = m.receive((byte) 127);
                                    BytePayload bytePayload = (BytePayload) reply;
                                    byte payload = bytePayload.getData();

                                    if (payload == 0) {
                                        System.out.println("Job executed successfully.");
                                        reply = m.receive((byte) 2);
                                        BytesPayload bytesPayload = (BytesPayload) reply;
                                        byte[] bytes_payload = bytesPayload.getData();
                                        System.out.println("Byte Array as String: " + Arrays.toString(bytes_payload));
                                    } else if (payload == -1) {
                                        System.out.println("Error sending file.");
                                    } else {
                                        System.out.println("Unknown error.");
                                    }

                                } catch (IOException e) {
                                    System.out.println("Error reading the file: " + e.getMessage());
                                }
    }


}