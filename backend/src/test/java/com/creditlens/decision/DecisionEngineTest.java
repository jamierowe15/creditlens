package com.creditlens.decision;

import com.creditlens.config.DecisionConfig;
import com.creditlens.domain.ApplicationStatus;
import com.creditlens.domain.EmploymentStatus;
import com.creditlens.domain.ProductType;
import com.creditlens.domain.ReasonCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests: no Spring context, runs in milliseconds. */
class DecisionEngineTest {

    private final DecisionEngine engine = new DecisionConfig().decisionEngine();

    private static ApplicantProfile profile(String income, String debt, int score, String amount, int term,
                                            EmploymentStatus emp) {
        return new ApplicantProfile(new BigDecimal(income), new BigDecimal(debt), score, new BigDecimal(amount),
                term, emp, ProductType.PERSONAL_LOAN);
    }

    @Test
    @DisplayName("Strong applicant is approved for the full amount with no reasons")
    void strongApplicantApproved() {
        DecisionResult r = engine.decide(profile("92000", "450", 781, "15000", 36, EmploymentStatus.FULL_TIME));

        assertThat(r.outcome()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(r.approvedAmount()).isEqualByComparingTo("15000");
        assertThat(r.reasons()).isEmpty();
        assertThat(r.apr()).isEqualByComparingTo("7.49");
        assertThat(r.riskScore()).isBetween(0, 30);
    }

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({
            "599, DECLINED, CS01",
            "600, REFERRED, CS02",
            "659, REFERRED, CS02",
    })
    void creditScoreBands(int score, ApplicationStatus expected, ReasonCode code) {
        DecisionResult r = engine.decide(profile("90000", "0", score, "10000", 36, EmploymentStatus.FULL_TIME));

        assertThat(r.outcome()).isEqualTo(expected);
        assertThat(r.reasons()).containsExactly(code);
        assertThat(r.approvedAmount()).isNull();
    }

    @Test
    void scoreOf660PassesTheScoreRule() {
        DecisionResult r = engine.decide(profile("90000", "0", 660, "10000", 36, EmploymentStatus.FULL_TIME));
        assertThat(r.outcome()).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    @DisplayName("DTI includes the new loan payment, not only existing debt")
    void dtiIncludesNewPayment() {
        // 60,000/yr = 5,000/mo. Existing debt 1,900 (38%) plus a new payment pushes DTI over 40%.
        DecisionResult r = engine.decide(profile("60000", "1900", 720, "10000", 36, EmploymentStatus.FULL_TIME));

        assertThat(r.debtToIncome()).isGreaterThan(new BigDecimal("0.40"));
        assertThat(r.reasons()).contains(ReasonCode.DTI2);
        assertThat(r.outcome()).isEqualTo(ApplicationStatus.REFERRED);
    }

    @Test
    @DisplayName("Decline beats refer, and ALL reasons are reported, most serious first")
    void declineWinsAndAllReasonsListed() {
        DecisionResult r = engine.decide(profile("30000", "1500", 610, "35000", 60, EmploymentStatus.UNEMPLOYED));

        assertThat(r.outcome()).isEqualTo(ApplicationStatus.DECLINED);
        assertThat(r.reasons()).contains(ReasonCode.EMP1, ReasonCode.CS02, ReasonCode.DTI1, ReasonCode.LTI1);
        assertThat(r.reasons().get(0).severity()).isEqualTo(com.creditlens.domain.Severity.DECLINE);
    }

    @Test
    void zeroIncomeDoesNotDivideByZero() {
        DecisionResult r = engine.decide(profile("0", "0", 700, "5000", 12, EmploymentStatus.UNEMPLOYED));

        assertThat(r.outcome()).isEqualTo(ApplicationStatus.DECLINED);
        assertThat(r.reasons()).contains(ReasonCode.INC1);
        assertThat(r.debtToIncome()).isEqualByComparingTo("9.9999");
    }

    @Test
    void selfEmployedIsReferredForVerification() {
        DecisionResult r = engine.decide(profile("80000", "200", 750, "8000", 24, EmploymentStatus.SELF_EMPLOYED));

        assertThat(r.outcome()).isEqualTo(ApplicationStatus.REFERRED);
        assertThat(r.reasons()).containsExactly(ReasonCode.EMP2);
    }

    @Test
    void engineRequiresRules() {
        assertThatThrownBy(() -> new DecisionEngine(java.util.List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
