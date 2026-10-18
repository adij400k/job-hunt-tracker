package com.aditya.jobtracker.dto;

import com.aditya.jobtracker.model.Job;
import com.aditya.jobtracker.model.Skill;
import java.util.List;

// DTO = Data Transfer Object: a small class made only to be turned into JSON.
// We never send the Job entity itself, because Job <-> Application point at each
// other and the JSON would loop forever. A DTO has only plain values.
// "record" = a short way to write a class with only fields, a constructor and getters.
public record JobResponse(int id, String type, String title, String company, String location,
                          Double salary, String applyLink, List<String> skills, String status) {

    // Copies the values from the entity into the DTO
    public static JobResponse from(Job job) {
        List<String> skills = job.getSkills().stream().map(Skill::getName).sorted().toList();
        // status is null when you have not applied to this job yet
        String status = (job.getApplication() == null) ? null : job.getApplication().getStatus().name();
        return new JobResponse(job.getId(), job.getJobType(), job.getTitle(), job.getCompany(),
                job.getLocation(), job.getSalary(), job.getApplyLink(), skills, status);
    }
}
