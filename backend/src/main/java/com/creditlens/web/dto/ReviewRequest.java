package com.creditlens.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Underwriter decision on a REFERRED application. A note is mandatory: decisions must be explained. */
public record ReviewRequest(
        @NotNull Boolean approve,
        @NotBlank @Size(max = 100) String underwriter,
        @NotBlank @Size(max = 500) String note) {
}
