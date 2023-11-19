import java.net.Socket;
import java.net.UnknownHostException;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;


public class Client {
    public static void main(String[] args) {
        try {
            Socket s = new Socket("localhost", 22345);

            try {

                Connection conn = new Connection(s);

                int option = -1;
                BufferedReader stdin = new BufferedReader(new InputStreamReader (System.in));

                while (option == -1) {
                    System.out.print("\n//////////// LOGIN/REGISTO /////////////\n"
                            + "\n1) Registar nova conta.\n"
                            + "2) Iniciar sessão.\n"
                            + "\n");

                    try{
                        option = Integer.parseInt(stdin.readLine().trim());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid integer.");
                        option = -1;
                    }

                    if (option == 1) {
                        System.out.print("\n////////////REGISTAR NOVA CONTA/////////////\n"
                                + "\n"
                                + "Introduza o seu username: ");
                        try {
                            String username = stdin.readLine();

                            System.out.print("\nIntroduza a sua password: ");
                                String password = stdin.readLine();
                                Account acc = new Account(username, password);
                                System.out.println("Invalid input. Please enter a valid string for the password.");

                        } catch (IOException e) {
                            System.out.println("Invalid input. Please enter a valid string for the username.");
                            option = -1;
                        }
    
                    } if (option == 2) {
                        System.out.print("\n////////////INICIAR SESSÃO/////////////\n"
                                + "\n"
                                + "Introduza o seu username: ");
                        try {
                            String username = stdin.readLine();

                            System.out.print("\nIntroduza a sua password: ");
                            String password = stdin.readLine();
                            Account acc = new Account(username, password);
                            Message message = new Message(0, acc);

                            conn.send(message);
                        } catch (IOException e) {
                            System.out.println("Invalid input. Please enter a valid string.");
                            option = -1;
                        }

                    }
                    else {
                        System.out.println("Please input an integer that corresponds to one of the options.");
                        option = -1;
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
