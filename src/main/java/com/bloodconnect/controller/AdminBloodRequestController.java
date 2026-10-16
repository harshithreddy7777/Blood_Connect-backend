package com.bloodconnect.controller;

import com.bloodconnect.dto.AdminBloodRequestDetailResponse;
import com.bloodconnect.dto.AdminBloodRequestResponse;
import com.bloodconnect.dto.AdminStatusUpdateRequest;
import com.bloodconnect.dto.ApiResponse;
import com.bloodconnect.dto.PagedResponse;
import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.RequestStatus;
import com.bloodconnect.entity.UrgencyLevel;
import com.bloodconnect.service.BloodRequestService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping({"/api/admin/requests", "/api/v1/admin/requests"})
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminBloodRequestController {

    private final BloodRequestService bloodRequestService;

    public AdminBloodRequestController(BloodRequestService bloodRequestService) {
        this.bloodRequestService = bloodRequestService;
    }

    /**
     * Lists all blood requests with dynamic filters, deterministic sorting, and pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminBloodRequestResponse>>> getRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) UrgencyLevel urgency,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        BloodGroup parsedBloodGroup = null;
        if (bloodGroup != null && !bloodGroup.trim().isEmpty()) {
            parsedBloodGroup = BloodGroup.fromString(bloodGroup.trim());
        }

        PagedResponse<AdminBloodRequestResponse> response = bloodRequestService.adminGetRequests(
                status, parsedBloodGroup, urgency, hospitalId, startDate, endDate, search, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success("Blood requests retrieved successfully", response));
    }

    /**
     * Retrieves a single blood request by ID with complete hospital details and audit history.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminBloodRequestDetailResponse>> getRequestById(@PathVariable Long id) {
        AdminBloodRequestDetailResponse response = bloodRequestService.adminGetRequestById(id);
        return ResponseEntity.ok(ApiResponse.success("Blood request details retrieved successfully", response));
    }

    /**
     * Updates the status of a blood request according to documented state transitions.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminBloodRequestDetailResponse>> updateRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusUpdateRequest request,
            org.springframework.security.core.Authentication authentication) {

        String adminEmail = authentication != null ? authentication.getName() : "admin@bloodconnect.com";
        AdminBloodRequestDetailResponse updated = bloodRequestService.adminUpdateStatus(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Blood request #%d status updated to %s", id, updated.getStatus()),
                updated
        ));
    }
}
