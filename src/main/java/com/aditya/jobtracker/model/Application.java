package com.aditya.jobtracker.model;

import com.aditya.jobtracker.exception.InvalidStatusChangeException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // ONE-TO-ONE: this class OWNS the link, so job_id (the foreign key) is a column here.
    // unique = true makes sure one job can't have two applications.
    @OneToOne
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    // STRING = store the word "APPLIED", not the number 1. Safer if we reorder the enum later.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    // ONE-TO-MANY (bidirectional): one application has many notes.
    // CascadeType.REMOVE: deleting the application deletes its notes too,
    // otherwise MySQL would refuse because the notes still point at it.
    @OneToMany(mappedBy = "application", cascade = CascadeType.REMOVE)
    private List<ApplicationNote> notes = new ArrayList<>();

    protected Application() {
    }

    public Application(Job job) {
        this.job = job;
        this.status = ApplicationStatus.APPLIED;
        this.appliedDate = LocalDate.now();
    }

    public void changeStatus(ApplicationStatus next) throws InvalidStatusChangeException {
        if (!status.canMoveTo(next)) {
            throw new InvalidStatusChangeException(
                    "Can't change status from " + status + " to " + next + ".");
        }
        status = next;
    }

    public long getDaysSinceApplied() {
        return ChronoUnit.DAYS.between(appliedDate, LocalDate.now());
    }

    public int getId() { return id; }
    public Job getJob() { return job; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDate getAppliedDate() { return appliedDate; }

    @Override
    public String toString() {
        return job.getTitle() + " at " + job.getCompany() + " | " + status
                + " | applied " + getDaysSinceApplied() + " days ago";
    }
}
