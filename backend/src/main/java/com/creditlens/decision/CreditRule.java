package com.creditlens.decision;

/**
 * Strategy pattern: every credit policy is its own small class implementing this interface.
 * Adding a policy = adding a class. The engine never changes (Open/Closed principle).
 */
public interface CreditRule {

    /** Short, stable name used in logs and tests. */
    String name();

    RuleResult evaluate(DecisionContext context);
}
