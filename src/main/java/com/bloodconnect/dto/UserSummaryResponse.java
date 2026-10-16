package com.bloodconnect.dto;

import com.bloodconnect.entity.Role;
import com.bloodconnect.entity.User;

import java.time.LocalDateTime;

public class UserSummaryResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    private boolean hasDonorProfile;

    public UserSummaryResponse() {
    }

    public UserSummaryResponse(Long id, String fullName, String email, Role role, LocalDateTime createdAt, boolean hasDonorProfile) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.hasDonorProfile = hasDonorProfile;
    }

    public static UserSummaryResponse fromEntity(User user) {
        if (user == null) return null;
        return new UserSummaryResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getCreatedAt(),
            user.getDonorProfile() != null
        );
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isHasDonorProfile() {
        return hasDonorProfile;
    }

    public void setHasDonorProfile(boolean hasDonorProfile) {
        this.hasDonorProfile = hasDonorProfile;
    }
}
