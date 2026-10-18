package com.aditya.jobtracker.controller;

import com.aditya.jobtracker.client.AdzunaClient;
import com.aditya.jobtracker.dto.FetchResponse;
import com.aditya.jobtracker.exception.ApiException;
import com.aditya.jobtracker.model.Job;
import com.aditya.jobtracker.service.JobTrackerService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adzuna")
public class AdzunaController {

    private final AdzunaClient client;
    private final JobTrackerService service;

    public AdzunaController(AdzunaClient client, JobTrackerService service) {
        this.client = client;
        this.service = service;
    }

    // POST /api/adzuna/fetch?what=java developer&where=mumbai
    // The network call to Adzuna happens here, OUTSIDE the database transaction.
    // Only the saving inside the service is a transaction.
    @PostMapping("/fetch")
    public FetchResponse fetch(@RequestParam String what,
                               @RequestParam(defaultValue = "") String where,
                               Authentication authentication) throws ApiException {
        if (what.isBlank()) {
            throw new IllegalArgumentException("The 'what' value is required.");
        }
        List<Job> found = client.searchJobs(what.trim(), where.trim(), 10);
        // the fetched jobs are saved for the person who pressed the button
        int added = service.addFetchedJobs(found, authentication.getName());
        return new FetchResponse(found.size(), added);
    }
}
