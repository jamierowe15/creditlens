package com.creditlens.decision;

import java.math.BigDecimal;

/**
 * Profile plus the numbers derived from it. Computed once by the engine and handed to every
 * rule, so no rule recalculates (or miscalculates) DTI on its own.
 */
public record DecisionContext(
        ApplicantProfile profile,
        BigDecimal monthlyIncome,
        BigDecimal apr,
        BigDecimal estimatedMonthlyPayment,
        BigDecimal debtToIncome,
        BigDecimal loanToIncome) {
}
