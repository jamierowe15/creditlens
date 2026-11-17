package com.creditlens.decision;

import com.creditlens.domain.ProductType;

import java.math.BigDecimal;

/** Risk-based pricing: better credit score = lower APR. */
public final class PricingPolicy {

    private PricingPolicy() {
    }

    public static BigDecimal baseAprFor(int creditScore) {
        if (creditScore >= 760) return new BigDecimal("7.49");
        if (creditScore >= 700) return new BigDecimal("9.49");
        if (creditScore >= 660) return new BigDecimal("12.99");
        if (creditScore >= 600) return new BigDecimal("16.99");
        return new BigDecimal("21.99");
    }

    public static BigDecimal aprFor(int creditScore, ProductType product) {
        return baseAprFor(creditScore).add(product.aprAdjustment());
    }
}
