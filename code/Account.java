import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;



public class Account implements Serializable {
    public final String username;
    public final String password;
    public boolean loggedIn = false;

    public Account() {
        this.username = null;
        this.password = null;
        this.loggedIn = false;
    }

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
        this.loggedIn = true; //Quando uma conta é criada, assume-se que o user fica logo loged in
    }

    public Account (String username, String password, boolean loggedIn) {
        this.username = username;
        this.password = password;
        this.loggedIn = loggedIn;
    }  

    public String toString() {
        return username + ":" + password + "\nLogged?: " + loggedIn;
    }

    public boolean equals(Object o) {
        if (o instanceof Account) {
            Account a = (Account) o;
            return username.equals(a.username) && password.equals(a.password) && loggedIn == a.loggedIn;
        } else {
            return false;
        }
    }

    public void serialize (ObjectOutputStream out) throws IOException{
        out.writeUTF (username);
        out.writeUTF (password);
        out.writeBoolean (loggedIn);
    }

    public Account deserialize (ObjectInputStream in) throws IOException{
        String username = in.readUTF();
        String password = in.readUTF();
        Boolean loggedIn = in.readBoolean();
        return new Account (username, password, loggedIn);
    }

}

