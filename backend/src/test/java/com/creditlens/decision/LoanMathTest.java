package com.creditlens.decision;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanMathTest {

    @Test
    void amortizedPaymentMatchesKnownValue() {
        // $10,000 at 12% APR for 12 months = $888.49 (standard amortization table value)
        assertThat(LoanMath.monthlyPayment(new BigDecimal("10000"), new BigDecimal("12"), 12))
                .isEqualByComparingTo("888.49");
    }

    @Test
    void zeroRateIsSimpleDivision() {
        assertThat(LoanMath.monthlyPayment(new BigDecimal("1200"), BigDecimal.ZERO, 12))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void ratioIsCappedAndSafeOnZero() {
        BigDecimal cap = new BigDecimal("9.9999");
        assertThat(LoanMath.ratio(BigDecimal.ONE, BigDecimal.ZERO, cap)).isEqualByComparingTo(cap);
        assertThat(LoanMath.ratio(new BigDecimal("1"), new BigDecimal("4"), cap)).isEqualByComparingTo("0.25");
    }

    @Test
    void rejectsNonPositiveTerm() {
        assertThatThrownBy(() -> LoanMath.monthlyPayment(BigDecimal.TEN, BigDecimal.ONE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
