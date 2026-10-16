package com.bloodconnect.service;

import com.bloodconnect.dto.AdminDonorUpdateRequest;
import com.bloodconnect.dto.DonorProfileRequest;
import com.bloodconnect.dto.DonorProfileResponse;
import com.bloodconnect.dto.DonorSearchResponse;
import com.bloodconnect.dto.PagedResponse;
import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.DonorProfile;
import com.bloodconnect.entity.User;
import com.bloodconnect.exception.DuplicateProfileException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.specification.DonorSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DonorService {

    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;

    public DonorService(DonorProfileRepository donorProfileRepository, UserRepository userRepository) {
        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new voluntary donor profile for the authenticated user.
     * Prevents duplicate profiles for the same user.
     */
    @Transactional
    public DonorProfileResponse createProfile(String userEmail, DonorProfileRequest request) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Enforce 1:1 constraint: a user may have at most one donor profile
        if (donorProfileRepository.existsByUserId(user.getId())) {
            throw new DuplicateProfileException("A donor profile already exists for this user account. Use update to modify details.");
        }

        // Validate date
        validateDonationDate(request.getLastDonationDate());

        DonorProfile profile = new DonorProfile(
                user,
                request.getBloodGroup(),
                request.getCity().trim(),
                request.getArea() != null ? request.getArea().trim() : null,
                request.getContactNumber().trim(),
                request.getAvailable() != null ? request.getAvailable() : true
        );
        profile.setLastDonationDate(request.getLastDonationDate());

        DonorProfile saved = donorProfileRepository.save(profile);
        user.setDonorProfile(saved);

        return DonorProfileResponse.fromEntity(saved);
    }

    /**
     * Retrieves the donor profile for the authenticated user.
     */
    @Transactional(readOnly = true)
    public DonorProfileResponse getMyProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        DonorProfile profile = donorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No donor profile found for user: " + user.getEmail()));

        return DonorProfileResponse.fromEntity(profile);
    }

    /**
     * Updates permitted fields of the authenticated user's donor profile.
     */
    @Transactional
    public DonorProfileResponse updateMyProfile(String userEmail, DonorProfileRequest request) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        DonorProfile profile = donorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No donor profile exists to update. Create a profile first."));

        validateDonationDate(request.getLastDonationDate());

        profile.setBloodGroup(request.getBloodGroup());
        profile.setCity(request.getCity().trim());
        profile.setArea(request.getArea() != null ? request.getArea().trim() : null);
        profile.setContactNumber(request.getContactNumber().trim());
        profile.setLastDonationDate(request.getLastDonationDate());

        if (request.getAvailable() != null) {
            profile.setAvailable(request.getAvailable());
        }

        DonorProfile updated = donorProfileRepository.save(profile);
        return DonorProfileResponse.fromEntity(updated);
    }

    /**
     * Session 6: Dynamic donor search against MySQL database.
     * Combines optional filters: bloodGroup, city (case-insensitive), area, and available.
     * Applies deterministic sorting and pagination.
     * Enforces privacy: returns DonorSearchResponse (no email, password hashes, security fields).
     * Enforces access control: excludes unavailable donors by default; only ADMIN can view unavailable donors.
     */
    @Transactional(readOnly = true)
    public PagedResponse<DonorSearchResponse> searchDonors(
            String bloodGroupStr,
            String city,
            String area,
            Boolean available,
            int page,
            int size,
            String sortBy,
            String sortDir,
            Authentication authentication) {

        // Access control for availability:
        // Exclude unavailable donors from default available-donor results
        Boolean effectiveAvailable = available;
        if (effectiveAvailable == null) {
            effectiveAvailable = Boolean.TRUE; // Default: available donors only
        } else if (Boolean.FALSE.equals(effectiveAvailable)) {
            boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (!isAdmin) {
                throw new AccessDeniedException("Only administrators are permitted to query unavailable donor profiles");
            }
        }

        BloodGroup bloodGroup = null;
        if (bloodGroupStr != null && !bloodGroupStr.trim().isEmpty()) {
            bloodGroup = BloodGroup.fromString(bloodGroupStr.trim());
        }

        Sort sort = buildDeterministicSort(sortBy, sortDir);
        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

        Specification<DonorProfile> spec = DonorSpecifications.withFilters(
                bloodGroup,
                city,
                area,
                effectiveAvailable
        );

        Page<DonorProfile> resultPage = donorProfileRepository.findAll(spec, pageable);
        Page<DonorSearchResponse> dtoPage = resultPage.map(DonorSearchResponse::fromEntity);

        return PagedResponse.of(dtoPage);
    }

    private Sort buildDeterministicSort(String sortBy, String sortDir) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String primaryField = "id";
        if (sortBy != null) {
            String cleaned = sortBy.trim().toLowerCase();
            switch (cleaned) {
                case "city":
                    primaryField = "city";
                    break;
                case "bloodgroup":
                    primaryField = "bloodGroup";
                    break;
                case "createdat":
                    primaryField = "createdAt";
                    break;
                case "updatedat":
                    primaryField = "updatedAt";
                    break;
                case "lastdonationdate":
                    primaryField = "lastDonationDate";
                    break;
                default:
                    primaryField = "id";
                    break;
            }
        }
        // Always include a secondary tie-breaker on ID to ensure deterministic order across pages
        if ("id".equals(primaryField)) {
            return Sort.by(direction, "id");
        } else {
            return Sort.by(direction, primaryField).and(Sort.by(Sort.Direction.ASC, "id"));
        }
    }

    // ==========================================
    // Administrator Operations
    // ==========================================

    @Transactional(readOnly = true)
    public List<DonorProfileResponse> getAllDonors() {
        return donorProfileRepository.findAll()
                .stream()
                .map(DonorProfileResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DonorProfileResponse getDonorById(Long id) {
        DonorProfile profile = donorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found with ID: " + id));
        return DonorProfileResponse.fromEntity(profile);
    }

    @Transactional
    public DonorProfileResponse adminUpdateDonor(Long id, AdminDonorUpdateRequest request) {
        DonorProfile profile = donorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found with ID: " + id));

        if (request.getLastDonationDate() != null) {
            validateDonationDate(request.getLastDonationDate());
            profile.setLastDonationDate(request.getLastDonationDate());
        }
        if (request.getBloodGroup() != null) {
            profile.setBloodGroup(request.getBloodGroup());
        }
        if (request.getCity() != null) {
            profile.setCity(request.getCity().trim());
        }
        if (request.getArea() != null) {
            profile.setArea(request.getArea().trim());
        }
        if (request.getContactNumber() != null) {
            profile.setContactNumber(request.getContactNumber().trim());
        }
        if (request.getAvailable() != null) {
            profile.setAvailable(request.getAvailable());
        }

        DonorProfile updated = donorProfileRepository.save(profile);
        return DonorProfileResponse.fromEntity(updated);
    }

    /**
     * Safely deactivates or removes a donor profile.
     * Note on Rule 8: Historical blood requests are associated with the User entity (requester),
     * NOT the DonorProfile. Therefore, removing a donor profile does NOT delete or alter
     * any submitted blood requests or user authentication accounts.
     */
    @Transactional
    public void deleteDonorProfile(Long id) {
        DonorProfile profile = donorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found with ID: " + id));

        User user = profile.getUser();
        if (user != null) {
            user.setDonorProfile(null);
        }

        donorProfileRepository.delete(profile);
    }

    private void validateDonationDate(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Last donation date cannot be in the future: " + date);
        }
    }
}
