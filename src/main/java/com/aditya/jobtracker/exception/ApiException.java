package com.aditya.jobtracker.exception;

// Thrown for anything that goes wrong while talking to Adzuna:
// no internet, wrong key, limit reached, or unreadable data.
public class ApiException extends Exception {

    public ApiException(String message) {
        super(message);
    }
}
