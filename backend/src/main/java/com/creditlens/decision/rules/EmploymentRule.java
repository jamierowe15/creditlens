package com.creditlens.decision.rules;

import com.creditlens.decision.CreditRule;
import com.creditlens.decision.DecisionContext;
import com.creditlens.decision.RuleResult;
import com.creditlens.domain.ReasonCode;

/** Income stability. Self-employed and student income needs documents a human must check. */
public class EmploymentRule implements CreditRule {

    @Override
    public String name() {
        return "employment";
    }

    @Override
    public RuleResult evaluate(DecisionContext ctx) {
        return switch (ctx.profile().employmentStatus()) {
            case UNEMPLOYED -> RuleResult.of(ReasonCode.EMP1);
            case SELF_EMPLOYED, STUDENT -> RuleResult.of(ReasonCode.EMP2);
            case FULL_TIME, PART_TIME, RETIRED -> RuleResult.pass();
        };
    }
}
