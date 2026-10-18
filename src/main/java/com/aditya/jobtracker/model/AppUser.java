package com.aditya.jobtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A person who can log in. Each object is one row in the app_users table.
// (We don't call the table "users" because that word is reserved in some databases.)
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // We NEVER store the real password. We store a BCrypt hash of it.
    // A hash can't be turned back into the password, so a database leak doesn't reveal it.
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // "USER" or "ADMIN"
    @Column(nullable = false, length = 20)
    private String role;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
}
