package com.creditlens.domain;

import java.math.BigDecimal;

/**
 * Credit products. Each product adjusts the base APR from the credit-score band
 * (secured car loans are cheaper, unsecured personal loans are the baseline).
 */
public enum ProductType {
    PERSONAL_LOAN(new BigDecimal("0.00")),
    CAR_LOAN(new BigDecimal("-2.00")),
    LINE_OF_CREDIT(new BigDecimal("-1.00"));

    private final BigDecimal aprAdjustment;

    ProductType(BigDecimal aprAdjustment) {
        this.aprAdjustment = aprAdjustment;
    }

    public BigDecimal aprAdjustment() {
        return aprAdjustment;
    }
}
