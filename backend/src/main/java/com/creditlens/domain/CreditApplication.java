package com.creditlens.domain;

import com.creditlens.decision.ApplicantProfile;
import com.creditlens.decision.DecisionResult;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * JPA entity mapped to table credit_application (created by Flyway V1__init.sql).
 *
 * Design choice: no public setters. State changes only through intention-revealing methods
 * (applyAutomatedDecision, review) that enforce the business rules. This is called a
 * "rich domain model" and it makes illegal states hard to reach.
 */
@Entity
@Table(name = "credit_application")
public class CreditApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 20)
    private String referenceNumber;

    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "annual_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "monthly_debt_payments", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyDebtPayments;

    @Column(name = "credit_score", nullable = false)
    private int creditScore;

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "dti_ratio", nullable = false, precision = 6, scale = 4)
    private BigDecimal dtiRatio;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal apr;

    @Column(name = "monthly_payment", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_reason_code", joinColumns = @JoinColumn(name = "application_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 10)
    @BatchSize(size = 50) // loads reason codes for up to 50 applications in ONE query (avoids N+1)
    private List<ReasonCode> reasonCodes = new ArrayList<>();

    @Column(name = "decided_by", nullable = false, length = 100)
    private String decidedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: two underwriters saving the same row at once -> the second one fails. */
    @Version
    private long version;

    protected CreditApplication() {
        // required by JPA
    }

    public static CreditApplication submit(ApplicantProfile profile, String applicantName, String email) {
        CreditApplication app = new CreditApplication();
        app.referenceNumber = newReferenceNumber();
        app.applicantName = applicantName.trim();
        app.email = email.trim().toLowerCase(Locale.ROOT);
        app.annualIncome = profile.annualIncome();
        app.monthlyDebtPayments = profile.monthlyDebtPayments();
        app.creditScore = profile.creditScore();
        app.requestedAmount = profile.requestedAmount();
        app.termMonths = profile.termMonths();
        app.employmentStatus = profile.employmentStatus();
        app.productType = profile.productType();
        app.createdAt = Instant.now();
        app.updatedAt = app.createdAt;
        return app;
    }

    public void applyAutomatedDecision(DecisionResult result, String engineName) {
        this.status = result.outcome();
        this.riskScore = result.riskScore();
        this.dtiRatio = result.debtToIncome();
        this.apr = result.apr();
        this.monthlyPayment = result.estimatedMonthlyPayment();
        this.approvedAmount = result.approvedAmount();
        this.reasonCodes.clear();
        this.reasonCodes.addAll(result.reasons());
        this.decidedBy = engineName;
    }

    /**
     * Underwriter decision. Only REFERRED applications can be reviewed: the engine's
     * APPROVED/DECLINED outcomes are final in this system.
     */
    public ApplicationStatus review(boolean approve, String underwriter) {
        if (status != ApplicationStatus.REFERRED) {
            throw new IllegalStateException(
                    "Only REFERRED applications can be reviewed; " + referenceNumber + " is " + status);
        }
        ApplicationStatus previous = status;
        this.status = approve ? ApplicationStatus.APPROVED : ApplicationStatus.DECLINED;
        this.approvedAmount = approve ? requestedAmount : null;
        this.decidedBy = underwriter.trim();
        return previous;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    private static String newReferenceNumber() {
        return "CL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getApplicantName() { return applicantName; }
    public String getEmail() { return email; }
    public BigDecimal getAnnualIncome() { return annualIncome; }
    public BigDecimal getMonthlyDebtPayments() { return monthlyDebtPayments; }
    public int getCreditScore() { return creditScore; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public int getTermMonths() { return termMonths; }
    public EmploymentStatus getEmploymentStatus() { return employmentStatus; }
    public ProductType getProductType() { return productType; }
    public ApplicationStatus getStatus() { return status; }
    public int getRiskScore() { return riskScore; }
    public BigDecimal getDtiRatio() { return dtiRatio; }
    public BigDecimal getApr() { return apr; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public List<ReasonCode> getReasonCodes() { return List.copyOf(reasonCodes); }
    public String getDecidedBy() { return decidedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
