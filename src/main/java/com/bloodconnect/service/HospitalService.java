package com.bloodconnect.service;

import com.bloodconnect.dto.HospitalDeleteResponse;
import com.bloodconnect.dto.HospitalRequest;
import com.bloodconnect.dto.HospitalResponse;
import com.bloodconnect.dto.PagedResponse;
import com.bloodconnect.entity.Hospital;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.HospitalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final BloodRequestRepository bloodRequestRepository;

    public HospitalService(HospitalRepository hospitalRepository,
                           BloodRequestRepository bloodRequestRepository) {
        this.hospitalRepository = hospitalRepository;
        this.bloodRequestRepository = bloodRequestRepository;
    }

    /**
     * Public list of accredited active hospitals.
     */
    @Transactional(readOnly = true)
    public List<HospitalResponse> getActiveHospitals(String city) {
        List<Hospital> hospitals;
        if (city != null && !city.trim().isEmpty()) {
            hospitals = hospitalRepository.findByCityIgnoreCaseAndActiveTrue(city.trim());
        } else {
            hospitals = hospitalRepository.findByActiveTrue();
        }
        return hospitals.stream()
                .map(HospitalResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Predictable paginated retrieval with deterministic sorting.
     */
    @Transactional(readOnly = true)
    public PagedResponse<HospitalResponse> getHospitalsPaged(String city, Boolean active, int page, int size, String sortBy, String sortDir) {
        Sort sort = buildDeterministicSort(sortBy, sortDir);
        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

        Page<Hospital> hospitalPage;
        boolean hasCity = city != null && !city.trim().isEmpty();

        if (Boolean.FALSE.equals(active)) {
            // Admin querying all hospitals or specifically inactive
            if (hasCity) {
                hospitalPage = hospitalRepository.findByCityIgnoreCase(city.trim(), pageable);
            } else {
                hospitalPage = hospitalRepository.findAll(pageable);
            }
        } else {
            // Default: active only
            if (hasCity) {
                hospitalPage = hospitalRepository.findByCityIgnoreCaseAndActiveTrue(city.trim(), pageable);
            } else {
                hospitalPage = hospitalRepository.findByActiveTrue(pageable);
            }
        }

        return PagedResponse.of(hospitalPage.map(HospitalResponse::fromEntity));
    }

    /**
     * Retrieves hospital by ID.
     */
    @Transactional(readOnly = true)
    public HospitalResponse getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found with ID: " + id));
        return HospitalResponse.fromEntity(hospital);
    }

    /**
     * Internal lookup asserting that hospital exists and is active.
     */
    @Transactional(readOnly = true)
    public Hospital getActiveHospitalEntity(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found with ID: " + id));
        if (!hospital.isActive()) {
            throw new IllegalStateException("Selected hospital '" + hospital.getName() + "' is currently inactive or not accredited.");
        }
        return hospital;
    }

    // ==========================================
    // Administrator Operations (Session 9)
    // ==========================================

    /**
     * Admin creates a new partner hospital.
     */
    @Transactional
    public HospitalResponse adminCreateHospital(HospitalRequest request) {
        Hospital hospital = new Hospital(
                request.getName().trim(),
                request.getAddress().trim(),
                request.getCity().trim(),
                request.getContactNumber().trim(),
                request.getActive() != null ? request.getActive() : true
        );

        Hospital saved = hospitalRepository.save(hospital);
        return HospitalResponse.fromEntity(saved);
    }

    /**
     * Admin updates hospital details.
     */
    @Transactional
    public HospitalResponse adminUpdateHospital(Long id, HospitalRequest request) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found with ID: " + id));

        hospital.setName(request.getName().trim());
        hospital.setAddress(request.getAddress().trim());
        hospital.setCity(request.getCity().trim());
        hospital.setContactNumber(request.getContactNumber().trim());
        if (request.getActive() != null) {
            hospital.setActive(request.getActive());
        }

        Hospital saved = hospitalRepository.save(hospital);
        return HospitalResponse.fromEntity(saved);
    }

    /**
     * Admin deletes or safely deactivates hospital:
     * - If historical blood requests reference this hospital, prefer deactivation to preserve request history.
     * - If no historical records reference it, safely delete.
     */
    @Transactional
    public HospitalDeleteResponse adminDeleteOrDeactivateHospital(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found with ID: " + id));

        long count = bloodRequestRepository.countByHospitalId(id);
        if (count > 0) {
            // Safe deactivation fallback: keep historical records intact!
            hospital.setActive(false);
            Hospital saved = hospitalRepository.save(hospital);
            return new HospitalDeleteResponse(
                    true,
                    String.format("Hospital '%s' is referenced by %d historical blood request(s) and has been safely deactivated rather than deleted to preserve request history.",
                            hospital.getName(), count),
                    true,
                    HospitalResponse.fromEntity(saved)
            );
        } else {
            // Safe to delete permanently
            hospitalRepository.delete(hospital);
            return new HospitalDeleteResponse(
                    true,
                    String.format("Hospital '%s' has been permanently deleted.", hospital.getName()),
                    false,
                    null
            );
        }
    }

    /**
     * Admin explicit status toggle / deactivation.
     */
    @Transactional
    public HospitalResponse adminUpdateHospitalStatus(Long id, boolean active) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found with ID: " + id));

        hospital.setActive(active);
        Hospital saved = hospitalRepository.save(hospital);
        return HospitalResponse.fromEntity(saved);
    }

    private Sort buildDeterministicSort(String sortBy, String sortDir) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String primaryField = "name";
        if (sortBy != null) {
            String cleaned = sortBy.trim().toLowerCase();
            switch (cleaned) {
                case "id":
                    primaryField = "id";
                    break;
                case "city":
                    primaryField = "city";
                    break;
                case "active":
                    primaryField = "active";
                    break;
                case "createdat":
                    primaryField = "createdAt";
                    break;
                default:
                    primaryField = "name";
                    break;
            }
        }
        if ("id".equals(primaryField)) {
            return Sort.by(direction, "id");
        } else {
            return Sort.by(direction, primaryField).and(Sort.by(Sort.Direction.ASC, "id"));
        }
    }
}
