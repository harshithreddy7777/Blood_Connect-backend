package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class DonorProfileRequest {

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 255, message = "Area/address cannot exceed 255 characters")
    private String area;

    @NotBlank(message = "Contact number is required")
    @Size(max = 20, message = "Contact number cannot exceed 20 characters")
    private String contactNumber;

    @PastOrPresent(message = "Last donation date cannot be in the future")
    private LocalDate lastDonationDate;

    private Boolean available = true;

    public DonorProfileRequest() {
    }

    public DonorProfileRequest(BloodGroup bloodGroup, String city, String area, String contactNumber, LocalDate lastDonationDate, Boolean available) {
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.area = area;
        this.contactNumber = contactNumber;
        this.lastDonationDate = lastDonationDate;
        this.available = available != null ? available : true;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
