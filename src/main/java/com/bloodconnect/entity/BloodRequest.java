package com.bloodconnect.entity;

import com.bloodconnect.exception.InvalidStatusTransitionException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "blood_requests",
    indexes = {
        @Index(name = "idx_request_status", columnList = "status"),
        @Index(name = "idx_request_blood_group", columnList = "blood_group"),
        @Index(name = "idx_request_location", columnList = "location"),
        @Index(name = "idx_request_urgency", columnList = "urgency")
    }
)
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Requester is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @NotNull(message = "Units required must be specified")
    @Min(value = 1, message = "Units required must be at least 1")
    @Column(name = "units_required", nullable = false)
    private Integer unitsRequired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @NotBlank(message = "Location/City is required")
    @Size(max = 150, message = "Location cannot exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String location;

    @NotNull(message = "Urgency level is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UrgencyLevel urgency = UrgencyLevel.HIGH;

    @NotBlank(message = "Contact number is required")
    @Size(max = 20, message = "Contact number cannot exceed 20 characters")
    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @NotNull(message = "Request status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "bloodRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("changedAt ASC")
    private List<RequestStatusAudit> auditTrail = new ArrayList<>();

    public BloodRequest() {
    }

    public BloodRequest(User requester, BloodGroup bloodGroup, Integer unitsRequired,
                        Hospital hospital, String location, UrgencyLevel urgency,
                        String contactNumber, String notes) {
        this.requester = requester;
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.hospital = hospital;
        this.location = location;
        this.urgency = urgency != null ? urgency : UrgencyLevel.HIGH;
        this.contactNumber = contactNumber;
        this.notes = notes;
        this.status = RequestStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = RequestStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Executes validated state transition according to the documented status state machine.
     *
     * @param nextStatus the proposed next status
     * @throws InvalidStatusTransitionException if the transition is not allowed
     * @throws IllegalArgumentException if the target status is null
     */
    public void transitionTo(RequestStatus nextStatus) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Target status cannot be null");
        }
        if (this.status == nextStatus) {
            throw new InvalidStatusTransitionException(
                String.format("Cannot transition blood request #%d to the same status '%s'", this.id != null ? this.id : 0, nextStatus)
            );
        }
        if (!this.status.canTransitionTo(nextStatus)) {
            throw new InvalidStatusTransitionException(
                String.format("Invalid status transition from '%s' to '%s'. Completed or terminal requests cannot be modified.", this.status, nextStatus)
            );
        }
        this.status = nextStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void addAudit(RequestStatusAudit audit) {
        this.auditTrail.add(audit);
        audit.setBloodRequest(this);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(Integer unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public UrgencyLevel getUrgency() {
        return urgency;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<RequestStatusAudit> getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(List<RequestStatusAudit> auditTrail) {
        this.auditTrail.clear();
        if (auditTrail != null) {
            this.auditTrail.addAll(auditTrail);
        }
    }
}
