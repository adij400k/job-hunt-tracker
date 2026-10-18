package com.aditya.jobtracker.dto;

import com.aditya.jobtracker.model.ApplicationNote;

public record NoteResponse(String date, String text) {

    public static NoteResponse from(ApplicationNote n) {
        return new NoteResponse(n.getId().getNoteDate().toString(), n.getText());
    }
}
