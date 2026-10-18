package com.aditya.jobtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDate;

// A short diary note about an application, for example "HR said they will call on Monday".
@Entity
@Table(name = "application_notes")
public class ApplicationNote {

    // @EmbeddedId = the primary key is the two-column ApplicationNoteId
    @EmbeddedId
    private ApplicationNoteId id;

    // MANY-TO-ONE: many notes belong to one application.
    // @MapsId says: the application_id column is BOTH the foreign key and part of the primary key.
    @ManyToOne
    @MapsId("applicationId")
    @JoinColumn(name = "application_id")
    private Application application;

    @Column(name = "note_text", nullable = false, length = 1000)
    private String text;

    protected ApplicationNote() {
    }

    public ApplicationNote(Application application, String text) {
        this.application = application;
        this.text = text;
        this.id = new ApplicationNoteId(application.getId(), LocalDate.now());
    }

    public ApplicationNoteId getId() { return id; }
    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }
    public String getText() { return text; }

    @Override
    public String toString() {
        return id.getNoteDate() + ": " + text;
    }
}
