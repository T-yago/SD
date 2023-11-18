import java.io.Serializable;


public class Account implements Serializable {
    public final String username;
    public final String password;

    public Account() {
        this.name = null;
        this.password = null;
    }

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String toString() {
        return username + ":" + password;
    }

    public boolean equals(Object o) {
        if (o instanceof Account) {
            Account a = (Account) o;
            return username.equals(a.username) && password.equals(a.password);
        } else {
            return false;
        }
    }

    @Override
    public void serialize (ObjectOutputStream out) {
        out.writeUTF (username);
        out.writeUTF (password);
    }

    @Override
    public Account deserialize (ObjectInputStream in) {
        username = in.readUTF();
        password = in.readUTF();
        return new Account (username, password);
    }

}

