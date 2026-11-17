package com.creditlens.domain;

/** Lifecycle of a credit application. REFERRED means "a human underwriter must decide". */
public enum ApplicationStatus {
    APPROVED,
    REFERRED,
    DECLINED
}
