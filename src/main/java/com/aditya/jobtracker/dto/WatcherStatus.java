package com.aditya.jobtracker.dto;

// what, where and everyMinutes are null when the watcher is not running.
public record WatcherStatus(boolean running, String what, String where, Integer everyMinutes) {
}
