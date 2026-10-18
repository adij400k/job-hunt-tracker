package com.aditya.jobtracker.controller;

import com.aditya.jobtracker.dto.ApplicationResponse;
import com.aditya.jobtracker.dto.NoteRequest;
import com.aditya.jobtracker.dto.NoteResponse;
import com.aditya.jobtracker.dto.StatusRequest;
import com.aditya.jobtracker.exception.DuplicateJobException;
import com.aditya.jobtracker.exception.InvalidStatusChangeException;
import com.aditya.jobtracker.exception.JobNotFoundException;
import com.aditya.jobtracker.model.Application;
import com.aditya.jobtracker.model.ApplicationStatus;
import com.aditya.jobtracker.service.JobTrackerService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final JobTrackerService service;

    public ApplicationController(JobTrackerService service) {
        this.service = service;
    }

    // GET /api/applications
    @GetMapping("/applications")
    public List<ApplicationResponse> listApplications(Authentication authentication) {
        return service.getApplicationsOf(authentication.getName()).stream()
                .map(ApplicationResponse::from).toList();
    }

    // GET /api/applications/summary  -> {"APPLIED":2,"INTERVIEW":1}
    @GetMapping("/applications/summary")
    public Map<String, Long> summary(Authentication authentication) {
        return service.getApplicationsOf(authentication.getName()).stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));
    }

    // POST /api/jobs/5/apply   (no body needed)
    @PostMapping("/jobs/{id}/apply")
    public ResponseEntity<ApplicationResponse> apply(@PathVariable int id, Authentication authentication)
            throws JobNotFoundException, DuplicateJobException {
        Application app = service.applyTo(id, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(app));
    }

    // PUT /api/jobs/5/status  with {"status":"INTERVIEW"}
    // PUT = change something that already exists
    @PutMapping("/jobs/{id}/status")
    public ApplicationResponse updateStatus(@PathVariable int id, @RequestBody StatusRequest req,
                                            Authentication authentication)
            throws JobNotFoundException, InvalidStatusChangeException {
        ApplicationStatus next;
        try {
            // valueOf turns the text into an enum value, or throws if the text is not a real status
            next = ApplicationStatus.valueOf(req.status().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Status must be one of: SAVED, APPLIED, INTERVIEW, OFFER, REJECTED.");
        }
        return ApplicationResponse.from(service.updateStatus(id, next, authentication.getName()));
    }

    // POST /api/jobs/5/notes  with {"text":"HR will call on Monday"}
    @PostMapping("/jobs/{id}/notes")
    public ResponseEntity<NoteResponse> addNote(@PathVariable int id, @RequestBody NoteRequest req,
                                                Authentication authentication)
            throws JobNotFoundException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NoteResponse.from(service.addNote(id, req.text(), authentication.getName())));
    }

    // GET /api/jobs/5/notes
    @GetMapping("/jobs/{id}/notes")
    public List<NoteResponse> listNotes(@PathVariable int id, Authentication authentication)
            throws JobNotFoundException {
        return service.listNotes(id, authentication.getName()).stream().map(NoteResponse::from).toList();
    }
}
