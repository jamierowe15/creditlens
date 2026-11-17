package com.creditlens.web.dto;

import com.creditlens.domain.ReasonCode;
import com.creditlens.domain.Severity;

public record ReasonView(String code, Severity severity, String description) {

    public static ReasonView of(ReasonCode code) {
        return new ReasonView(code.name(), code.severity(), code.description());
    }
}
