package com.creditlens.service;

import com.creditlens.decision.ApplicantProfile;
import com.creditlens.decision.DecisionEngine;
import com.creditlens.decision.DecisionResult;
import com.creditlens.domain.ApplicationStatus;
import com.creditlens.domain.AuditAction;
import com.creditlens.domain.CreditApplication;
import com.creditlens.domain.DecisionAudit;
import com.creditlens.repository.CreditApplicationRepository;
import com.creditlens.repository.DecisionAuditRepository;
import com.creditlens.util.Masking;
import com.creditlens.web.dto.CreateApplicationRequest;
import com.creditlens.web.dto.ReviewRequest;
import com.creditlens.web.dto.StatsResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business workflow layer. Controllers stay thin (HTTP only); this class owns the use cases.
 * @Transactional: the application row AND its audit row commit together or not at all.
 */
@Service
@Transactional
public class CreditApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CreditApplicationService.class);

    private final CreditApplicationRepository applications;
    private final DecisionAuditRepository audits;
    private final DecisionEngine engine;
    private final MeterRegistry meters;
    private final Timer decisionTimer;

    // Constructor injection: dependencies are explicit, final, and easy to replace in tests.
    public CreditApplicationService(CreditApplicationRepository applications,
                                    DecisionAuditRepository audits,
                                    DecisionEngine engine,
                                    MeterRegistry meters) {
        this.applications = applications;
        this.audits = audits;
        this.engine = engine;
        this.meters = meters;
        this.decisionTimer = Timer.builder("creditlens.decision.latency")
                .description("Time spent in the rules engine")
                .register(meters);
    }

    public CreditApplication submit(CreateApplicationRequest request) {
        ApplicantProfile profile = request.toProfile();
        Timer.Sample sample = Timer.start(meters);
        DecisionResult result = engine.decide(profile);
        sample.stop(decisionTimer);

        CreditApplication app = CreditApplication.submit(profile, request.applicantName(), request.email());
        app.applyAutomatedDecision(result, DecisionEngine.ACTOR);
        applications.save(app);

        audits.save(DecisionAudit.record(app.getId(), AuditAction.AUTO_DECISION, null, app.getStatus(),
                DecisionEngine.ACTOR, summarize(result), result.rulesVersion()));
        countDecision(app.getStatus(), "auto");

        // Note what is logged: reference, outcome, risk. Email is masked; name and income are not logged.
        log.info("Decisioned application ref={} outcome={} risk={} email={}",
                app.getReferenceNumber(), app.getStatus(), app.getRiskScore(), Masking.email(app.getEmail()));
        return app;
    }

    @Transactional(readOnly = true)
    public DecisionResult preview(ApplicantProfile profile) {
        return engine.decide(profile);
    }

    @Transactional(readOnly = true)
    public Page<CreditApplication> list(ApplicationStatus status, Pageable pageable) {
        return status == null ? applications.findAll(pageable) : applications.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public CreditApplication get(Long id) {
        return applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<DecisionAudit> auditTrail(Long id) {
        if (!applications.existsById(id)) {
            throw new ApplicationNotFoundException(id);
        }
        return audits.findByApplicationIdOrderByIdAsc(id);
    }

    public CreditApplication review(Long id, ReviewRequest request) {
        CreditApplication app = get(id);
        ApplicationStatus previous;
        try {
            previous = app.review(request.approve(), request.underwriter());
        } catch (IllegalStateException e) {
            throw new InvalidStateTransitionException(e.getMessage());
        }
        audits.save(DecisionAudit.record(app.getId(), AuditAction.MANUAL_REVIEW, previous, app.getStatus(),
                request.underwriter().trim(), request.note(), DecisionEngine.RULES_VERSION));
        countDecision(app.getStatus(), "manual");
        log.info("Manual review ref={} {} -> {} by={}", app.getReferenceNumber(), previous, app.getStatus(),
                request.underwriter());
        return app;
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        long approved = applications.countByStatus(ApplicationStatus.APPROVED);
        long referred = applications.countByStatus(ApplicationStatus.REFERRED);
        long declined = applications.countByStatus(ApplicationStatus.DECLINED);
        long total = approved + referred + declined;
        double approvalRate = total == 0 ? 0 : Math.round(approved * 1000.0 / total) / 10.0;
        Double avg = applications.averageCreditScore();
        double avgScore = avg == null ? 0 : Math.round(avg * 10) / 10.0;
        return new StatsResponse(total, approved, referred, declined, approvalRate, avgScore);
    }

    private void countDecision(ApplicationStatus outcome, String source) {
        Counter.builder("creditlens.decisions")
                .description("Credit decisions made")
                .tag("outcome", outcome.name())
                .tag("source", source)
                .register(meters)
                .increment();
    }

    private static String summarize(DecisionResult r) {
        if (r.reasons().isEmpty()) {
            return "All rules passed";
        }
        return "Reasons: " + String.join(", ", r.reasons().stream().map(Enum::name).toList());
    }
}
