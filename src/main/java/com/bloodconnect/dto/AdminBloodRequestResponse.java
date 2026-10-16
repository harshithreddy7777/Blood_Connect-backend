package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.BloodRequest;
import com.bloodconnect.entity.RequestStatus;
import com.bloodconnect.entity.UrgencyLevel;

import java.time.LocalDateTime;

public class AdminBloodRequestResponse {

    private Long id;
    private Long requesterId;
    private String requesterName;
    private String requesterEmail;
    private BloodGroup bloodGroup;
    private Integer unitsRequired;
    private Long hospitalId;
    private String hospitalName;
    private String hospitalCity;
    private String hospitalAddress;
    private String location;
    private UrgencyLevel urgency;
    private String contactNumber;
    private String notes;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public AdminBloodRequestResponse() {
    }

    public static AdminBloodRequestResponse fromEntity(BloodRequest entity) {
        if (entity == null) return null;
        AdminBloodRequestResponse dto = new AdminBloodRequestResponse();
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
        }
        dto.setLocation(entity.getLocation());
        dto.setUrgency(entity.getUrgency());
        dto.setContactNumber(entity.getContactNumber());
        dto.setNotes(entity.getNotes());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setVersion(entity.getVersion());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
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

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalCity() {
        return hospitalCity;
    }

    public void setHospitalCity(String hospitalCity) {
        this.hospitalCity = hospitalCity;
    }

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
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
}
