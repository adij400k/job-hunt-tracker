package com.aditya.jobtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

// Rows of this type have job_type = 'FULL-TIME' in the jobs table.
@Entity
@DiscriminatorValue("FULL-TIME")
public class FullTimeJob extends Job {

    // Integer (not int): in a single shared table, internship rows leave this column empty (NULL).
    @Column(name = "experience_years")
    private Integer experienceYears;

    protected FullTimeJob() {
    }

    public FullTimeJob(String title, String company, String location,
                       Double salary, int experienceYears) {
        super(title, company, location, salary);
        this.experienceYears = experienceYears;
    }

    @Override
    public String getJobType() {
        return "FULL-TIME";
    }

    @Override
    protected String getExtraDetails() {
        int years = (experienceYears == null) ? -1 : experienceYears;
        if (years < 0) return "experience not listed";
        return years == 0 ? "freshers can apply" : years + " years experience";
    }
}
