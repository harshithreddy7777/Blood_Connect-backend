package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.DonorProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DonorProfileResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private BloodGroup bloodGroup;
    private String city;
    private String area;
    private String contactNumber;
    private LocalDate lastDonationDate;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DonorProfileResponse() {
    }

    public DonorProfileResponse(Long id, Long userId, String fullName, String email,
                                BloodGroup bloodGroup, String city, String area,
                                String contactNumber, LocalDate lastDonationDate,
                                boolean available, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.area = area;
        this.contactNumber = contactNumber;
        this.lastDonationDate = lastDonationDate;
        this.available = available;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DonorProfileResponse fromEntity(DonorProfile profile) {
        if (profile == null) return null;
        return new DonorProfileResponse(
                profile.getId(),
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getUser() != null ? profile.getUser().getFullName() : null,
                profile.getUser() != null ? profile.getUser().getEmail() : null,
                profile.getBloodGroup(),
                profile.getCity(),
                profile.getArea(),
                profile.getContactNumber(),
                profile.getLastDonationDate(),
                profile.isAvailable(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
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
