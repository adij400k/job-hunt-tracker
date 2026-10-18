package com.aditya.jobtracker.exception;

// Thrown when someone tries a status change the rules don't allow.
public class InvalidStatusChangeException extends Exception {

    public InvalidStatusChangeException(String message) {
        super(message);
    }
}
