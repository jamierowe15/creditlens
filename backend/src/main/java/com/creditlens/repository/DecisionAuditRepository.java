package com.creditlens.repository;

import com.creditlens.domain.DecisionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionAuditRepository extends JpaRepository<DecisionAudit, Long> {

    List<DecisionAudit> findByApplicationIdOrderByIdAsc(Long applicationId);
}
