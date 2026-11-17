package com.creditlens.web.dto;

import com.creditlens.decision.ApplicantProfile;
import com.creditlens.domain.EmploymentStatus;
import com.creditlens.domain.ProductType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for POST /api/applications. Never trust the client: every field is validated
 * on the server even though the Angular form validates the same things.
 */
public record CreateApplicationRequest(
        @NotBlank @Size(max = 100) String applicantName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal annualIncome,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal monthlyDebtPayments,
        @NotNull @Min(300) @Max(900) Integer creditScore,
        @NotNull @DecimalMin("1000.00") @DecimalMax("100000.00") @Digits(integer = 10, fraction = 2)
        BigDecimal requestedAmount,
        @NotNull @Min(6) @Max(84) Integer termMonths,
        @NotNull EmploymentStatus employmentStatus,
        @NotNull ProductType productType) {

    public ApplicantProfile toProfile() {
        return new ApplicantProfile(annualIncome, monthlyDebtPayments, creditScore, requestedAmount,
                termMonths, employmentStatus, productType);
    }
}
