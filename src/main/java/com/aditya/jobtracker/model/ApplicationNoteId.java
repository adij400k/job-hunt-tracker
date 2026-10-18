package com.aditya.jobtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

// COMPOSITE KEY: the primary key of a note is TWO columns together,
// the application id and the date. So an application can have one note per day.
// @Embeddable = "this class is not a table, its fields are part of another table's key".
// A composite key class must be Serializable and must have equals() and hashCode().
@Embeddable
public class ApplicationNoteId implements Serializable {

    @Column(name = "application_id")
    private int applicationId;

    @Column(name = "note_date")
    private LocalDate noteDate;

    protected ApplicationNoteId() {
    }

    public ApplicationNoteId(int applicationId, LocalDate noteDate) {
        this.applicationId = applicationId;
        this.noteDate = noteDate;
    }

    public int getApplicationId() { return applicationId; }
    public LocalDate getNoteDate() { return noteDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationNoteId)) return false;
        ApplicationNoteId other = (ApplicationNoteId) o;
        return applicationId == other.applicationId && Objects.equals(noteDate, other.noteDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, noteDate);
    }
}
