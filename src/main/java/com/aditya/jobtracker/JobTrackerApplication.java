package com.aditya.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication = configuration + component scan + auto-configuration.
// The scan covers this package and every package below it (model, repository, service, controller).
@SpringBootApplication
public class JobTrackerApplication {

    public static void main(String[] args) {
        // Java 26 is newer than the Byte Buddy library inside Hibernate expects (same as Stage 5).
        System.setProperty("net.bytebuddy.experimental", "true");
        SpringApplication.run(JobTrackerApplication.class, args);
    }
}
