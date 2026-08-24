package com.hiresense.api.web;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    public record HealthResponse(String status, String service, String timestamp) {}

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "hiresense-api", Instant.now().toString());
    }
}
