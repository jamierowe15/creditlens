package com.creditlens.web.dto;

import com.creditlens.decision.DecisionResult;
import com.creditlens.domain.ApplicationStatus;

import java.math.BigDecimal;
import java.util.List;

public record DecisionPreviewResponse(
        ApplicationStatus outcome,
        int riskScore,
        BigDecimal dtiRatio,
        BigDecimal apr,
        BigDecimal monthlyPayment,
        BigDecimal approvedAmount,
        List<ReasonView> reasons,
        String rulesVersion) {

    public static DecisionPreviewResponse from(DecisionResult r) {
        return new DecisionPreviewResponse(r.outcome(), r.riskScore(), r.debtToIncome(), r.apr(),
                r.estimatedMonthlyPayment(), r.approvedAmount(),
                r.reasons().stream().map(ReasonView::of).toList(), r.rulesVersion());
    }
}
