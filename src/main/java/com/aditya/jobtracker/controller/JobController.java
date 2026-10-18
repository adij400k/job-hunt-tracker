package com.aditya.jobtracker.controller;

import com.aditya.jobtracker.dto.CreateJobRequest;
import com.aditya.jobtracker.dto.JobResponse;
import com.aditya.jobtracker.dto.SkillRequest;
import com.aditya.jobtracker.exception.DuplicateJobException;
import com.aditya.jobtracker.exception.JobNotFoundException;
import com.aditya.jobtracker.model.FullTimeJob;
import com.aditya.jobtracker.model.InternshipJob;
import com.aditya.jobtracker.model.Job;
import com.aditya.jobtracker.service.JobTrackerService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// REST idea: the URL names a THING (jobs), the HTTP verb says what to DO with it.
//   GET = read, POST = create, PUT = change, DELETE = remove.
// @RequestMapping("/api/jobs") = every method below starts with this URL.
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobTrackerService service;

    // Spring injects the service (dependency injection)
    public JobController(JobTrackerService service) {
        this.service = service;
    }

    // GET /api/jobs            -> all jobs
    // GET /api/jobs?skill=Java -> only jobs that need Java
    // @RequestParam = a value from the URL after the ?
    @GetMapping
    // Authentication = the logged-in person. Spring fills this parameter in for us.
    // authentication.getName() is the username, taken from the login and not from the request.
    public List<JobResponse> list(@RequestParam(required = false) String skill,
                                  Authentication authentication) {
        String username = authentication.getName();
        List<Job> jobs = (skill == null || skill.isBlank())
                ? service.getJobsOf(username)
                : service.findJobsBySkill(skill, username);
        return jobs.stream().map(JobResponse::from).toList();
    }

    // GET /api/jobs/5
    // @PathVariable = the {id} part of the URL
    @GetMapping("/{id}")
    public JobResponse get(@PathVariable int id, Authentication authentication)
            throws JobNotFoundException {
        return JobResponse.from(service.findJob(id, authentication.getName()));
    }

    // POST /api/jobs  with a JSON body
    // @RequestBody = Spring turns the JSON body into a CreateJobRequest object
    @PostMapping
    public ResponseEntity<JobResponse> create(@RequestBody CreateJobRequest req,
                                              Authentication authentication)
            throws DuplicateJobException {
        require(req.title(), "title");
        require(req.company(), "company");
        require(req.location(), "location");

        Job job;
        if ("INTERNSHIP".equalsIgnoreCase(req.type())) {
            int months = (req.durationMonths() == null) ? 0 : req.durationMonths();
            job = new InternshipJob(req.title(), req.company(), req.location(), req.salary(), months);
        } else {
            int years = (req.experienceYears() == null) ? -1 : req.experienceYears();
            job = new FullTimeJob(req.title(), req.company(), req.location(), req.salary(), years);
        }
        if (req.applyLink() != null) {
            job.setApplyLink(req.applyLink());
        }

        Job saved = service.addJob(job, authentication.getName());
        // 201 Created is the correct status for "a new thing was made"
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(saved));
    }

    // DELETE /api/jobs/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) throws JobNotFoundException {
        service.deleteJob(id);
        return ResponseEntity.noContent().build();      // 204 = done, nothing to send back
    }

    // POST /api/jobs/5/skills  with {"name":"Java"}
    @PostMapping("/{id}/skills")
    public JobResponse addSkill(@PathVariable int id, @RequestBody SkillRequest req,
                                Authentication authentication)
            throws JobNotFoundException {
        require(req.name(), "name");
        return JobResponse.from(service.addSkill(id, req.name(), authentication.getName()));
    }

    // Small helper: a required text field must not be missing or blank
    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The field '" + field + "' is required.");
        }
    }
}
