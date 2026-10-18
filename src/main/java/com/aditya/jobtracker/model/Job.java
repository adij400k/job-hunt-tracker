package com.aditya.jobtracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Abstract class AND a database entity.
// SINGLE_TABLE inheritance: FullTimeJob and InternshipJob are stored in the SAME
// table (jobs). The job_type column tells Hibernate which kind each row is.
@Entity
@Table(name = "jobs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "job_type", length = 20)
public abstract class Job implements Displayable {

    // The database creates the id now, so the old static counter is gone.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    // MANY-TO-ONE: many jobs belong to one company.
    // This side owns the link, so the foreign key column company_id is in the jobs table.
    // CascadeType.PERSIST: saving a new job also saves its new company.
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "company_id", nullable = false)
    private Company employer;

    @Column(nullable = false)
    private String location;

    // OWNER: each job belongs to one user, so users only see their own jobs.
    // MANY-TO-ONE again: many jobs, one user. The column owner_id is a foreign key to app_users.
    // LAZY = don't load the user together with the job (we only need the user's id in queries).
    // It may be NULL for jobs saved before this change (see the SQL line in the instructions).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    private Double salary;     // Double, so it can be null when unknown

    @Column(name = "apply_link", length = 2000)
    private String applyLink = "";

    // ONE-TO-ONE (bidirectional): a job has at most one application.
    // The Application class owns the link. cascade ALL + orphanRemoval means:
    // delete the job and its application is deleted with it.
    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private Application application;

    // MANY-TO-MANY: a job needs many skills, a skill is needed by many jobs.
    // MySQL stores this in a third table, job_skills, with two foreign keys.
    // EAGER = load the skills together with the job (we need them after the session closes).
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new HashSet<>();

    // Hibernate needs this empty constructor. protected = only subclasses and Hibernate can use it.
    protected Job() {
    }

    public Job(String title, String company, String location, Double salary) {
        // Unchecked exception: a blank title is a programming mistake.
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Job title can't be empty.");
        }
        this.title = title.trim();
        this.employer = new Company(company.trim());
        this.location = location.trim();
        this.salary = salary;
    }

    public abstract String getJobType();

    protected abstract String getExtraDetails();

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return employer.getName(); }
    public String getLocation() { return location; }
    public Double getSalary() { return salary; }
    public String getApplyLink() { return applyLink; }
    public void setApplyLink(String applyLink) { this.applyLink = applyLink; }
    public Company getEmployer() { return employer; }
    public void setEmployer(Company employer) { this.employer = employer; }
    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }
    public Set<Skill> getSkills() { return skills; }
    public AppUser getOwner() { return owner; }
    public void setOwner(AppUser owner) { this.owner = owner; }

    @Override
    public String getSummary() {
        String pay = (salary == null) ? "salary not listed" : "Rs." + salary + " per year";
        String skillText = skills.isEmpty() ? "" : " | skills: " + skills.stream()
                .map(Skill::getName)
                .sorted()
                .collect(Collectors.joining(", "));

        return "#" + id + " [" + getJobType() + "] " + title + " at " + getCompany()
                + ", " + location + " | " + pay + " | " + getExtraDetails() + skillText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job)) return false;
        Job other = (Job) o;
        return title.equalsIgnoreCase(other.title)
                && getCompany().equalsIgnoreCase(other.getCompany())
                && location.equalsIgnoreCase(other.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title.toLowerCase(), getCompany().toLowerCase(), location.toLowerCase());
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
