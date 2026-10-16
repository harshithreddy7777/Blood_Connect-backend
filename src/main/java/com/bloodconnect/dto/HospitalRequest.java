package com.bloodconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class HospitalRequest {

    @NotBlank(message = "Hospital name is required")
    @Size(min = 2, max = 150, message = "Hospital name must be between 2 and 150 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(min = 3, max = 255, message = "Address must be between 3 and 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @NotBlank(message = "Contact phone number is required")
    @Size(min = 7, max = 20, message = "Contact phone number must be between 7 and 20 characters")
    private String contactNumber;

    private Boolean active = true;

    public HospitalRequest() {
    }

    public HospitalRequest(String name, String address, String city, String contactNumber, Boolean active) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.contactNumber = contactNumber;
        this.active = active != null ? active : true;
    }

    // Getters and Setters
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
