package com.creditlens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Append-only audit trail: who decided what, when, under which rules version.
 * Regulators and internal audit ask "why was this person declined?" years later.
 * Rows are only ever inserted, never updated or deleted.
 */
@Entity
@Table(name = "decision_audit")
public class DecisionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ApplicationStatus toStatus;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(length = 500)
    private String note;

    @Column(name = "rules_version", length = 20)
    private String rulesVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DecisionAudit() {
    }

    public static DecisionAudit record(Long applicationId, AuditAction action, ApplicationStatus from,
                                       ApplicationStatus to, String actor, String note, String rulesVersion) {
        DecisionAudit a = new DecisionAudit();
        a.applicationId = applicationId;
        a.action = action;
        a.fromStatus = from;
        a.toStatus = to;
        a.actor = actor;
        a.note = note;
        a.rulesVersion = rulesVersion;
        a.createdAt = Instant.now();
        return a;
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public AuditAction getAction() { return action; }
    public ApplicationStatus getFromStatus() { return fromStatus; }
    public ApplicationStatus getToStatus() { return toStatus; }
    public String getActor() { return actor; }
    public String getNote() { return note; }
    public String getRulesVersion() { return rulesVersion; }
    public Instant getCreatedAt() { return createdAt; }
}
