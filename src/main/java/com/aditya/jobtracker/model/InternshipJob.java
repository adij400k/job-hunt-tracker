package com.aditya.jobtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("INTERNSHIP")
public class InternshipJob extends Job {

    @Column(name = "duration_months")
    private Integer durationMonths;

    protected InternshipJob() {
    }

    public InternshipJob(String title, String company, String location,
                         Double stipendPerYear, int durationMonths) {
        super(title, company, location, stipendPerYear);
        this.durationMonths = durationMonths;
    }

    @Override
    public String getJobType() {
        return "INTERNSHIP";
    }

    @Override
    protected String getExtraDetails() {
        int months = (durationMonths == null) ? 0 : durationMonths;
        return months <= 0 ? "duration not listed" : months + " months";
    }
}
