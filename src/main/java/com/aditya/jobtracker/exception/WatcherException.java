package com.aditya.jobtracker.exception;

// Thrown when the watcher is started twice, or stopped when it isn't running.
public class WatcherException extends Exception {

    public WatcherException(String message) {
        super(message);
    }
}
