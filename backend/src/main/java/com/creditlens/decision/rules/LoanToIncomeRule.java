package com.creditlens.decision.rules;

import com.creditlens.decision.CreditRule;
import com.creditlens.decision.DecisionContext;
import com.creditlens.decision.RuleResult;
import com.creditlens.domain.ReasonCode;

import java.math.BigDecimal;

/** Requested amount vs annual income. Over 100% decline, over 50% refer. */
public class LoanToIncomeRule implements CreditRule {

    private static final BigDecimal DECLINE_ABOVE = new BigDecimal("1.00");
    private static final BigDecimal REFER_ABOVE = new BigDecimal("0.50");

    @Override
    public String name() {
        return "loan-to-income";
    }

    @Override
    public RuleResult evaluate(DecisionContext ctx) {
        BigDecimal lti = ctx.loanToIncome();
        if (lti.compareTo(DECLINE_ABOVE) > 0) return RuleResult.of(ReasonCode.LTI1);
        if (lti.compareTo(REFER_ABOVE) > 0) return RuleResult.of(ReasonCode.LTI2);
        return RuleResult.pass();
    }
}
