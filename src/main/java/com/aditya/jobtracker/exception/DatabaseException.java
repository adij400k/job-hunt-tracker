package com.aditya.jobtracker.exception;

// Our own exception for database problems.
// It extends RuntimeException (unchecked), so we don't have to add
// "throws" to every method that touches the database.
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }
}
