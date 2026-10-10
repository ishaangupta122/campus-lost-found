package com.campus.lostandfound.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.lostandfound.service.HealthService;

/**
 * GET /api/health - quick check that the server and the database are running.
 * The controller only handles HTTP; the actual check lives in HealthService.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        if (healthService.isDatabaseUp()) {
            return ResponseEntity.ok(Map.of("status", "UP", "database", "UP"));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DEGRADED", "database", "DOWN"));
    }
}
