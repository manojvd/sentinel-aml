package com.sentinel.aml.admin;

import java.util.Map;

/** Every field is optional -- only what's provided gets updated. This is the "tune without a redeploy" API. */
public record UpdateDetectionRuleRequest(Boolean enabled, Integer weight, Map<String, Object> config) {
}
