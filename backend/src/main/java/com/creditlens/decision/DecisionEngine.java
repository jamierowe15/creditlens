package com.creditlens.decision;

import com.creditlens.domain.ApplicationStatus;
import com.creditlens.domain.ReasonCode;
import com.creditlens.domain.Severity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The heart of CreditLens. Pure Java with no Spring, no database, no HTTP, which is why it is
 * fast and easy to unit test. Spring creates it as a bean in DecisionConfig.
 *
 * Flow: derive numbers -> run every rule -> worst severity wins -> score risk -> build result.
 */
public class DecisionEngine {

    public static final String RULES_VERSION = "2026.11-v1";
    public static final String ACTOR = "CreditLens Engine " + RULES_VERSION;

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal DTI_CAP = new BigDecimal("9.9999");

    private final List<CreditRule> rules;

    public DecisionEngine(List<CreditRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("At least one credit rule is required");
        }
        this.rules = List.copyOf(rules);
    }

    public DecisionResult decide(ApplicantProfile profile) {
        DecisionContext ctx = buildContext(profile);

        // Run every rule (no short-circuit) so the applicant gets ALL reasons, not just the first.
        List<ReasonCode> reasons = new ArrayList<>();
        Severity worst = Severity.PASS;
        for (CreditRule rule : rules) {
            RuleResult result = rule.evaluate(ctx);
            if (!result.passed()) {
                reasons.add(result.reasonCode());
                if (result.severity().compareTo(worst) > 0) {
                    worst = result.severity();
                }
            }
        }
        // Most serious reasons first, so DECLINE reasons are listed before REFER reasons.
        reasons.sort(Comparator.comparing((ReasonCode r) -> r.severity()).reversed());

        ApplicationStatus outcome = switch (worst) {
            case DECLINE -> ApplicationStatus.DECLINED;
            case REFER -> ApplicationStatus.REFERRED;
            case PASS -> ApplicationStatus.APPROVED;
        };

        BigDecimal approvedAmount = outcome == ApplicationStatus.APPROVED ? profile.requestedAmount() : null;

        return new DecisionResult(
                outcome,
                riskScore(ctx),
                ctx.debtToIncome(),
                ctx.apr(),
                ctx.estimatedMonthlyPayment(),
                approvedAmount,
                reasons,
                RULES_VERSION);
    }

    DecisionContext buildContext(ApplicantProfile p) {
        BigDecimal monthlyIncome = p.annualIncome().divide(TWELVE, 2, RoundingMode.HALF_UP);
        BigDecimal apr = PricingPolicy.aprFor(p.creditScore(), p.productType());
        BigDecimal payment = LoanMath.monthlyPayment(p.requestedAmount(), apr, p.termMonths());
        // DTI includes the NEW payment: can the applicant afford existing debt + this loan?
        BigDecimal dti = LoanMath.ratio(p.monthlyDebtPayments().add(payment), monthlyIncome, DTI_CAP);
        BigDecimal lti = LoanMath.ratio(p.requestedAmount(), p.annualIncome(), DTI_CAP);
        return new DecisionContext(p, monthlyIncome, apr, payment, dti, lti);
    }

    /**
     * 0 (lowest risk) to 100 (highest risk). Weighted blend of score, DTI and loan-to-income.
     * Displayed to underwriters; the approve/refer/decline outcome comes from the rules.
     */
    static int riskScore(DecisionContext ctx) {
        double scorePart = clamp((900 - ctx.profile().creditScore()) / 6.0);   // 900 -> 0, 300 -> 100
        double dtiPart = clamp(ctx.debtToIncome().doubleValue() * 200);        // 50% DTI -> 100
        double ltiPart = clamp(ctx.loanToIncome().doubleValue() * 100);        // 100% LTI -> 100
        return (int) Math.round(scorePart * 0.50 + dtiPart * 0.35 + ltiPart * 0.15);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
