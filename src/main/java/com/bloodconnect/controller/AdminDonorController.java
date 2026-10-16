package com.bloodconnect.controller;

import com.bloodconnect.dto.AdminDonorUpdateRequest;
import com.bloodconnect.dto.ApiResponse;
import com.bloodconnect.dto.DonorProfileResponse;
import com.bloodconnect.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/donors", "/api/v1/admin/donors"})
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDonorController {

    private final DonorService donorService;

    public AdminDonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DonorProfileResponse>>> listAllDonors() {
        List<DonorProfileResponse> donors = donorService.getAllDonors();
        return ResponseEntity.ok(ApiResponse.success("All donor profiles retrieved", donors));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DonorProfileResponse>> getDonorById(@PathVariable Long id) {
        DonorProfileResponse donor = donorService.getDonorById(id);
        return ResponseEntity.ok(ApiResponse.success("Donor profile retrieved", donor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DonorProfileResponse>> updateDonor(
            @PathVariable Long id,
            @Valid @RequestBody AdminDonorUpdateRequest request) {

        DonorProfileResponse updated = donorService.adminUpdateDonor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Donor profile updated by administrator", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDonor(@PathVariable Long id) {
        donorService.deleteDonorProfile(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Donor profile removed successfully. Associated user account and historical requests remain preserved.",
                null
        ));
    }
}
