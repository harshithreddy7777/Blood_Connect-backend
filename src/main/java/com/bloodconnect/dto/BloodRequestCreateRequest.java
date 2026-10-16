package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.UrgencyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BloodRequestCreateRequest {

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotNull(message = "Units required must be specified")
    @Min(value = 1, message = "Units required must be at least 1")
    private Integer unitsRequired;

    @NotNull(message = "Hospital selection is required")
    private Long hospitalId;

    @NotBlank(message = "Location/city is required")
    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgency = UrgencyLevel.HIGH;

    @NotBlank(message = "Contact number is required")
    @Size(max = 20, message = "Contact number cannot exceed 20 characters")
    private String contactNumber;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    public BloodRequestCreateRequest() {
    }

    public BloodRequestCreateRequest(BloodGroup bloodGroup, Integer unitsRequired, Long hospitalId,
                                    String location, UrgencyLevel urgency, String contactNumber, String notes) {
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.hospitalId = hospitalId;
        this.location = location;
        this.urgency = urgency != null ? urgency : UrgencyLevel.HIGH;
        this.contactNumber = contactNumber;
        this.notes = notes;
    }

    // Getters and Setters
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
}
