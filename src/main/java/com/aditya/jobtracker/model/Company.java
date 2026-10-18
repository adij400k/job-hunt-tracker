package com.aditya.jobtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

// @Entity = "this class is a database table".
// Each Company object is one row in the companies table.
@Entity
@Table(name = "companies")
public class Company {

    // @Id marks the primary key. IDENTITY means MySQL generates the number
    // itself (AUTO_INCREMENT), so we never set the id by hand.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    // ONE-TO-MANY: one company has many jobs.
    // mappedBy = "employer" means the Job class owns the link (the foreign key
    // column lives in the jobs table), and this list is just the other side.
    @OneToMany(mappedBy = "employer")
    private List<Job> jobs = new ArrayList<>();

    // Hibernate needs an empty constructor to create objects from database rows.
    protected Company() {
    }

    public Company(String name) {
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<Job> getJobs() { return jobs; }
}
