package com.bloodconnect.service;

import com.bloodconnect.dto.AdminDashboardResponse;
import com.bloodconnect.dto.RecentRequestSummary;
import com.bloodconnect.entity.BloodRequest;
import com.bloodconnect.entity.RequestStatus;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.HospitalRepository;
import com.bloodconnect.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalRepository hospitalRepository;

    public AdminDashboardService(UserRepository userRepository,
                                 DonorProfileRepository donorProfileRepository,
                                 BloodRequestRepository bloodRequestRepository,
                                 HospitalRepository hospitalRepository) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.hospitalRepository = hospitalRepository;
    }

    /**
     * Compute actual real-time aggregate statistics from the database.
     * Guaranteed zero-default counts (no nulls), deterministic recent request ordering,
     * and strict redaction of sensitive personal identity fields.
     */
    public AdminDashboardResponse getDashboardSummary() {
        AdminDashboardResponse summary = new AdminDashboardResponse();

        // 1. Registered users aggregate
        summary.setTotalUsers(userRepository.count());

        // 2. Donor aggregates
        // Total donors: All profiles created in database
        summary.setTotalDonors(donorProfileRepository.count());
        // Active donor profiles: Profiles that are verified and active (not deactivated)
        summary.setActiveDonorProfiles(donorProfileRepository.countByActiveTrue());
        // Available donors: Active donors currently ready to donate
        summary.setAvailableDonors(donorProfileRepository.countByActiveTrueAndAvailableTrue());

        // 3. Clinical blood request aggregates by state
        summary.setTotalBloodRequests(bloodRequestRepository.count());
        summary.setPendingRequests(bloodRequestRepository.countByStatus(RequestStatus.PENDING));
        summary.setApprovedRequests(bloodRequestRepository.countByStatus(RequestStatus.APPROVED));
        summary.setFulfilledRequests(bloodRequestRepository.countByStatus(RequestStatus.FULFILLED));
        summary.setRejectedRequests(bloodRequestRepository.countByStatus(RequestStatus.REJECTED));
        summary.setCancelledRequests(bloodRequestRepository.countByStatus(RequestStatus.CANCELLED));

        // 4. Partner hospital aggregates
        summary.setTotalHospitals(hospitalRepository.count());
        summary.setActiveHospitals(hospitalRepository.countByActiveTrue());

        // 5. Recent requests: Deterministic ordering by createdAt DESC, id DESC
        List<BloodRequest> recent = bloodRequestRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
        ).getContent();

        List<RecentRequestSummary> recentSummaries = recent.stream()
                .map(RecentRequestSummary::fromEntity)
                .collect(Collectors.toList());

        summary.setRecentRequests(recentSummaries);

        return summary;
    }
}
