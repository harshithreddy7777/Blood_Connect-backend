package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodGroup;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class AdminDonorUpdateRequest {

    private BloodGroup bloodGroup;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 255, message = "Area cannot exceed 255 characters")
    private String area;

    @Size(max = 20, message = "Contact number cannot exceed 20 characters")
    private String contactNumber;

    @PastOrPresent(message = "Last donation date cannot be in the future")
    private LocalDate lastDonationDate;

    private Boolean available;

    public AdminDonorUpdateRequest() {
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
