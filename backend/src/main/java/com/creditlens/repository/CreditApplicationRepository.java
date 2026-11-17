package com.creditlens.repository;

import com.creditlens.domain.ApplicationStatus;
import com.creditlens.domain.CreditApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data writes the SQL from the method names at startup. Every query is parameterized
 * (bind variables), so user input can never be executed as SQL: no SQL injection.
 */
public interface CreditApplicationRepository extends JpaRepository<CreditApplication, Long> {

    Page<CreditApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    long countByStatus(ApplicationStatus status);

    @Query("select avg(a.creditScore) from CreditApplication a")
    Double averageCreditScore(); // null when the table is empty
}
