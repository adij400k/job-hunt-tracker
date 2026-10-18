package com.aditya.jobtracker.security;

import com.aditya.jobtracker.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// CommandLineRunner = Spring runs this once, right after the app has started.
// It makes sure one ADMIN user exists. The name and password come from config.properties,
// so the password is never written in the code.
@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserService userService;
    private final String adminUsername;
    private final String adminPassword;

    // @Value reads one setting. After the colon is the default if the setting is missing.
    public AdminSeeder(UserService userService,
                       @Value("${admin.username:admin}") String adminUsername,
                       @Value("${admin.password:}") String adminPassword) {
        this.userService = userService;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminPassword.isBlank()) {
            System.out.println("No admin.password in config.properties, so no admin user was created.");
            return;
        }
        userService.createAdminIfMissing(adminUsername, adminPassword);
        System.out.println("Admin user '" + adminUsername + "' is ready.");
    }
}
