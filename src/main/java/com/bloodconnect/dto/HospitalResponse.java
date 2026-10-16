package com.bloodconnect.dto;

import com.bloodconnect.entity.Hospital;

public class HospitalResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String contactNumber;
    private boolean active;

    public HospitalResponse() {
    }

    public HospitalResponse(Long id, String name, String address, String city, String contactNumber, boolean active) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.contactNumber = contactNumber;
        this.active = active;
    }

    public static HospitalResponse fromEntity(Hospital hospital) {
        if (hospital == null) return null;
        return new HospitalResponse(
                hospital.getId(),
                hospital.getName(),
                hospital.getAddress(),
                hospital.getCity(),
                hospital.getContactNumber(),
                hospital.isActive()
        );
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
}
