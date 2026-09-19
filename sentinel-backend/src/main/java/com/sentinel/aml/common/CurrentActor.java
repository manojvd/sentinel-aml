package com.sentinel.aml.common;

import com.sentinel.aml.security.SentinelUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

public record CurrentActor(String username, String role) {

    public static CurrentActor fromSecurityContext() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SentinelUserDetails principal)) {
            return new CurrentActor("system", "SYSTEM");
        }
        return new CurrentActor(principal.getUsername(), principal.getRole());
    }
}
