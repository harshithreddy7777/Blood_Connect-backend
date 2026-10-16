package com.bloodconnect.dto;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.DonorProfile;

import java.time.LocalDate;

/**
 * Public/Seeker facing donor search result item.
 * Strictly complies with Session 6 privacy requirements:
 * - Returns only necessary contact & matching fields.
 * - NEVER exposes email, password hashes, user ID, or internal security fields.
 */
public class DonorSearchResponse {

    private Long id;
    private String fullName;
    private BloodGroup bloodGroup;
    private String city;
    private String area;
    private String contactNumber;
    private boolean available;
    private LocalDate lastDonationDate;

    public DonorSearchResponse() {
    }

    public DonorSearchResponse(Long id, String fullName, BloodGroup bloodGroup, String city,
                               String area, String contactNumber, boolean available,
                               LocalDate lastDonationDate) {
        this.id = id;
        this.fullName = fullName;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.area = area;
        this.contactNumber = contactNumber;
        this.available = available;
        this.lastDonationDate = lastDonationDate;
    }

    public static DonorSearchResponse fromEntity(DonorProfile profile) {
        if (profile == null) return null;
        String name = profile.getUser() != null ? profile.getUser().getFullName() : "Anonymous Donor";
        return new DonorSearchResponse(
                profile.getId(),
                name,
                profile.getBloodGroup(),
                profile.getCity(),
                profile.getArea(),
                profile.getContactNumber(),
                profile.isAvailable(),
                profile.getLastDonationDate()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }
}
