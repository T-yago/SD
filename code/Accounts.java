import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.*;

public class Accounts implements Serializable {
    private Map<String, Account> accounts;
    private ReentrantReadWriteLock lock;

    public Accounts() {
        this.accounts = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void addAccount(String username, String password) {
        lock.writeLock().lock();
        try {
            if (accounts.containsKey(username)) {
                throw new AccountAlreadyExistsException(username);
            }
            Account account = new Account (username, password);
            accounts.put(username, account);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void logInUser(String username, String password) {
        lock.writeLock().lock();
        try {
            if (!accounts.containsKey(username)) {
                throw new IncorrectLoginDataException();
            }

            Account account = accounts.get(username);

            if (account.isLoggedIn()) {
                throw new AccountAlreadyLogged(account.getUsername());
            }

            if (!account.getPassword().equals(password)) {
                throw new IncorrectLoginDataException();
            }

            account.loggedIn = true;
        } finally {
            lock.writeLock().unlock();
        }   
    }



    public boolean containsAccount(String username) {
        lock.readLock().lock();
        try {
            return accounts.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isLoggedIn (String username) {
        lock.readLock().lock();
        try{
            return accounts.get(username).isLoggedIn();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void logOutUser (String username) {
        lock.writeLock().lock();
        try {
            accounts.get(username).loggedIn = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void printAccounts () {
        lock.readLock().lock();
        try {
            for (Account a : accounts.values()) {
                System.out.println(a.toString());
            }
        } finally {
            lock.readLock().unlock();
        }
    }


    public static class AccountAlreadyExistsException extends RuntimeException {
        public AccountAlreadyExistsException(String username) {
            System.out.println("Account with username " + username + " already exists.");
        }
    }

    public static class IncorrectLoginDataException extends RuntimeException {
        public IncorrectLoginDataException() {
            System.out.println("Invalid Username or Password.");
        }
    }

    public static class AccountAlreadyLogged extends RuntimeException {
        public AccountAlreadyLogged(String username) {
            System.out.println("User " + username + " is already logged in.");
        }
    }
}


 