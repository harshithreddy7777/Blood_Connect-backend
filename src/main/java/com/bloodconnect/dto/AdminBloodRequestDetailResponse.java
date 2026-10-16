package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminBloodRequestDetailResponse extends AdminBloodRequestResponse {

    private HospitalResponse hospital;
    private List<RequestStatusAuditResponse> auditTrail = new ArrayList<>();

    public AdminBloodRequestDetailResponse() {
    }

    public static AdminBloodRequestDetailResponse fromEntityWithAudits(BloodRequest entity) {
        if (entity == null) return null;
        AdminBloodRequestDetailResponse dto = new AdminBloodRequestDetailResponse();
        dto.setId(entity.getId());
        if (entity.getRequester() != null) {
            dto.setRequesterId(entity.getRequester().getId());
            dto.setRequesterName(entity.getRequester().getFullName());
            dto.setRequesterEmail(entity.getRequester().getEmail());
        }
        dto.setBloodGroup(entity.getBloodGroup());
        dto.setUnitsRequired(entity.getUnitsRequired());
        if (entity.getHospital() != null) {
            dto.setHospitalId(entity.getHospital().getId());
            dto.setHospitalName(entity.getHospital().getName());
            dto.setHospitalCity(entity.getHospital().getCity());
            dto.setHospitalAddress(entity.getHospital().getAddress());
            dto.setHospital(HospitalResponse.fromEntity(entity.getHospital()));
        }
        dto.setLocation(entity.getLocation());
        dto.setUrgency(entity.getUrgency());
        dto.setContactNumber(entity.getContactNumber());
        dto.setNotes(entity.getNotes());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setVersion(entity.getVersion());

        if (entity.getAuditTrail() != null) {
            dto.setAuditTrail(
                    entity.getAuditTrail().stream()
                            .map(RequestStatusAuditResponse::fromEntity)
                            .collect(Collectors.toList())
            );
        }
        return dto;
    }

    public HospitalResponse getHospital() {
        return hospital;
    }

    public void setHospital(HospitalResponse hospital) {
        this.hospital = hospital;
    }

    public List<RequestStatusAuditResponse> getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(List<RequestStatusAuditResponse> auditTrail) {
        this.auditTrail = auditTrail;
    }
}
