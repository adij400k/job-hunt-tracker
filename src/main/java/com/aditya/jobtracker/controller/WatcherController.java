package com.aditya.jobtracker.controller;

import com.aditya.jobtracker.dto.WatcherStatus;
import com.aditya.jobtracker.exception.WatcherException;
import com.aditya.jobtracker.watcher.WatcherManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// start and stop are ADMIN only (see SecurityConfig). status is open to any logged-in user.
@RestController
@RequestMapping("/api/watcher")
public class WatcherController {

    private final WatcherManager manager;

    public WatcherController(WatcherManager manager) {
        this.manager = manager;
    }

    // POST /api/watcher/start?what=java developer&where=mumbai&minutes=5
    @PostMapping("/start")
    public WatcherStatus start(@RequestParam String what,
                               @RequestParam(defaultValue = "") String where,
                               @RequestParam int minutes,
                               Authentication authentication) throws WatcherException {
        // the watcher saves its jobs for the admin who started it
        manager.start(what, where, minutes, authentication.getName());
        return manager.status();
    }

    @PostMapping("/stop")
    public WatcherStatus stop() throws WatcherException {
        manager.stop();
        return manager.status();
    }

    @GetMapping("/status")
    public WatcherStatus status() {
        return manager.status();
    }
}
