package com.creditlens.web.dto;

import com.creditlens.domain.ApplicationStatus;
import com.creditlens.domain.CreditApplication;
import com.creditlens.domain.EmploymentStatus;
import com.creditlens.domain.ProductType;
import com.creditlens.util.Masking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What the API returns. We never return the JPA entity itself: the DTO controls exactly which
 * fields leave the service (the email is masked) and keeps the API stable if the table changes.
 */
public record ApplicationResponse(
        Long id,
        String referenceNumber,
        String applicantName,
        String emailMasked,
        BigDecimal annualIncome,
        BigDecimal monthlyDebtPayments,
        int creditScore,
        BigDecimal requestedAmount,
        int termMonths,
        EmploymentStatus employmentStatus,
        ProductType productType,
        ApplicationStatus status,
        int riskScore,
        BigDecimal dtiRatio,
        BigDecimal apr,
        BigDecimal monthlyPayment,
        BigDecimal approvedAmount,
        List<ReasonView> reasons,
        String decidedBy,
        Instant createdAt,
        Instant updatedAt) {

    public static ApplicationResponse from(CreditApplication a) {
        return new ApplicationResponse(
                a.getId(), a.getReferenceNumber(), a.getApplicantName(), Masking.email(a.getEmail()),
                a.getAnnualIncome(), a.getMonthlyDebtPayments(), a.getCreditScore(), a.getRequestedAmount(),
                a.getTermMonths(), a.getEmploymentStatus(), a.getProductType(), a.getStatus(),
                a.getRiskScore(), a.getDtiRatio(), a.getApr(), a.getMonthlyPayment(), a.getApprovedAmount(),
                a.getReasonCodes().stream().map(ReasonView::of).toList(),
                a.getDecidedBy(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
