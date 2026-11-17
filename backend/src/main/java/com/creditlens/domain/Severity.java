package com.creditlens.domain;

/** How serious a rule finding is. DECLINE beats REFER, REFER beats PASS. */
public enum Severity {
    PASS,
    REFER,
    DECLINE
}
