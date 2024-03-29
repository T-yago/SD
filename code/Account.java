import java.io.Serializable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;



public class Account implements Payload {
    public String username;
    public String password;
    public boolean loggedIn = false;
    private int jobCounter = 0; 


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

    public void setPassword (String password) {
        this.password = password;
    }

    public void setUsername (String username) {
        this.username = username;
    }

    public void logIn() {
        loggedIn = true;
    }

    public void logOut() {
        loggedIn = false;
    }

    public String getUsername() {
        return username;
    }

    public int getJobCounter() {
        return jobCounter++;
    }

    public String getPassword() {
        return password;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String toString() {
        return username + ":" + password + "\nLogged?: " + loggedIn;
    }

    public boolean equals(Object o) {
        if (o instanceof Account) {
            Account a = (Account) o;
            return username.equals(a.username);
        } else {
            return false;
        }
    }

    @Override
    public void serialize (DataOutputStream out) throws IOException{
        out.writeUTF (username);
        out.writeUTF (password);
        out.writeBoolean (loggedIn);
    }

    @Override
    public Account deserialize (DataInputStream in) throws IOException{
        String username = in.readUTF();
        String password = in.readUTF();
        Boolean loggedIn = in.readBoolean();
        return new Account (username, password, loggedIn);
    }

}

