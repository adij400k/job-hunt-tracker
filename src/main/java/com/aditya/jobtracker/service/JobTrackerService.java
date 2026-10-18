package com.aditya.jobtracker.service;

import com.aditya.jobtracker.exception.DuplicateJobException;
import com.aditya.jobtracker.exception.InvalidStatusChangeException;
import com.aditya.jobtracker.exception.JobNotFoundException;
import com.aditya.jobtracker.model.AppUser;
import com.aditya.jobtracker.model.Application;
import com.aditya.jobtracker.model.ApplicationNote;
import com.aditya.jobtracker.model.ApplicationStatus;
import com.aditya.jobtracker.model.Job;
import com.aditya.jobtracker.model.Skill;
import com.aditya.jobtracker.repository.AppUserRepository;
import com.aditya.jobtracker.repository.ApplicationNoteRepository;
import com.aditya.jobtracker.repository.ApplicationRepository;
import com.aditya.jobtracker.repository.CompanyRepository;
import com.aditya.jobtracker.repository.JobRepository;
import com.aditya.jobtracker.repository.SkillRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// This replaces the old JobTracker + JobRepository classes.
// @Service = "Spring, please create ONE object of this class and manage it" (a bean).
//
// Big change from Stage 5: there are no in-memory lists any more.
// The database is the only truth, so the "id 0" problem cannot happen again.
//
// @Transactional = every public method runs inside one transaction.
// Spring starts it, commits at the end, and rolls back if something goes wrong.
// rollbackFor = Exception.class is needed because our own exceptions are CHECKED.
// By default Spring rolls back only for unchecked exceptions.
@Service
@Transactional(rollbackFor = Exception.class)
public class JobTrackerService {

    private final JobRepository jobRepo;
    private final CompanyRepository companyRepo;
    private final SkillRepository skillRepo;
    private final ApplicationRepository appRepo;
    private final ApplicationNoteRepository noteRepo;
    private final AppUserRepository userRepo;
    private final EntityManager em;

    // DEPENDENCY INJECTION (constructor injection):
    // We never write "new JobRepository()". Spring sees what this constructor needs,
    // creates those objects, and passes them in. That is IoC (Inversion of Control):
    // Spring controls object creation, not our code.
    public JobTrackerService(JobRepository jobRepo, CompanyRepository companyRepo,
                             SkillRepository skillRepo, ApplicationRepository appRepo,
                             ApplicationNoteRepository noteRepo, AppUserRepository userRepo,
                             EntityManager em) {
        this.jobRepo = jobRepo;
        this.companyRepo = companyRepo;
        this.skillRepo = skillRepo;
        this.appRepo = appRepo;
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.em = em;
    }

    // Every method below gets the username of the logged-in person.
    // The controller takes it from Spring Security, never from the page, so a user can't pretend to be someone else.

