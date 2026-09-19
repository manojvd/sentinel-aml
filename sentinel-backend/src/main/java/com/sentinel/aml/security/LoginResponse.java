package com.sentinel.aml.security;

public record LoginResponse(String token, String username, String role, long expiresInMinutes) {
}
