package com.creditlens.decision;

import com.creditlens.domain.ReasonCode;
import com.creditlens.domain.Severity;

/** Outcome of a single rule. A PASS carries no reason code. */
public record RuleResult(Severity severity, ReasonCode reasonCode) {

    private static final RuleResult PASS = new RuleResult(Severity.PASS, null);

    public static RuleResult pass() {
        return PASS;
    }

    public static RuleResult of(ReasonCode code) {
        return new RuleResult(code.severity(), code);
    }

    public boolean passed() {
        return severity == Severity.PASS;
    }
}
