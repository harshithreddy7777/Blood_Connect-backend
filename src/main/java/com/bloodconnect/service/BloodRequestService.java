package com.bloodconnect.service;

import com.bloodconnect.dto.*;
import com.bloodconnect.entity.*;
import com.bloodconnect.exception.DuplicateRequestException;
import com.bloodconnect.exception.InvalidStatusTransitionException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.RequestStatusAuditRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.specification.BloodRequestSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final RequestStatusAuditRepository requestStatusAuditRepository;
    private final UserRepository userRepository;
    private final HospitalService hospitalService;

    public BloodRequestService(BloodRequestRepository bloodRequestRepository,
                               RequestStatusAuditRepository requestStatusAuditRepository,
                               UserRepository userRepository,
                               HospitalService hospitalService) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.requestStatusAuditRepository = requestStatusAuditRepository;
        this.userRepository = userRepository;
        this.hospitalService = hospitalService;
    }

    /**
     * Creates a new emergency blood requisition for the authenticated user.
     * Enforces:
     * - Requester identity derived server-side from JWT.
     * - Verification that selected hospital exists and is active.
     * - Detection of duplicate pending requests for the same hospital & blood group.
     * - Default initial status is strictly PENDING.
     * - Independence from current donor pool availability.
     * - Records initial status audit trail.
     */
    @Transactional
    public BloodRequestResponse createRequest(String userEmail, BloodRequestCreateRequest request) {
        User requester = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Validate units
        if (request.getUnitsRequired() == null || request.getUnitsRequired() < 1) {
            throw new IllegalArgumentException("Units required must be at least 1");
        }

        // Verify hospital exists and is active
        Hospital hospital = hospitalService.getActiveHospitalEntity(request.getHospitalId());

        // Prevent duplicate spam: check if user already has an active PENDING request for same hospital and blood group
        boolean duplicateExists = bloodRequestRepository.existsByRequesterIdAndBloodGroupAndHospitalIdAndStatus(
                requester.getId(),
                request.getBloodGroup(),
                hospital.getId(),
                RequestStatus.PENDING
        );
        if (duplicateExists) {
            throw new DuplicateRequestException(
                    String.format("You already have an active pending blood request for blood group %s at %s. Please track your existing request or cancel it before creating a new one.",
                            request.getBloodGroup().getDisplayValue(), hospital.getName())
            );
        }

        BloodRequest bloodRequest = new BloodRequest(
                requester,
                request.getBloodGroup(),
                request.getUnitsRequired(),
                hospital,
                request.getLocation().trim(),
                request.getUrgency() != null ? request.getUrgency() : UrgencyLevel.HIGH,
                request.getContactNumber().trim(),
                request.getNotes() != null ? request.getNotes().trim() : null
        );

        BloodRequest saved = bloodRequestRepository.save(bloodRequest);

        // Record initial status creation in audit trail
        RequestStatusAudit initialAudit = new RequestStatusAudit(
                saved,
                userEmail,
                "ROLE_USER",
                RequestStatus.PENDING,
                RequestStatus.PENDING,
                "Initial emergency blood request submission"
        );
        requestStatusAuditRepository.save(initialAudit);
        saved.addAudit(initialAudit);

        return BloodRequestResponse.fromEntity(saved);
    }

    /**
     * Retrieves all blood requests submitted by the authenticated user.
     * Strictly isolates data to the caller.
     */
    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getMyRequests(String userEmail) {
        User requester = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        List<BloodRequest> requests = bloodRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId());
        return requests.stream()
                .map(BloodRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Inspects a specific blood request by ID.
     * Enforces authorization: only the original requester or an administrator can inspect the record.
     * Prevents access to another user's private request by guessing its ID (HTTP 403 Forbidden).
     */
    @Transactional(readOnly = true)
    public BloodRequestResponse getRequestById(Long requestId, String userEmail, boolean isAdmin) {
        BloodRequest bloodRequest = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with ID: " + requestId));

        if (!isAdmin && !bloodRequest.getRequester().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Access denied: You do not have permission to view this blood request");
        }

        return BloodRequestResponse.fromEntity(bloodRequest);
    }

    /**
     * Cancels a blood request.
     * Users may only cancel their own requests while in PENDING or APPROVED status.
     * Cannot cancel fulfilled or rejected requests.
     */
    @Transactional
    public BloodRequestResponse cancelRequest(Long requestId, String userEmail, boolean isAdmin) {
        BloodRequest bloodRequest = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with ID: " + requestId));

        if (!isAdmin && !bloodRequest.getRequester().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Access denied: You do not have permission to cancel this blood request");
        }

        RequestStatus oldStatus = bloodRequest.getStatus();
        bloodRequest.transitionTo(RequestStatus.CANCELLED);

        // Record cancellation audit entry
        RequestStatusAudit audit = new RequestStatusAudit(
                bloodRequest,
                userEmail,
                isAdmin ? "ROLE_ADMIN" : "ROLE_USER",
                oldStatus,
                RequestStatus.CANCELLED,
                isAdmin ? "Requisition cancelled by administrator" : "Requisition cancelled by requester"
        );
        requestStatusAuditRepository.save(audit);
        bloodRequest.addAudit(audit);

        BloodRequest updated = bloodRequestRepository.save(bloodRequest);
        return BloodRequestResponse.fromEntity(updated);
    }

    // ==========================================
    // Administrator Operations (Session 8)
    // ==========================================

    /**
     * Retrieves administrative view of all blood requests with dynamic filters,
     * deterministic sorting, and pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminBloodRequestResponse> adminGetRequests(
            RequestStatus status,
            BloodGroup bloodGroup,
            UrgencyLevel urgency,
            Long hospitalId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = buildDeterministicSort(sortBy, sortDir);
        int pageIndex = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

        Specification<BloodRequest> spec = BloodRequestSpecifications.withFilters(
                status, bloodGroup, urgency, hospitalId, startDate, endDate, search
        );

        Page<BloodRequest> pageResult = bloodRequestRepository.findAll(spec, pageable);
        return PagedResponse.of(pageResult.map(AdminBloodRequestResponse::fromEntity));
    }

    /**
     * Retrieves detailed record of a single blood request including requester info,
     * hospital details, and the complete status change audit trail.
     */
    @Transactional(readOnly = true)
    public AdminBloodRequestDetailResponse adminGetRequestById(Long id) {
        BloodRequest bloodRequest = bloodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with ID: " + id));

        List<RequestStatusAudit> audits = requestStatusAuditRepository.findByBloodRequestIdOrderByChangedAtAsc(id);
        AdminBloodRequestDetailResponse dto = AdminBloodRequestDetailResponse.fromEntityWithAudits(bloodRequest);
        if (audits != null && !audits.isEmpty()) {
            dto.setAuditTrail(audits.stream().map(RequestStatusAuditResponse::fromEntity).collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Administrative status transition with business rule enforcement:
     * - PENDING -> APPROVED, REJECTED, CANCELLED
     * - APPROVED -> FULFILLED, REJECTED (with justification), CANCELLED
     * - Rejects transitions from terminal states (FULFILLED, REJECTED, CANCELLED)
     * - Rejects same-status transitions
     * - Logs actor, old status, new status, remarks, and timestamp to audit trail
     * - Preserves consistency under concurrent updates via optimistic locking (@Version)
     */
    @Transactional
    public AdminBloodRequestDetailResponse adminUpdateStatus(Long id, AdminStatusUpdateRequest request, String adminEmail) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Target status is required");
        }

        BloodRequest bloodRequest = bloodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found with ID: " + id));

        RequestStatus oldStatus = bloodRequest.getStatus();
        RequestStatus newStatus = request.getStatus();

        // Enforce state machine transition (throws InvalidStatusTransitionException if invalid)
        bloodRequest.transitionTo(newStatus);

        String remarks = (request.getRemarks() != null && !request.getRemarks().trim().isEmpty())
                ? request.getRemarks().trim()
                : String.format("Status updated from %s to %s by administrator", oldStatus, newStatus);

        RequestStatusAudit audit = new RequestStatusAudit(
                bloodRequest,
                adminEmail,
                "ROLE_ADMIN",
                oldStatus,
                newStatus,
                remarks
        );
        requestStatusAuditRepository.save(audit);
        bloodRequest.addAudit(audit);

        BloodRequest saved = bloodRequestRepository.save(bloodRequest);
        return adminGetRequestById(saved.getId());
    }

    private Sort buildDeterministicSort(String sortBy, String sortDir) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String primaryField = "createdAt";
        if (sortBy != null) {
            String cleaned = sortBy.trim().toLowerCase();
            switch (cleaned) {
                case "id":
                    primaryField = "id";
                    break;
                case "urgency":
                    primaryField = "urgency";
                    break;
                case "status":
                    primaryField = "status";
                    break;
                case "unitsrequired":
                    primaryField = "unitsRequired";
                    break;
                case "bloodgroup":
                    primaryField = "bloodGroup";
                    break;
                case "updatedat":
                    primaryField = "updatedAt";
                    break;
                default:
                    primaryField = "createdAt";
                    break;
            }
        }
        if ("id".equals(primaryField)) {
            return Sort.by(direction, "id");
        } else {
            return Sort.by(direction, primaryField).and(Sort.by(Sort.Direction.DESC, "id"));
        }
    }
}
