package com.sentinel.aml.seed;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/seed-demo-data")
@Tag(name = "Admin - Demo Data", description = "One-shot synthetic dataset covering every detection typology")
@PreAuthorize("hasRole('ADMIN')")
public class SeedController {

    private final DemoSeedService demoSeedService;

    public SeedController(DemoSeedService demoSeedService) {
        this.demoSeedService = demoSeedService;
    }

    @PostMapping
    public DemoSeedSummary seed(@RequestParam(defaultValue = "10000") int totalTransactions) throws IOException {
        return demoSeedService.seed(totalTransactions);
    }
}
