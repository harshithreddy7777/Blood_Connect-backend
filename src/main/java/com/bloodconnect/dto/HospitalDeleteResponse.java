package com.bloodconnect.dto;

public class HospitalDeleteResponse {

    private boolean success;
    private String message;
    private boolean deactivated;
    private HospitalResponse hospital;

    public HospitalDeleteResponse() {
    }

    public HospitalDeleteResponse(boolean success, String message, boolean deactivated, HospitalResponse hospital) {
        this.success = success;
        this.message = message;
        this.deactivated = deactivated;
        this.hospital = hospital;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    public HospitalResponse getHospital() {
        return hospital;
    }

    public void setHospital(HospitalResponse hospital) {
        this.hospital = hospital;
    }
}
