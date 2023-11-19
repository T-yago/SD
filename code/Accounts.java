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


    private static class AccountAlreadyExistsException extends RuntimeException {
        public AccountAlreadyExistsException(String username) {
            super("Account with username " + username + " already exists.");
        }
    }
}


 