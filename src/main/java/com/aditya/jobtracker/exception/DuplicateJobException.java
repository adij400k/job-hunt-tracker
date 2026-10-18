package com.aditya.jobtracker.exception;

// Thrown when the same job is added twice, or applied to twice.
public class DuplicateJobException extends Exception {

    public DuplicateJobException(String message) {
        super(message);
    }
}
