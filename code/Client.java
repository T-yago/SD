import java.net.Socket;
import java.net.UnknownHostException;
import java.io.DataInputStream;
import java.io.IOException;


public class Client {
    public static void main(String[] args) {
        System.out.println("Client started.");
        try {
            Socket s = new Socket("localhost", 22345);

            try {

                Connection conn = new Connection(s);
                System.out.println("Client started.");

                int option = -1;
                BufferedReader stdin = new DataInputStream(System.in);

                while (option == -1) {
                    System.out.print("\n//////////// LOGIN/REGISTO /////////////\n"
                            + "\n1) Registar nova conta.\n"
                            + "2) Iniciar sessão.\n"
                            + "\n");

                    try {
                         System.out.print("Ola");
                        int ga = stdin.readInt();
                        System.out.print(ga);


                        String input = stdin.readUTF();
                        System.out.println(input);
                        option = Integer.parseInt(input.trim());
                    } catch (IOException | NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid integer.");
                        option = -1;
                    }

                    if (option == 1) {
                        System.out.print("\n////////////REGISTAR NOVA CONTA/////////////\n"
                                + "\n"
                                + "Introduza o seu username: ");
                        try {
                            String username = stdin.readUTF();

                            System.out.print("\nIntroduza a sua password: ");
                            try {
                                String password = stdin.readUTF();
                                Account acc = new Account(username, password);
                            } catch (IOException e) {
                                System.out.println("Invalid input. Please enter a valid string for the password.");
                            }
                        } catch (IOException e) {
                            System.out.println("Invalid input. Please enter a valid string for the username.");
                            option = -1;
                        }
                    } else if (option == 2) {
                        System.out.print("\n////////////INICIAR SESSÃO/////////////\n"
                                + "\n"
                                + "Introduza o seu username: ");
                        try {
                            String username = stdin.readUTF().trim();

                            System.out.print("\nIntroduza a sua password: ");
                            try {
                                String password = stdin.readUTF();
                                Account acc = new Account(username, password);
                                Message message = new Message(0, acc);

                                conn.send(message);
                            } catch (IOException e) {
                                System.out.println("Invalid input. Please enter a valid string.");
                                option = -1;
                            }
                        } catch (IOException e) {
                            System.out.println("Invalid input. Please enter a valid string.");
                            option = -1;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error creating connection, exiting program");
                System.exit(1);
            }
        } catch (IOException e) {
            System.out.println("Connection refused, exiting program");
            System.exit(1);
        }
    }
}
