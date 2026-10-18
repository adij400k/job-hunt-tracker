package com.aditya.jobtracker.watcher;

import com.aditya.jobtracker.client.AdzunaClient;
import com.aditya.jobtracker.exception.ApiException;
import com.aditya.jobtracker.model.Job;
import com.aditya.jobtracker.service.JobTrackerService;
import java.util.List;

// Checks Adzuna again and again in the background.
// "implements Runnable" means this class has a run() method that
// a Thread can execute. Main keeps working while this runs on its own thread.
public class JobWatcher implements Runnable {

    private final JobTrackerService service;
    private final AdzunaClient client;
    private final String what;
    private final String where;
    private final long intervalMillis;
    private final String ownerUsername;     // the user who gets the jobs the watcher finds

    // volatile: when Main changes this to false, the watcher thread sees the
    // change straight away, instead of using an old copy it remembered.
    private volatile boolean running = true;

    private Thread thread;

    public JobWatcher(JobTrackerService service, AdzunaClient client,
                      String what, String where, long intervalMillis, String ownerUsername) {
        this.service = service;
        this.client = client;
        this.what = what;
        this.where = where;
        this.intervalMillis = intervalMillis;
        this.ownerUsername = ownerUsername;
    }

    public void start() {
        thread = new Thread(this, "job-watcher");
        // daemon thread = it won't keep the program alive after you choose Exit
        thread.setDaemon(true);
        thread.start();   // start() creates the new thread, which then calls run()
    }

    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();   // wakes the thread if it is sleeping
        }
    }

    public boolean isRunning() {
        return running && thread != null && thread.isAlive();
    }

    // This code runs on the background thread, not on the main thread.
    @Override
    public void run() {
        System.out.println("\n[Watcher] Started. Watching for '" + what + "'.");

        while (running) {
            try {
                // The network call happens here, outside any database transaction,
                // so nothing is locked while we wait for Adzuna.
                List<Job> found = client.searchJobs(what, where, 10);
                int added = service.addFetchedJobs(found, ownerUsername);

                if (added > 0) {
                    System.out.println("\n[Watcher] " + added + " new job(s) found! "
                            + "Check GET /api/jobs.");
                }
            } catch (ApiException e) {
                if (running) {
                    System.out.println("\n[Watcher] Check failed: " + e.getMessage());
                }
            } catch (RuntimeException e) {   // any database problem must not kill the thread
                System.out.println("\n[Watcher] Unexpected error: " + e.getMessage());
            }

            try {
                Thread.sleep(intervalMillis);   // wait before checking again
            } catch (InterruptedException e) {
                break;   // stop() interrupted our sleep, so we leave the loop
            }
        }
        System.out.println("\n[Watcher] Stopped.");
    }
}
