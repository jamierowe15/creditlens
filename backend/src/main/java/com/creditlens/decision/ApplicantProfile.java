package com.creditlens.decision;

import com.creditlens.domain.EmploymentStatus;
import com.creditlens.domain.ProductType;

import java.math.BigDecimal;

/**
 * The financial facts the engine decides on. A record is an immutable data carrier:
 * Java generates the constructor, accessors, equals, hashCode and toString.
 * Note: no name or email here. The engine never needs PII to make a decision.
 */
public record ApplicantProfile(
        BigDecimal annualIncome,
        BigDecimal monthlyDebtPayments,
        int creditScore,
        BigDecimal requestedAmount,
        int termMonths,
        EmploymentStatus employmentStatus,
        ProductType productType) {
}
