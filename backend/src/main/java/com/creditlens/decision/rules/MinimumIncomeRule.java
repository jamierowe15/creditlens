package com.creditlens.decision.rules;

import com.creditlens.decision.CreditRule;
import com.creditlens.decision.DecisionContext;
import com.creditlens.decision.RuleResult;
import com.creditlens.domain.ReasonCode;

import java.math.BigDecimal;

public class MinimumIncomeRule implements CreditRule {

    private static final BigDecimal MINIMUM = new BigDecimal("20000");

    @Override
    public String name() {
        return "minimum-income";
    }

    @Override
    public RuleResult evaluate(DecisionContext ctx) {
        return ctx.profile().annualIncome().compareTo(MINIMUM) < 0
                ? RuleResult.of(ReasonCode.INC1)
                : RuleResult.pass();
    }
}
