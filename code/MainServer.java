import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sd23.*;



public class MainServer {


    private static ArrayList<WorkerInfo> workers = new ArrayList<>();
    private static Accounts accounts = new Accounts();
    private static WaitingJobs waitingJobs = new WaitingJobs(50);
    private static byte idCounter = (byte) 16;
    private static Lock idCounter_Lock = new ReentrantLock();
    private static Map<Integer, JobInfo> mapJobs = new HashMap<>();
    private static Lock mapJobs_Lock = new ReentrantLock();
    private static Lock awake_send_Thread_Lock = new ReentrantLock();
    private static Condition awake_send_Thread_cond = awake_send_Thread_Lock.newCondition();

    private static class WorkerInfo {
        private final Connection c;
        private int total_mem;
        private ReentrantLock lock = new ReentrantLock();

        WorkerInfo (int total_Mem, Connection c) {
            this.c = c;
            this.total_mem = total_Mem;
        }

        int getMem() {
            return this.total_mem;
        }

        Connection getConnection() {
            return this.c;
        }

        void updateMem(int mem_update) {
            this.total_mem += mem_update;
        }

        void lock() {
            this.lock.lock();
        }

        void unlock() {
            this.lock.unlock();
        }
    }


    static class JobMemoryException extends Exception {
        public JobMemoryException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Server <num_Clients>");
            System.exit(1);
        }
        int numThreads = Integer.parseInt(args[0]);

