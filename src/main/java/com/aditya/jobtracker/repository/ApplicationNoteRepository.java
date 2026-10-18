package com.aditya.jobtracker.repository;

import com.aditya.jobtracker.model.ApplicationNote;
import com.aditya.jobtracker.model.ApplicationNoteId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// The id type here is our composite key class, not Integer.
public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, ApplicationNoteId> {

    @Query("select n from ApplicationNote n where n.id.applicationId = :a order by n.id.noteDate")
    List<ApplicationNote> findNotes(@Param("a") int applicationId);
}
