package com.sentinel.aml.security;

import com.sentinel.aml.domain.User;
import com.sentinel.aml.domain.UserRole;
import com.sentinel.aml.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Idempotently seeds one demo user per role so the app is immediately demoable. Passwords come
 * from config (env-var overridable, see application.yml) -- never hardcoded for anything beyond
 * local-dev convenience.
 */
@Component
public class DemoUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String analystPassword;
    private final String viewerPassword;

    public DemoUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${sentinel.demo-users.admin-password}") String adminPassword,
            @Value("${sentinel.demo-users.analyst-password}") String analystPassword,
            @Value("${sentinel.demo-users.viewer-password}") String viewerPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.analystPassword = analystPassword;
        this.viewerPassword = viewerPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin", "Sentinel Admin", UserRole.ADMIN, adminPassword);
        seed("analyst", "Compliance Analyst", UserRole.COMPLIANCE_ANALYST, analystPassword);
        seed("viewer", "Read-only Viewer", UserRole.VIEWER, viewerPassword);
    }

    private void seed(String username, String fullName, UserRole role, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Seeded demo user '{}' with role {}", username, role);
    }
}
