package com.aditya.jobtracker.repository;

import com.aditya.jobtracker.model.Company;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

    // Optional = "the answer may be empty". It avoids returning null.
    Optional<Company> findByNameIgnoreCase(String name);
}
