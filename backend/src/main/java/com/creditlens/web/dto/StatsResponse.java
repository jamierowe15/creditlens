package com.creditlens.web.dto;

public record StatsResponse(
        long total,
        long approved,
        long referred,
        long declined,
        double approvalRatePercent,
        double averageCreditScore) {
}
