package com.creditlens.config;

import com.creditlens.decision.DecisionEngine;
import com.creditlens.decision.rules.CreditScoreRule;
import com.creditlens.decision.rules.DebtToIncomeRule;
import com.creditlens.decision.rules.EmploymentRule;
import com.creditlens.decision.rules.LoanToIncomeRule;
import com.creditlens.decision.rules.MinimumIncomeRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Wires the pure-Java engine into Spring. The engine itself has no Spring annotations, so it
 * can be unit tested with a plain "new DecisionEngine(...)".
 */
@Configuration
public class DecisionConfig {

    @Bean
    public DecisionEngine decisionEngine() {
        return new DecisionEngine(List.of(
                new MinimumIncomeRule(),
                new EmploymentRule(),
                new CreditScoreRule(),
                new DebtToIncomeRule(),
                new LoanToIncomeRule()));
    }
}
