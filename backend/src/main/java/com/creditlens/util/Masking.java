package com.creditlens.util;

/**
 * PII masking for logs and API responses. Rule of thumb in banking: personal data never
 * appears in plain text in logs, because logs are copied to many systems and kept for years.
 */
public final class Masking {

    private Masking() {
    }

    /** jamie.rowe@example.com -> j*********@example.com */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "*".repeat(Math.max(3, at - 1)) + email.substring(at);
    }
}
