package com.bloodconnect.dto;

import com.bloodconnect.entity.RequestStatus;
import com.bloodconnect.entity.RequestStatusAudit;

import java.time.LocalDateTime;

public class RequestStatusAuditResponse {

    private Long id;
    private String actorEmail;
    private String actorRole;
    private RequestStatus oldStatus;
    private RequestStatus newStatus;
    private String remarks;
    private LocalDateTime changedAt;

    public RequestStatusAuditResponse() {
    }

    public RequestStatusAuditResponse(Long id, String actorEmail, String actorRole,
                                      RequestStatus oldStatus, RequestStatus newStatus,
                                      String remarks, LocalDateTime changedAt) {
        this.id = id;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.remarks = remarks;
        this.changedAt = changedAt;
    }

    public static RequestStatusAuditResponse fromEntity(RequestStatusAudit audit) {
        if (audit == null) return null;
        return new RequestStatusAuditResponse(
                audit.getId(),
                audit.getActorEmail(),
                audit.getActorRole(),
                audit.getOldStatus(),
                audit.getNewStatus(),
                audit.getRemarks(),
                audit.getChangedAt()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
