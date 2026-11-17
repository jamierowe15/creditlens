package com.creditlens.decision;

import com.creditlens.domain.ApplicationStatus;
import com.creditlens.domain.ReasonCode;

import java.math.BigDecimal;
import java.util.List;

/** Everything the engine concluded. approvedAmount is null unless the outcome is APPROVED. */
public record DecisionResult(
        ApplicationStatus outcome,
        int riskScore,
        BigDecimal debtToIncome,
        BigDecimal apr,
        BigDecimal estimatedMonthlyPayment,
        BigDecimal approvedAmount,
        List<ReasonCode> reasons,
        String rulesVersion) {

    public DecisionResult {
        reasons = List.copyOf(reasons); // defensive copy keeps the record truly immutable
    }
}
