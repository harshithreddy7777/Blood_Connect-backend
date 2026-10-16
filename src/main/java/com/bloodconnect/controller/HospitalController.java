package com.bloodconnect.controller;

import com.bloodconnect.dto.ApiResponse;
import com.bloodconnect.dto.HospitalResponse;
import com.bloodconnect.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/hospitals", "/api/v1/hospitals"})
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    /**
     * Public and authenticated hospital directory listing.
     * Default returns active accredited partner hospitals.
     * Supports optional city filter, pagination, and deterministic sorting.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getHospitals(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        if (page != null) {
            int pageIndex = Math.max(0, page);
            int pageSize = size != null ? size : 10;
            return ResponseEntity.ok(ApiResponse.success(
                    "Hospitals fetched successfully",
                    hospitalService.getHospitalsPaged(city, true, pageIndex, pageSize, sortBy, sortDir)
            ));
        }

        List<HospitalResponse> hospitals = hospitalService.getActiveHospitals(city);
        return ResponseEntity.ok(ApiResponse.success("Hospitals fetched successfully", hospitals));
    }

    /**
     * Inspect single hospital details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalById(@PathVariable Long id) {
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(ApiResponse.success("Hospital fetched successfully", hospital));
    }
}
