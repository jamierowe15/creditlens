package com.creditlens.domain;

/**
 * Explainable reason codes. Every automated REFER or DECLINE must say WHY, in words an
 * applicant or auditor can understand. Lenders call these "adverse action" reasons.
 */
public enum ReasonCode {
    CS01(Severity.DECLINE, "Credit score is below the 600 minimum"),
    CS02(Severity.REFER, "Credit score is in the 600-659 manual review band"),
    DTI1(Severity.DECLINE, "Debt-to-income ratio would be above 50%"),
    DTI2(Severity.REFER, "Debt-to-income ratio would be between 40% and 50%"),
    EMP1(Severity.DECLINE, "No current employment income"),
    EMP2(Severity.REFER, "Income type requires manual verification"),
    LTI1(Severity.DECLINE, "Requested amount is more than annual income"),
    LTI2(Severity.REFER, "Requested amount is more than 50% of annual income"),
    INC1(Severity.DECLINE, "Annual income is below the $20,000 minimum");

    private final Severity severity;
    private final String description;

    ReasonCode(Severity severity, String description) {
        this.severity = severity;
        this.description = description;
    }

    public Severity severity() {
        return severity;
    }

    public String description() {
        return description;
    }
}
