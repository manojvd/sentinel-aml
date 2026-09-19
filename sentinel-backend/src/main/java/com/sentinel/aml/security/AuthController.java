package com.sentinel.aml.security;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Login and JWT issuance")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expirationMinutes;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${sentinel.jwt.expiration-minutes}") long expirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expirationMinutes = expirationMinutes;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        var principal = (SentinelUserDetails) authentication.getPrincipal();
        String token = jwtService.issueToken(principal.getUsername(), principal.getRole());
        return new LoginResponse(token, principal.getUsername(), principal.getRole(), expirationMinutes);
    }
}
