package com.bloodconnect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "donor_profiles",
    indexes = {
        @Index(name = "idx_donor_blood_group", columnList = "blood_group"),
        @Index(name = "idx_donor_city", columnList = "city"),
        @Index(name = "idx_donor_available", columnList = "available"),
        @Index(name = "idx_donor_search", columnList = "blood_group, city, available")
    }
)
public class DonorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String city;

    @Size(max = 255, message = "Area/address cannot exceed 255 characters")
    @Column(name = "area_address", length = 255)
    private String area;

    @NotBlank(message = "Contact number is required")
    @Size(max = 20, message = "Contact number cannot exceed 20 characters")
    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DonorProfile() {
    }

    public DonorProfile(User user, BloodGroup bloodGroup, String city, String area, String contactNumber, boolean available) {
        this.user = user;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.area = area;
        this.contactNumber = contactNumber;
        this.available = available;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
