package com.aditya.jobtracker.dto;

import com.aditya.jobtracker.model.Application;

public record ApplicationResponse(int id, int jobId, String jobTitle, String company,
                                  String status, String appliedDate, long daysSinceApplied) {

    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(a.getId(), a.getJob().getId(), a.getJob().getTitle(),
                a.getJob().getCompany(), a.getStatus().name(), a.getAppliedDate().toString(),
                a.getDaysSinceApplied());
    }
}
