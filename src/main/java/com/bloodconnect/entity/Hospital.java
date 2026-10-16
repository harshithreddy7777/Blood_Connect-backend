package com.bloodconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "hospitals",
    indexes = {
        @Index(name = "idx_hospital_city", columnList = "city")
    }
)
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Hospital name is required")
    @Size(max = 150, message = "Hospital name cannot exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String city;

    @NotBlank(message = "Contact number is required")
    @Size(max = 20, message = "Contact number cannot exceed 20 characters")
    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "hospital", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BloodRequest> bloodRequests = new ArrayList<>();

    public Hospital() {
    }

    public Hospital(String name, String address, String city, String contactNumber) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.contactNumber = contactNumber;
        this.active = true;
    }

    public Hospital(String name, String address, String city, String contactNumber, boolean active) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.contactNumber = contactNumber;
        this.active = active;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
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

    public List<BloodRequest> getBloodRequests() {
        return bloodRequests;
    }

    public void setBloodRequests(List<BloodRequest> bloodRequests) {
        this.bloodRequests = bloodRequests;
    }
}
