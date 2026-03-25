package com.ron.javainfohunter.api.controller;

import com.ron.javainfohunter.api.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller
 *
 * Simple controller for testing API connectivity.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> hello() {
        return ResponseEntity.ok(ApiResponse.success("Hello from TestController!"));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "pong");
        response.put("service", "javainfohunter-api");
        response.put("version", "0.0.1-SNAPSHOT");
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
