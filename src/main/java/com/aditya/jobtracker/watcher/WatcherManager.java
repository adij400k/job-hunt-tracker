package com.aditya.jobtracker.watcher;

import com.aditya.jobtracker.client.AdzunaClient;
import com.aditya.jobtracker.dto.WatcherStatus;
import com.aditya.jobtracker.exception.WatcherException;
import com.aditya.jobtracker.service.JobTrackerService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

// Holds the one running JobWatcher.
// We create a new JobWatcher with "new" each time, because a stopped thread can't restart
// and each watcher has its own settings. That is why JobWatcher is not a bean itself.
// The manager IS a bean, so the controller can use it.
@Component
public class WatcherManager {

    private final JobTrackerService service;
    private final AdzunaClient client;

    private JobWatcher watcher;     // null until started
    private String what;
    private String where;
    private int minutes;

    public WatcherManager(JobTrackerService service, AdzunaClient client) {
        this.service = service;
        this.client = client;
    }

    // synchronized: two web requests could arrive at the same moment (two threads)
    public synchronized void start(String what, String where, int minutes, String ownerUsername)
            throws WatcherException {
        if (watcher != null && watcher.isRunning()) {
            throw new WatcherException("The watcher is already running.");
        }
        if (what == null || what.isBlank()) {
            throw new IllegalArgumentException("The 'what' value is required.");
        }
        if (minutes < 1) {
            throw new IllegalArgumentException("Check every 1 minute or more.");
        }
        this.what = what.trim();
        this.where = (where == null) ? "" : where.trim();
        this.minutes = minutes;
        watcher = new JobWatcher(service, client, this.what, this.where, minutes * 60_000L, ownerUsername);
        watcher.start();
    }

    public synchronized void stop() throws WatcherException {
        if (watcher == null || !watcher.isRunning()) {
            throw new WatcherException("The watcher isn't running.");
        }
        watcher.stop();
    }

    public synchronized WatcherStatus status() {
        if (watcher != null && watcher.isRunning()) {
            return new WatcherStatus(true, what, where, minutes);
        }
        return new WatcherStatus(false, null, null, null);
    }

    // Runs when Spring shuts down, so the thread is stopped cleanly
    @PreDestroy
    public synchronized void shutdown() {
        if (watcher != null && watcher.isRunning()) {
            watcher.stop();
        }
    }
}
