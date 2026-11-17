package com.creditlens.decision;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Money math. Always BigDecimal, never double: 0.1 + 0.2 != 0.3 in floating point,
 * and a bank cannot lose fractions of a cent.
 */
public final class LoanMath {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private LoanMath() {
    }

    /**
     * Standard amortized payment: M = P * r / (1 - (1 + r)^-n)
     * P = principal, r = monthly rate (APR / 100 / 12), n = number of monthly payments.
     */
    public static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal aprPercent, int termMonths) {
        if (termMonths <= 0) {
            throw new IllegalArgumentException("termMonths must be positive");
        }
        if (aprPercent.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal r = aprPercent.divide(HUNDRED, MC).divide(TWELVE, MC);
        BigDecimal growth = BigDecimal.ONE.add(r, MC).pow(termMonths, MC);           // (1 + r)^n
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(growth, MC), MC);
        return principal.multiply(r, MC).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /** Safe ratio with 4 decimal places. Returns the cap when the denominator is zero. */
    public static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator, BigDecimal capWhenZero) {
        if (denominator.signum() == 0) {
            return capWhenZero;
        }
        BigDecimal value = numerator.divide(denominator, 4, RoundingMode.HALF_UP);
        return value.min(capWhenZero);
    }
}