    // Finds the user row for a username. Used when we save a job, to set its owner.
    private AppUser findUser(String username) {
        return userRepo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    // The ONE place that checks ownership. If the job doesn't exist OR belongs to someone else,
    // we throw the same JobNotFoundException (404). So nobody can find out which ids other users have.
    private Job findOwnedJob(int id, String username) throws JobNotFoundException {
        return jobRepo.findByIdAndOwnerUsernameIgnoreCase(id, username)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    // readOnly = true: we only read, so no changes are saved. A bit faster.
    @Transactional(readOnly = true)
    public List<Job> getJobsOf(String username) {
        return jobRepo.findByOwnerUsernameIgnoreCaseOrderByIdAsc(username);
    }

    @Transactional(readOnly = true)
    public Job findJob(int id, String username) throws JobNotFoundException {
        return findOwnedJob(id, username);
    }

    public Job addJob(Job job, String username) throws DuplicateJobException {
        return saveForOwner(job, findUser(username));
    }

    // The real saving. It is private: only this class uses it.
    private Job saveForOwner(Job job, AppUser owner) throws DuplicateJobException {
        boolean exists = jobRepo.existsByOwnerUsernameIgnoreCaseAndTitleIgnoreCaseAndEmployerNameIgnoreCaseAndLocationIgnoreCase(
                owner.getUsername(), job.getTitle(), job.getCompany(), job.getLocation());
        if (exists) {
            throw new DuplicateJobException(
                    "You already saved " + job.getTitle() + " at " + job.getCompany() + ".");
        }
        // If the company already exists, reuse it instead of creating a second one.
        companyRepo.findByNameIgnoreCase(job.getCompany()).ifPresent(job::setEmployer);
        job.setOwner(owner);
        return jobRepo.save(job);       // INSERT. The returned job has its database id.
    }

    // Used for jobs fetched from Adzuna. The jobs belong to the user who fetched them
    // (or the admin who started the watcher).
    // Saves only the new ones and returns how many were new.
    // A job that already exists is skipped, not treated as an error.
    public int addFetchedJobs(List<Job> fetched, String username) {
        AppUser owner = findUser(username);      // look the user up once, not once per job
        int added = 0;
        for (Job job : fetched) {
            try {
                saveForOwner(job, owner);
                added++;
            } catch (DuplicateJobException e) {
                // already saved, so we skip it
            }
        }
        return added;
    }

    public Application applyTo(int jobId, String username) throws JobNotFoundException, DuplicateJobException {
        Job job = findOwnedJob(jobId, username);
        if (appRepo.findByJobId(jobId).isPresent()) {
            throw new DuplicateJobException("You already applied to this job.");
        }
        return appRepo.save(new Application(job));
    }

    public Application updateStatus(int jobId, ApplicationStatus next, String username)
            throws JobNotFoundException, InvalidStatusChangeException {
        findOwnedJob(jobId, username);
        Application app = appRepo.findByJobId(jobId)
                .orElseThrow(() -> new InvalidStatusChangeException("You haven't applied to this job yet."));
        app.changeStatus(next);     // checks the rules first
        // No save() call! The app is "persistent" inside this transaction,
        // so Hibernate notices the change and runs UPDATE at commit (dirty checking).
        return app;
    }

    // Only ADMIN can reach this (see SecurityConfig). It is a moderator action,
    // so it works on ANY job, not only the admin's own jobs.
    public void deleteJob(int id) throws JobNotFoundException {
        Job job = jobRepo.findById(id).orElseThrow(() -> new JobNotFoundException(id));
        jobRepo.delete(job);        // cascade also removes its application and notes
    }

    public Job addSkill(int jobId, String skillName, String username) throws JobNotFoundException {
        Job job = findOwnedJob(jobId, username);
        String name = skillName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Skill name can't be empty.");
        }
        // reuse the skill if it exists, otherwise create it
        // (skills are shared between users: "Java" is the same skill for everyone)
        Skill skill = skillRepo.findByNameIgnoreCase(name)
                .orElseGet(() -> skillRepo.save(new Skill(name)));
        // keep both sides of the many-to-many in sync
        job.getSkills().add(skill);
        skill.getJobs().add(job);
        return job;
    }

    @Transactional(readOnly = true)
    public List<Job> findJobsBySkill(String skillName, String username) {
        return jobRepo.findBySkillNameAndOwner(skillName.trim(), username);
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsOf(String username) {
        return appRepo.findByJobOwnerUsernameIgnoreCase(username);
    }

    public ApplicationNote addNote(int jobId, String text, String username) throws JobNotFoundException {
        findOwnedJob(jobId, username);
        Application app = appRepo.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("You haven't applied to this job yet."));
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("A note can't be empty.");
        }
        ApplicationNote note = new ApplicationNote(app, text.trim());
        // composite key = application id + today's date, so one note per day
        if (noteRepo.existsById(note.getId())) {
            throw new IllegalArgumentException("You already wrote a note for this job today.");
        }
        // We use persist() on purpose. save() would call merge() here, because our key is
        // already filled in. persist() is the simple, direct INSERT.
        em.persist(note);
        return note;
    }

    @Transactional(readOnly = true)
    public List<ApplicationNote> listNotes(int jobId, String username) throws JobNotFoundException {
        findOwnedJob(jobId, username);
        Application app = appRepo.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("You haven't applied to this job yet."));
        return noteRepo.findNotes(app.getId());
    }
}
