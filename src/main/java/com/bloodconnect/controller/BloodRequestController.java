package com.bloodconnect.controller;

import com.bloodconnect.dto.ApiResponse;
import com.bloodconnect.dto.BloodRequestCreateRequest;
import com.bloodconnect.dto.BloodRequestResponse;
import com.bloodconnect.service.BloodRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/requests", "/api/v1/requests"})
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;

    public BloodRequestController(BloodRequestService bloodRequestService) {
        this.bloodRequestService = bloodRequestService;
    }

    /**
     * Session 7: Submit an emergency blood request.
     * Generates persistent database ID and starts in PENDING status.
     * Disclaimer: does not guarantee blood inventory supply.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BloodRequestResponse>> createRequest(
            Authentication authentication,
            @Valid @RequestBody BloodRequestCreateRequest request) {

        BloodRequestResponse response = bloodRequestService.createRequest(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Blood request submitted successfully with PENDING status. Voluntary donors in the area are notified.",
                        response
                ));
    }

    /**
     * Session 7: View authenticated user's submitted blood requests.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<BloodRequestResponse>>> getMyRequests(Authentication authentication) {
        List<BloodRequestResponse> requests = bloodRequestService.getMyRequests(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Blood requests fetched successfully", requests));
    }

    /**
     * Session 7: View request details by ID.
     * Prevents access to another user's private request by ID guessing (403 Forbidden).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> getRequestById(
            @PathVariable Long id,
            Authentication authentication) {

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        BloodRequestResponse response = bloodRequestService.getRequestById(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Blood request details retrieved successfully", response));
    }

    /**
     * Session 7: Cancel own blood request (PENDING or APPROVED status only).
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> cancelRequest(
            @PathVariable Long id,
            Authentication authentication) {

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        BloodRequestResponse response = bloodRequestService.cancelRequest(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Blood request cancelled successfully", response));
    }
}
