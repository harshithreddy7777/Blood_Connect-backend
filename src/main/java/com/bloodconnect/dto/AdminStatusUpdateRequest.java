package com.bloodconnect.dto;

import com.bloodconnect.entity.RequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminStatusUpdateRequest {

    @NotNull(message = "Target status is required")
    private RequestStatus status;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    public AdminStatusUpdateRequest() {
    }

    public AdminStatusUpdateRequest(RequestStatus status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
