package com.aditya.jobtracker.exception;

// Thrown when someone registers with a username that is already taken.
public class DuplicateUserException extends Exception {

    public DuplicateUserException(String message) {
        super(message);
    }
}
