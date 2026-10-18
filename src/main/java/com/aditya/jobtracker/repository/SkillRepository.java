package com.aditya.jobtracker.repository;

import com.aditya.jobtracker.model.Skill;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Integer> {

    Optional<Skill> findByNameIgnoreCase(String name);
}
