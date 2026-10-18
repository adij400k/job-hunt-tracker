package com.aditya.jobtracker.model;

// An enum is a fixed set of allowed values. A status can only be one of these,
// so we can't make a typo like "Intervew" anywhere in the program.
public enum ApplicationStatus {

    SAVED, APPLIED, INTERVIEW, OFFER, REJECTED;

    // The rule for which status can come after which.
    // Example: you can't go from SAVED straight to OFFER.
    public boolean canMoveTo(ApplicationStatus next) {
        switch (this) {
            case SAVED:
                return next == APPLIED;
            case APPLIED:
                return next == INTERVIEW || next == REJECTED;
            case INTERVIEW:
                return next == OFFER || next == REJECTED;
            default:
                return false;   // OFFER and REJECTED are the end
        }
    }
}
