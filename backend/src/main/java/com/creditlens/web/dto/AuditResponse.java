package com.creditlens.web.dto;

import com.creditlens.domain.ApplicationStatus;
import com.creditlens.domain.AuditAction;
import com.creditlens.domain.DecisionAudit;

import java.time.Instant;

public record AuditResponse(
        Long id,
        AuditAction action,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String actor,
        String note,
        String rulesVersion,
        Instant createdAt) {

    public static AuditResponse from(DecisionAudit a) {
        return new AuditResponse(a.getId(), a.getAction(), a.getFromStatus(), a.getToStatus(),
                a.getActor(), a.getNote(), a.getRulesVersion(), a.getCreatedAt());
    }
}
