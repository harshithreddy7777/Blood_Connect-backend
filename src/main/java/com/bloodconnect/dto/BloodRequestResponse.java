package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.BloodRequest;
import com.bloodconnect.entity.RequestStatus;
import com.bloodconnect.entity.UrgencyLevel;

import java.time.LocalDateTime;

public class BloodRequestResponse {

    private Long id;
    private Long requesterId;
    private String requesterName;
    private BloodGroup bloodGroup;
    private Integer unitsRequired;
    private Long hospitalId;
    private String hospitalName;
    private String hospitalAddress;
    private String hospitalCity;
    private String location;
    private UrgencyLevel urgency;
    private String contactNumber;
    private String notes;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BloodRequestResponse() {
    }

    public BloodRequestResponse(Long id, Long requesterId, String requesterName,
                                BloodGroup bloodGroup, Integer unitsRequired,
                                Long hospitalId, String hospitalName, String hospitalAddress, String hospitalCity,
                                String location, UrgencyLevel urgency, String contactNumber,
                                String notes, RequestStatus status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.hospitalAddress = hospitalAddress;
        this.hospitalCity = hospitalCity;
        this.location = location;
        this.urgency = urgency;
        this.contactNumber = contactNumber;
        this.notes = notes;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static BloodRequestResponse fromEntity(BloodRequest request) {
        if (request == null) return null;
        return new BloodRequestResponse(
                request.getId(),
                request.getRequester() != null ? request.getRequester().getId() : null,
                request.getRequester() != null ? request.getRequester().getFullName() : "Anonymous",
                request.getBloodGroup(),
                request.getUnitsRequired(),
                request.getHospital() != null ? request.getHospital().getId() : null,
                request.getHospital() != null ? request.getHospital().getName() : "Not Specified",
                request.getHospital() != null ? request.getHospital().getAddress() : null,
                request.getHospital() != null ? request.getHospital().getCity() : null,
                request.getLocation(),
                request.getUrgency(),
                request.getContactNumber(),
                request.getNotes(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
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

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
    }

    public String getHospitalCity() {
        return hospitalCity;
    }

    public void setHospitalCity(String hospitalCity) {
        this.hospitalCity = hospitalCity;
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
}
