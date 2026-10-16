package com.bloodconnect.controller;

import com.bloodconnect.dto.ApiResponse;
import com.bloodconnect.dto.DonorProfileRequest;
import com.bloodconnect.dto.DonorProfileResponse;
import com.bloodconnect.dto.DonorSearchResponse;
import com.bloodconnect.dto.PagedResponse;
import com.bloodconnect.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/donors", "/api/v1/donors"})
public class DonorController {

    private final DonorService donorService;

    public DonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    /**
     * Session 6: Public / Authenticated donor search endpoint.
     * GET /api/donors?bloodGroup=&city=&area=&available=&page=0&size=10&sortBy=id&sortDir=ASC
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DonorSearchResponse>>> searchDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Boolean available,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            Authentication authentication) {

        PagedResponse<DonorSearchResponse> result = donorService.searchDonors(
                bloodGroup, city, area, available, page, size, sortBy, sortDir, authentication);

        return ResponseEntity.ok(ApiResponse.success("Donors fetched successfully", result));
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<DonorProfileResponse>> createMyProfile(
            Authentication authentication,
            @Valid @RequestBody DonorProfileRequest request) {

        DonorProfileResponse response = donorService.createProfile(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Donor profile created successfully", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DonorProfileResponse>> getMyProfile(Authentication authentication) {
        DonorProfileResponse response = donorService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Donor profile fetched successfully", response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<DonorProfileResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody DonorProfileRequest request) {

        DonorProfileResponse response = donorService.updateMyProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Donor profile updated successfully", response));
    }
}
