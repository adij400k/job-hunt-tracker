package com.aditya.jobtracker.dto;

// The JSON a client sends to add a job. Example:
// {"type":"FULL-TIME","title":"Java Developer","company":"Infosys","location":"Mumbai",
//  "salary":600000,"experienceYears":0}
// Integer and Double (not int, double) so a missing field simply becomes null.
public record CreateJobRequest(String type, String title, String company, String location,
                               Double salary, Integer experienceYears, Integer durationMonths,
                               String applyLink) {
}
