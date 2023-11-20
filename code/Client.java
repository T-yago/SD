import java.net.Socket;
import java.net.UnknownHostException;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;


public class Client {
    public static void main(String[] args) {

            Account acc = null;
            boolean loggedIn = false;

        try {
            Socket s = new Socket("localhost", 22347);
            Connection conn = new Connection(s);
            
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
                            System.out.print("\n////////////REGISTAR NOVA CONTA/////////////\n"
                                    + "\n"
                                    + "Introduza o seu username: ");
                            String username = stdin.readLine().trim();

                            System.out.print("\nIntroduza a sua password: ");
                            String password = stdin.readLine().trim();
                            acc = new Account(username, password);
                            Message message = new Message((byte)0, acc);
                            conn.send(message);

                            Message reply = conn.receive();

                            BytePayload bytePayload = (BytePayload)reply.getPayload();
                            byte payload = bytePayload.getData();
                            
                            if (payload == 0) {
                                System.out.println("Registo efetuado com sucesso.");
                                loggedIn = true;
                            } else if (payload == -1) {
                                System.out.println("User já existe.");
                            } else {
                                System.out.println("Erro desconhecido.");
                            }

                        } else if (option == 2) {
                            System.out.print("\n////////////INICIAR SESSÃO/////////////\n"
                                    + "\n"
                                    + "Introduza o seu username: ");
                            String username = stdin.readLine().trim();

                            System.out.print("\nIntroduza a sua password: ");
                            String password = stdin.readLine().trim();
                            System.out.print(password);
                            acc = new Account(username, password,false);
                            Message message = new Message((byte)1, acc);
                            System.out.print("\nCriou mensagem: " + message.toString());

                            conn.send(message);

                            System.out.print("\nEnviou mensagem ");

                            Message reply = conn.receive();

                            BytePayload BytePayload = (BytePayload)reply.getPayload();
                            byte payload = BytePayload.getData();

                            if (payload == 0) {
                                System.out.println("Login efetuado com sucesso.");
                                loggedIn = true;
                            } else if (payload == -1) {
                                System.out.println("Conta já está logada.");
                            } else if (payload == -2) {
                                System.out.println("Username ou Passowrd incorretas.");
                            } else {
                                System.out.println("Erro desconhecido.");
                            }
                        } else {
                            System.out.println("Please input an integer that corresponds to one of the options.");
                            option = -1;
                        }
                    } catch (NumberFormatException | IOException e) {
                        System.out.println("Invalid input. Please enter a valid integer or string.");
                        //System.out.println (e.getMessage());
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
                            //TODO
                        }
                        else if (option == 2) {
                            //TODO
                        }
                        else if (option == 3) {

                            Message message = new Message((byte)1, acc);
                            conn.send(message);

                            Message reply = conn.receive();

                            BytePayload bytePayload = (BytePayload)reply.getPayload();
                            byte payload = bytePayload.getData();

                            if (payload == 0) {
                                System.out.println("Logout efetuado com sucesso.");
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
}

