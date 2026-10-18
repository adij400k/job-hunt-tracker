package com.aditya.jobtracker.dto;

// Who you are. We never send the password or the hash back.
public record MeResponse(String username, String role) {
}
