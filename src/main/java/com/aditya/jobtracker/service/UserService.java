package com.aditya.jobtracker.service;

import com.aditya.jobtracker.exception.DuplicateUserException;
import com.aditya.jobtracker.model.AppUser;
import com.aditya.jobtracker.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserService {

    private final AppUserRepository repo;
    private final PasswordEncoder encoder;   // the BCrypt bean from SecurityConfig

    public UserService(AppUserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    // Anyone can register, but only as a normal USER.
    public AppUser register(String username, String password) throws DuplicateUserException {
        if (username == null || username.trim().length() < 3 || username.trim().length() > 50) {
            throw new IllegalArgumentException("The username must be 3 to 50 characters.");
        }
        if (username.trim().contains(" ")) {
            throw new IllegalArgumentException("The username can't contain spaces.");
        }
        // BCrypt only reads the first 72 characters, so we limit the length
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new IllegalArgumentException("The password must be 6 to 72 characters.");
        }
        String name = username.trim();
        if (repo.existsByUsernameIgnoreCase(name)) {
            throw new DuplicateUserException("That username is already taken.");
        }
        // encode() = hash the password with BCrypt (it also adds a random "salt")
        return repo.save(new AppUser(name, encoder.encode(password), "USER"));
    }

    // Used once at startup to make sure an admin exists
    public void createAdminIfMissing(String username, String password) {
        if (!repo.existsByUsernameIgnoreCase(username)) {
            repo.save(new AppUser(username, encoder.encode(password), "ADMIN"));
        }
    }
}
