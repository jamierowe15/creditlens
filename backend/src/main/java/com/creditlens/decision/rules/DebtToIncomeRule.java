package com.creditlens.decision.rules;

import com.creditlens.decision.CreditRule;
import com.creditlens.decision.DecisionContext;
import com.creditlens.decision.RuleResult;
import com.creditlens.domain.ReasonCode;

import java.math.BigDecimal;

/** DTI = (existing monthly debt + new payment) / monthly income. Above 50% decline, 40-50% refer. */
public class DebtToIncomeRule implements CreditRule {

    private static final BigDecimal DECLINE_ABOVE = new BigDecimal("0.50");
    private static final BigDecimal REFER_ABOVE = new BigDecimal("0.40");

    @Override
    public String name() {
        return "debt-to-income";
    }

    @Override
    public RuleResult evaluate(DecisionContext ctx) {
        BigDecimal dti = ctx.debtToIncome();
        // compareTo, never equals: new BigDecimal("0.5").equals(new BigDecimal("0.50")) is false!
        if (dti.compareTo(DECLINE_ABOVE) > 0) return RuleResult.of(ReasonCode.DTI1);
        if (dti.compareTo(REFER_ABOVE) > 0) return RuleResult.of(ReasonCode.DTI2);
        return RuleResult.pass();
    }
}
