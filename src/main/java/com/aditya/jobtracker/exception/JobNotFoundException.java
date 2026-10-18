package com.aditya.jobtracker.exception;

// Thrown when we look for a job id that doesn't exist.
public class JobNotFoundException extends Exception {

    public JobNotFoundException(int id) {
        super("No job found with id " + id);
    }
}
