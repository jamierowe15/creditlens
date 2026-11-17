package com.creditlens.decision.rules;

import com.creditlens.decision.CreditRule;
import com.creditlens.decision.DecisionContext;
import com.creditlens.decision.RuleResult;
import com.creditlens.domain.ReasonCode;

/** Below 600: decline. 600-659: a human decides. 660+: pass. */
public class CreditScoreRule implements CreditRule {

    @Override
    public String name() {
        return "credit-score";
    }

    @Override
    public RuleResult evaluate(DecisionContext ctx) {
        int score = ctx.profile().creditScore();
        if (score < 600) return RuleResult.of(ReasonCode.CS01);
        if (score < 660) return RuleResult.of(ReasonCode.CS02);
        return RuleResult.pass();
    }
}
