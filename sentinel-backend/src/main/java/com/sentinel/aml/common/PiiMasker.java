package com.sentinel.aml.common;

/**
 * PII masking lives here, in the service layer, so it's enforced regardless of which client
 * (dashboard, API caller, script) is asking -- never left to the frontend to hide.
 */
public final class PiiMasker {

    private PiiMasker() {
    }

    /** "Krishna" -> "K******" */
    public static String maskName(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.charAt(0) + "*".repeat(Math.max(1, value.length() - 1));
    }

    /** "CUST_00001" -> "CUST_****1" (keeps prefix + last char for correlation, masks the rest) */
    public static String maskId(String value) {
        if (value == null || value.length() <= 4) {
            return value == null ? null : "*".repeat(value.length());
        }
        int visibleTail = 1;
        String head = value.substring(0, value.length() - visibleTail - 4);
        String tail = value.substring(value.length() - visibleTail);
        return head + "****" + tail;
    }
}
