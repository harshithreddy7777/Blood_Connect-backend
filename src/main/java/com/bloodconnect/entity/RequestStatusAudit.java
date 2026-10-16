package com.bloodconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "request_status_audits",
    indexes = {
        @Index(name = "idx_audit_request_id", columnList = "request_id"),
        @Index(name = "idx_audit_changed_at", columnList = "changed_at")
    }
)
public class RequestStatusAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private BloodRequest bloodRequest;

    @Column(name = "actor_email", nullable = false, length = 150)
    private String actorEmail;

    @Column(name = "actor_role", nullable = false, length = 30)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false, length = 20)
    private RequestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private RequestStatus newStatus;

    @Column(length = 500)
    private String remarks;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    public RequestStatusAudit() {
    }

    public RequestStatusAudit(BloodRequest bloodRequest, String actorEmail, String actorRole,
                              RequestStatus oldStatus, RequestStatus newStatus, String remarks) {
        this.bloodRequest = bloodRequest;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.remarks = remarks;
        this.changedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BloodRequest getBloodRequest() {
        return bloodRequest;
    }

    public void setBloodRequest(BloodRequest bloodRequest) {
        this.bloodRequest = bloodRequest;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public RequestStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(RequestStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public RequestStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(RequestStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