        // Inicia a thread que envia os pedidos para os workers
        new Thread ( () -> {

            while (true) {
                // Verifica se existe algum pedido que tem prioridade sobre os outros
                if (waitingJobs.check_mandatoryJob()>0) {
                    int minMemory = waitingJobs.check_mandatoryJob();

                    // Calcula a máxima memória disponível
                    int maxMem = 0;
                    WorkerInfo worker = null;
                    while (maxMem<minMemory) {
                        for (WorkerInfo w : workers) {
                            if (w.getMem() > maxMem) {
                                maxMem = w.getMem();
                                worker = w;
                            }
                        }

                        if (maxMem < minMemory) {
                            try {
                                awake_send_Thread_cond.await();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    JobInfo job = waitingJobs.getJob(maxMem);

                    // Envia o job para o worker que tem mais espaço livre
                    Message m = new Message((byte) job.getId(), job.getPayload());
                    try {
                        worker.getConnection().send(m);
                    } catch (IOException e) {
                        // Remove o worker da lista de workers disponíves
                        for (WorkerInfo w: workers) {
                            w.lock();
                        }
                        workers.remove(worker);
                        for (WorkerInfo w: workers) {
                            w.unlock();
                        }
                    }
                    worker.updateMem(-minMemory);

                } else {
                    // Calcula a máxima memória disponível
                    int maxMem = 0;
                    WorkerInfo worker = null;
                    while (maxMem==0) {
                        for (WorkerInfo w: workers) {
                            if (w.getMem() > maxMem) {
                                maxMem = w.getMem();
                                worker = w;
                            }
                        }

                        if (maxMem==0) {
                            try {
                                awake_send_Thread_Lock.lock();
                                awake_send_Thread_cond.await();
                                awake_send_Thread_Lock.unlock();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }

                    JobInfo job = waitingJobs.getJob(maxMem);
                    if (job==null) {
                        try {
                            System.out.println("VOU DORMIRRRRRRRRRRRRRRRRRRRRRRRRRRR.");
                            awake_send_Thread_Lock.lock();
                            awake_send_Thread_cond.await();
                            awake_send_Thread_Lock.unlock();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {

                        // Envia o job para o worker que tem mais espaço livre
                        Message m = new Message((byte) job.getId(), job.getPayload());
                        try {
                            System.out.println("MANDEI PARA O WORKER FILHÃO.");
                            worker.getConnection().send(m);
                            System.out.println("MANDEI PARA O WORKER FILHÃO.");
                        } catch (IOException e) {
                            // Remove o worker da lista de workers disponíves
                            for (WorkerInfo w: workers) {
                                w.lock();
                            }
                            workers.remove(worker);
                            for (WorkerInfo w: workers) {
                                w.unlock();
                            }
                        }

                        worker.updateMem(-job.getSize());
                    }

                }
            }
        }).start();

        try {
            final ServerSocket serverSocket = new ServerSocket(22347);

            // Thread que espera por um sinal de interrupção e fecha o servidor, caso este apareça
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down gracefully...");
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                        System.out.println("Server socket closed.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            for (int i = 0; i < numThreads; i++) {
                Thread clientThread = new Thread(() -> {
                    while (true) {
                        Socket clientSocket;
                        try {
                            clientSocket = serverSocket.accept();
                            Connection connection = new Connection(clientSocket);
                            new Thread (() -> {
                                try {
                                    handleConnection(connection);
                                } catch (JobMemoryException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                        } catch (IOException e) {
                            System.out.println("Error handling client");;
                        }
                    }
                });
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Error creating server socket.");
            System.exit(1);
        }
    }

    private static void handleConnection(Connection conn) throws JobMemoryException {
        try (conn) {
            while (true) {
                System.out.println("Começou!");
                Message message = conn.receive();
                byte type = message.getType();

                System.out.println("Received message type: " + type);
        
                    if (type == 0) { // Registo
                        Account account = (Account) message.getPayload();
                        try {
                            accounts.addAccount(account.getUsername(), account.getPassword());
                            System.out.println("Account created successfully.");
                            accounts.printAccounts();
                            conn.send(new Message((byte)127,new BytePayload((byte)0)));
                            System.out.println("Sent confirmation.");


                        } catch (Exception e) {
                            conn.send(new Message((byte)127,new BytePayload((byte)-1)));
                            System.out.println("Error creating account.");
                        }

                    } else if (type == 1) { // Login / Logout, dependendo do estado da conta
                        Account account = (Account) message.getPayload();
                        if (!account.isLoggedIn()) {
                            try{
                                accounts.logInUser(account.getUsername(), account.getPassword());
                                System.out.println("Account login successfull.");
                                conn.send(new Message((byte)127,new BytePayload((byte)0)));

                            } catch (Accounts.AccountAlreadyLogged e) { // caso em que a conta já está logada
                                conn.send(new Message((byte)127,new BytePayload((byte)-1)));

                            } catch (Accounts.IncorrectLoginDataException e) { // caso em que a password ou user estão errados  
                                conn.send(new Message((byte)127,new BytePayload((byte)-2)));

                            } catch (Exception e) { // erro desconhecido
                                conn.send(new Message((byte)127,new BytePayload((byte)-3)));
                            }
                        }
                        else {
                            try{
                                accounts.logOutUser(account.getUsername());
                                System.out.println("Account logout successfull.");
                                conn.send(new Message((byte)127,new BytePayload((byte)0)));
                            } catch (Exception e) {
                                conn.send(new Message((byte)127,new BytePayload((byte)-1)));
                                e.printStackTrace();
                            }
                        }

                    } else if (type == 2) { // Encaminhar o pedido para um worker
                        System.out.println("ENTROU");
                        BytesPayload bytesPayload = (BytesPayload) message.getPayload();
                        int mem_require = bytesPayload.readFirstInt();
                        int id = bytesPayload.readSecondInt();

                        // Insere o pedido no map de pedidos a enviar
                        idCounter_Lock.lock();
                        int new_id = idCounter++;
                        idCounter_Lock.unlock();

                        Lock lock_wake_me = new ReentrantLock();
                        Condition cond = lock_wake_me.newCondition();
                        JobInfo jobInfo = new JobInfo(new_id, id, conn, lock_wake_me, cond, mem_require, bytesPayload);
                        mapJobs_Lock.lock();
                        mapJobs.put(new_id, jobInfo);
                        mapJobs_Lock.unlock();
                        waitingJobs.addJob(jobInfo);
                        awake_send_Thread_Lock.lock();
                        awake_send_Thread_cond.signal();
                        awake_send_Thread_Lock.unlock();

                        System.out.println("Estou à espera da resposta");

                        // Thread espera pela resposta
                        try {
                            mapJobs_Lock.lock();
                            while (mapJobs.get(new_id).getAnswer_job()==null) {
                                lock_wake_me.lock();
                                cond.await();
                                lock_wake_me.unlock();
                            }
                        } catch (InterruptedException e) {
                            mapJobs_Lock.unlock();
                        }

                        System.out.println("Já tenho a resposta");

                        // Envia a resposta
                        byte[] answer = mapJobs.get(new_id).getAnswer_job();
                        byte[] answerWithId = new byte[answer.length + 4];
                        answerWithId[0] = (byte) id;
                        answerWithId[1] = (byte) (id >> 8);
                        answerWithId[2] = (byte) (id >> 16);
                        answerWithId[3] = (byte) (id >> 24);
                        System.arraycopy(answer, 0, answerWithId, 4, answer.length);

                        System.err.println("success, returned " + answer.length + " bytes");
                        conn.send(new Message((byte) 127, new BytePayload((byte) 0)));
                        conn.send(new Message((byte) 2, new BytesPayload(answerWithId)));
                    } else if (type == 3) {

                        // Vai buscar a memória total livre e a fila de espera
                        System.out.println("TOU AQUI.");
                        int totalFreeSpace = 0;
                        for (WorkerInfo w: workers) {
                            totalFreeSpace += w.getMem();
                        }
                        ArrayList<Byte> waitList = waitingJobs.getWaitList();

                        // Converte os dois para binário e concatena-os
                        byte[] answer = new byte[waitList.size() + 4];
                        answer[0] = (byte) totalFreeSpace;
                        answer[1] = (byte) (totalFreeSpace >> 8);
                        answer[2] = (byte) (totalFreeSpace >> 16);
                        answer[3] = (byte) (totalFreeSpace >> 24);

                        for (int i = 0;i<waitList.size();i++) {
                            answer[i+4] = waitList.get(i);
                        }

                        Payload payload = new BytesPayload(answer);
                        System.out.println("BYTES -> " + answer.toString());
                        Message reply = new Message((byte) 4, payload);
                        conn.send(reply);

                    } else if (type == 10) { // Dá origem a um worker
                        BytesPayload bytesPayload = (BytesPayload) message.getPayload();
                        int total_Mem = bytesPayload.readFirstInt();
                        WorkerInfo worker = new WorkerInfo(total_Mem, conn);

                        workers.add(worker);

                        boolean flag = true;
                        while (flag) {
                            try {
                                message = conn.receive();
                            } catch (IOException e) {
                                for (WorkerInfo w : workers) {
                                    w.lock();
                                }
                                workers.remove(worker);
                                for (WorkerInfo w : workers) {
                                    w.unlock();
                                }
                                flag = false;
                            }

                            if (flag) {
                                int id = (int) message.getType();

                                BytesPayload bytesPayload1 = (BytesPayload) message.getPayload();

                                // Adiciona ao map de respostas a jobs e avisa a thread que estava à espera
                                JobInfo j = mapJobs.get(id);
                                byte[] answer = Arrays.copyOfRange(((BytesPayload) message.getPayload()).getData(), 4, ((BytesPayload) message.getPayload()).getData().length);
                                j.answerJob(answer);
                                worker.updateMem(mapJobs.get(id).getSize());
                                awake_send_Thread_Lock.lock();
                                awake_send_Thread_cond.signal();
                                awake_send_Thread_Lock.unlock();
                            }
                        }
                        } else {
                        System.out.println("Received invalid message type: " + type);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client.");
                e.printStackTrace();
            }
        }
}

