package com.bloodconnect.controller;

import com.bloodconnect.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/admin", "/api/v1/admin"})
public class AdminTestController {

    @GetMapping("/ping")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> adminPing() {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin authorization verified",
                Map.of("access", "GRANTED", "role", "ROLE_ADMIN")
        ));
    }
}
