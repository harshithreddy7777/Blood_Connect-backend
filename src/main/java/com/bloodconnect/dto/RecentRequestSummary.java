package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.BloodRequest;
import com.bloodconnect.entity.RequestStatus;
import com.bloodconnect.entity.UrgencyLevel;

import java.time.LocalDateTime;

public class RecentRequestSummary {

    private Long id;
    private BloodGroup bloodGroup;
    private Integer unitsRequired;
    private Long hospitalId;
    private String hospitalName;
    private String hospitalCity;
    private String location;
    private UrgencyLevel urgency;
    private RequestStatus status;
    private LocalDateTime createdAt;

    public RecentRequestSummary() {
    }

    public RecentRequestSummary(Long id, BloodGroup bloodGroup, Integer unitsRequired,
                                Long hospitalId, String hospitalName, String hospitalCity,
                                String location, UrgencyLevel urgency, RequestStatus status,
                                LocalDateTime createdAt) {
        this.id = id;
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.hospitalCity = hospitalCity;
        this.location = location;
        this.urgency = urgency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static RecentRequestSummary fromEntity(BloodRequest request) {
        if (request == null) return null;
        return new RecentRequestSummary(
                request.getId(),
                request.getBloodGroup(),
                request.getUnitsRequired(),
                request.getHospital() != null ? request.getHospital().getId() : null,
                request.getHospital() != null ? request.getHospital().getName() : "N/A",
                request.getHospital() != null ? request.getHospital().getCity() : null,
                request.getLocation(),
                request.getUrgency(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
