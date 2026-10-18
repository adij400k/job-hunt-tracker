package com.aditya.jobtracker.dto;

// Every error is sent back in the same shape: {"message":"..."}
public record ErrorResponse(String message) {
}
