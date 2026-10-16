package com.bloodconnect.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardResponse {

    private long totalUsers = 0L;
    private long totalDonors = 0L;
    private long activeDonorProfiles = 0L;
    private long availableDonors = 0L;

    private long totalBloodRequests = 0L;
    private long pendingRequests = 0L;
    private long approvedRequests = 0L;
    private long fulfilledRequests = 0L;
    private long rejectedRequests = 0L;
    private long cancelledRequests = 0L;

    private long totalHospitals = 0L;
    private long activeHospitals = 0L;

    private List<RecentRequestSummary> recentRequests = new ArrayList<>();

    private Map<String, String> definitions = new LinkedHashMap<>();

    public AdminDashboardResponse() {
        initDefinitions();
    }

    private void initDefinitions() {
        definitions.put(
                "totalDonors",
                "Total number of registered donor profiles currently stored in the system database."
        );
        definitions.put(
                "activeDonorProfiles",
                "Registered donor profiles in good standing that are currently active in the registry (verified and not suspended/deactivated)."
        );
        definitions.put(
                "availableDonors",
                "Active donor profiles who have explicitly set their preference to available (ready for immediate blood donation dispatch, not in a 90-day cooldown period or marked busy)."
        );
    }

    // Getters and Setters
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalDonors() {
        return totalDonors;
    }

    public void setTotalDonors(long totalDonors) {
        this.totalDonors = totalDonors;
    }

    public long getActiveDonorProfiles() {
        return activeDonorProfiles;
    }

    public void setActiveDonorProfiles(long activeDonorProfiles) {
        this.activeDonorProfiles = activeDonorProfiles;
    }

    public long getAvailableDonors() {
        return availableDonors;
    }

    public void setAvailableDonors(long availableDonors) {
        this.availableDonors = availableDonors;
    }

    public long getTotalBloodRequests() {
        return totalBloodRequests;
    }

    public void setTotalBloodRequests(long totalBloodRequests) {
        this.totalBloodRequests = totalBloodRequests;
    }

    public long getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(long pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public long getApprovedRequests() {
        return approvedRequests;
    }

    public void setApprovedRequests(long approvedRequests) {
        this.approvedRequests = approvedRequests;
    }

    public long getFulfilledRequests() {
        return fulfilledRequests;
    }

    public void setFulfilledRequests(long fulfilledRequests) {
        this.fulfilledRequests = fulfilledRequests;
    }

    public long getRejectedRequests() {
        return rejectedRequests;
    }

    public void setRejectedRequests(long rejectedRequests) {
        this.rejectedRequests = rejectedRequests;
    }

    public long getCancelledRequests() {
        return cancelledRequests;
    }

    public void setCancelledRequests(long cancelledRequests) {
        this.cancelledRequests = cancelledRequests;
    }

    public long getTotalHospitals() {
        return totalHospitals;
    }

    public void setTotalHospitals(long totalHospitals) {
        this.totalHospitals = totalHospitals;
    }

    public long getActiveHospitals() {
        return activeHospitals;
    }

    public void setActiveHospitals(long activeHospitals) {
        this.activeHospitals = activeHospitals;
    }

    public List<RecentRequestSummary> getRecentRequests() {
        return recentRequests;
    }

    public void setRecentRequests(List<RecentRequestSummary> recentRequests) {
        this.recentRequests = recentRequests != null ? recentRequests : new ArrayList<>();
    }

    public Map<String, String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, String> definitions) {
        this.definitions = definitions;
    }
}
