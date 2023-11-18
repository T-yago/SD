import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.*;

public class Accounts implements Serializable {
    private Map<String, Account> accountMap;
    private ReentrantReadWriteLock lock;

    public Accounts() {
        this.accountMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void addAccount(String username, String password) {
        lock.writeLock().lock();
        try {
            if (accountMap.containsKey(username)) {
                throw new AccountAlreadyExistsException(username);
            }
            Account account = new Account (username, password);
            accountMap.put(username, account);
        } finally {
            lock.writeLock().unlock();
        }
    }


    public boolean containsAccount(String username) {
        lock.readLock().lock();
        try {
            return accountMap.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }


    private static class AccountAlreadyExistsException extends RuntimeException {
        public AccountAlreadyExistsException(String username) {
            super("Account with username " + username + " already exists.");
        }
    }
}


 