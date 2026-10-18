package com.aditya.jobtracker.repository;

import com.aditya.jobtracker.model.Job;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// This is an INTERFACE. We write no class and no SQL for the basic work.
// Spring Data creates the class for us when the app starts.
// JpaRepository<Job, Integer> = "this repository handles Job objects, and the id type is Integer".
// It already gives us save(), findById(), findAll(), delete(), count() ... for free.
public interface JobRepository extends JpaRepository<Job, Integer> {

    // DERIVED QUERY: Spring reads the method NAME and builds the query from it.
    // "findBy" + "OwnerUsername" + "OrderById" + "Asc"
    //   -> select ... from jobs where owner's username = ? order by id asc
    // OwnerUsername means: job.owner.username. IgnoreCase = ignore capital letters.
    List<Job> findByOwnerUsernameIgnoreCaseOrderByIdAsc(String username);

    // One job by its id, but ONLY if it belongs to this user.
    // If the job belongs to someone else, this returns "empty", just like a missing job.
    Optional<Job> findByIdAndOwnerUsernameIgnoreCase(int id, String username);

    // Duplicate check, now PER USER: two users may both save the same job.
    // EmployerName means: job.employer.name (the company's name).
    boolean existsByOwnerUsernameIgnoreCaseAndTitleIgnoreCaseAndEmployerNameIgnoreCaseAndLocationIgnoreCase(
            String username, String title, String companyName, String location);

    // When a name would get too long, we write the query ourselves with @Query (HQL, as in Stage 5).
    // Same skill search as before, but only this user's jobs.
    @Query("select j from Job j join j.skills s where lower(s.name) = lower(:name) "
            + "and lower(j.owner.username) = lower(:username) order by j.id")
    List<Job> findBySkillNameAndOwner(@Param("name") String name, @Param("username") String username);
}
