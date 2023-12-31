import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WaitingJobs {
    private final int size;
    private static ArrayList<ArrayDeque<JobInfo>> jobs;
    private static Lock l = new ReentrantLock();

    public WaitingJobs(int size) {
        this.size = size;
        jobs = new ArrayList<>();
        for (int i = 0;i<size;i++) {
            jobs.add(new ArrayDeque<JobInfo>());
        }
    }

    public void addJob(JobInfo job) {
        l.lock();
        jobs.get(size-1).addLast(job);
        System.out.println(jobs);
        l.unlock();
    }

    public JobInfo getJob(int minSize) {
        l.lock();
        try {
            if (jobs.get(0).size()==0) {

                JobInfo job_chosen;
                for (ArrayDeque<JobInfo> jobsArray: jobs) {
                    if (jobsArray.size()>0) {
                        Iterator<JobInfo> iterator = jobsArray.iterator();
                        while (iterator.hasNext()) {
                            JobInfo j = iterator.next();
                            if (j.getSize()<=minSize) {
                                job_chosen = j;
                                iterator.remove();

                                // Roda a lista de prioridades
                                for (int i = 0; i < jobs.size() - 1; i++) {
                                    jobs.set(i, jobs.get(i + 1));
                                }
                                return job_chosen;
                            }
                        }
                    }
                }
            } else {
                return jobs.get(0).pollFirst();
            }

            return null;
        } finally {
            l.unlock();
        }

    }

    public int check_mandatoryJob() {
        if (jobs.get(0).size()>0) {
            return jobs.get(0).peekFirst().getSize();
        } else {
            return 0;
        }
    }

    public ArrayList<Byte> getWaitList() {
        ArrayList<Byte> r = new ArrayList<>();

        try {
            l.lock();
            for (ArrayDeque<JobInfo> jobsArray: jobs) {
                for (JobInfo j: jobsArray) {
                    r.add(j.getOriginalId());
                }
            }
            return r;
        } finally {
            l.unlock();
        }

    }

}
