package com.aditya.jobtracker.repository;

import com.aditya.jobtracker.model.Application;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    // "Job" + "Id" = application.job.id. Finds the application of one job, if there is one.
    Optional<Application> findByJobId(int jobId);

    // "Job" + "Owner" + "Username" = application.job.owner.username
    // All applications of one user (used for the status summary and the applications list).
    List<Application> findByJobOwnerUsernameIgnoreCase(String username);
}
