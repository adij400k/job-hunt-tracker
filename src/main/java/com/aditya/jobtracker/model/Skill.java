package com.aditya.jobtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    // MANY-TO-MANY (the other side): a skill can be needed by many jobs.
    // The join table is defined on the Job side, so here we only say mappedBy.
    @ManyToMany(mappedBy = "skills")
    private Set<Job> jobs = new HashSet<>();

    protected Skill() {
    }

    public Skill(String name) {
        this.name = name.trim();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name.trim(); }
    public Set<Job> getJobs() { return jobs; }

    // Two skills are the same if the names match, ignoring capital letters.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Skill)) return false;
        return name.equalsIgnoreCase(((Skill) o).name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }
}
