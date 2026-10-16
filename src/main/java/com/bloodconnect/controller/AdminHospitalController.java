package com.bloodconnect.controller;

import com.bloodconnect.dto.ApiResponse;
import com.bloodconnect.dto.HospitalDeleteResponse;
import com.bloodconnect.dto.HospitalRequest;
import com.bloodconnect.dto.HospitalResponse;
import com.bloodconnect.dto.PagedResponse;
import com.bloodconnect.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/admin/hospitals", "/api/v1/admin/hospitals"})
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminHospitalController {

    private final HospitalService hospitalService;

    public AdminHospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    /**
     * Admin view of all hospitals (including inactive ones) with filtering, pagination, and sorting.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<HospitalResponse>>> getHospitals(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        PagedResponse<HospitalResponse> response = hospitalService.getHospitalsPaged(
                city, active, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success("Hospitals fetched successfully", response));
    }

    /**
     * Admin inspects hospital details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalById(@PathVariable Long id) {
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(ApiResponse.success("Hospital fetched successfully", hospital));
    }

    /**
     * Admin registers a new partner hospital.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HospitalResponse>> createHospital(
            @Valid @RequestBody HospitalRequest request) {

        HospitalResponse created = hospitalService.adminCreateHospital(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Partner hospital registered successfully", created));
    }

    /**
     * Admin updates existing partner hospital details.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HospitalResponse>> updateHospital(
            @PathVariable Long id,
            @Valid @RequestBody HospitalRequest request) {

        HospitalResponse updated = hospitalService.adminUpdateHospital(id, request);
        return ResponseEntity.ok(ApiResponse.success("Hospital details updated successfully", updated));
    }

    /**
     * Admin safely deletes or deactivates hospital:
     * - Deactivates if referenced by historical blood requests to preserve clinical history.
     * - Physically deletes if unreferenced.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<HospitalDeleteResponse>> deleteHospital(@PathVariable Long id) {
        HospitalDeleteResponse result = hospitalService.adminDeleteOrDeactivateHospital(id);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }

    /**
     * Admin explicit status toggle (Active / Inactive).
     * Accepts either ?active=true/false query param or {"active": true/false} JSON body.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<HospitalResponse>> updateHospitalStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active,
            @RequestBody(required = false) java.util.Map<String, Object> body) {

        boolean targetActive = true;
        if (active != null) {
            targetActive = active;
        } else if (body != null && body.containsKey("active")) {
            targetActive = Boolean.parseBoolean(String.valueOf(body.get("active")));
        } else {
            throw new IllegalArgumentException("Hospital 'active' status must be specified either via query parameter or request body");
        }

        HospitalResponse updated = hospitalService.adminUpdateHospitalStatus(id, targetActive);
        String msg = targetActive
                ? String.format("Hospital '%s' has been activated and accredited", updated.getName())
                : String.format("Hospital '%s' has been deactivated", updated.getName());
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }
}
