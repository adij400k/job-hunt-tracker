package com.aditya.jobtracker.dto;

// {"username":"aditya","password":"secret123"}
public record RegisterRequest(String username, String password) {
}
