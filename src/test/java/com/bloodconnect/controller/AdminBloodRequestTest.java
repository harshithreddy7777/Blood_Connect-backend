package com.bloodconnect.controller;

import com.bloodconnect.dto.AdminStatusUpdateRequest;
import com.bloodconnect.entity.*;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.HospitalRepository;
import com.bloodconnect.repository.RequestStatusAuditRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class AdminBloodRequestTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private RequestStatusAuditRepository requestStatusAuditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User adminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;
    private Hospital testHospital;
    private BloodRequest pendingRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        requestStatusAuditRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        hospitalRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Admin
        adminUser = new User("System Admin", "admin.sess8@example.com", passwordEncoder.encode("AdminPass123"), Role.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getRole().name(), adminUser.getId());

        // 2. Create Regular User
        regularUser = new User("Requester Charlie", "requester.charlie@example.com", passwordEncoder.encode("CharliePass123"), Role.ROLE_USER);
        regularUser = userRepository.save(regularUser);
        userToken = jwtUtil.generateToken(regularUser.getEmail(), regularUser.getRole().name(), regularUser.getId());

        // 3. Create Hospital
        testHospital = new Hospital("Apollo Speciality", "Apollo Cross Road", "Hyderabad", "040-23607777");
        testHospital = hospitalRepository.save(testHospital);

        // 4. Create Baseline Blood Request (starts in PENDING)
        pendingRequest = new BloodRequest(
                regularUser,
                BloodGroup.O_POSITIVE,
                3,
                testHospital,
                "Hyderabad",
                UrgencyLevel.CRITICAL,
                "9876543210",
                "Patient scheduled for emergency cardiac surgery"
        );
        pendingRequest = bloodRequestRepository.save(pendingRequest);

        RequestStatusAudit initialAudit = new RequestStatusAudit(
                pendingRequest,
                regularUser.getEmail(),
                "ROLE_USER",
                RequestStatus.PENDING,
                RequestStatus.PENDING,
                "Initial emergency submission"
        );
        requestStatusAuditRepository.save(initialAudit);
        pendingRequest.addAudit(initialAudit);
    }

    // ==========================================
    // 1. Role Authorization & Authentication
    // ==========================================

    @Test
    @DisplayName("Admin endpoint GET /api/admin/requests returns 200 OK for ADMIN")
    void testGetAllRequests_AsAdmin_Returns200WithPagedResults() throws Exception {
        mockMvc.perform(get("/api/admin/requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].bloodGroup").value("O+"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].requesterEmail").value("requester.charlie@example.com"));
    }

    @Test
    @DisplayName("Admin endpoint GET /api/admin/requests returns 403 Forbidden for USER")
    void testGetAllRequests_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/requests")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Admin endpoint GET /api/admin/requests returns 401 Unauthorized when unauthenticated")
    void testGetAllRequests_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/requests"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. Query Filtering (Status, BloodGroup, Urgency)
    // ==========================================

    @Test
    @DisplayName("Filter requests by status returns only matching records")
    void testFilterRequests_ByStatus() throws Exception {
        // Create an approved request
        BloodRequest approvedReq = new BloodRequest(
                regularUser, BloodGroup.A_POSITIVE, 2, testHospital, "Hyderabad",
                UrgencyLevel.HIGH, "9876543211", "Approved case"
        );
        approvedReq.setStatus(RequestStatus.APPROVED);
        bloodRequestRepository.save(approvedReq);

        mockMvc.perform(get("/api/admin/requests")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"));

        mockMvc.perform(get("/api/admin/requests")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Filter requests by blood group returns only matching records")
    void testFilterRequests_ByBloodGroup() throws Exception {
        BloodRequest bReq = new BloodRequest(
                regularUser, BloodGroup.B_POSITIVE, 1, testHospital, "Hyderabad",
                UrgencyLevel.LOW, "9876543212", "B positive case"
        );
        bloodRequestRepository.save(bReq);

        mockMvc.perform(get("/api/admin/requests")
                        .param("bloodGroup", "B+")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].bloodGroup").value("B+"));
    }

    @Test
    @DisplayName("Filter requests by urgency returns only matching records")
    void testFilterRequests_ByUrgency() throws Exception {
        mockMvc.perform(get("/api/admin/requests")
                        .param("urgency", "CRITICAL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].urgency").value("CRITICAL"));

        mockMvc.perform(get("/api/admin/requests")
                        .param("urgency", "LOW")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // ==========================================
    // 3. Inspection & Audit Trail
    // ==========================================

    @Test
    @DisplayName("GET /api/admin/requests/{id} returns full details with audit trail")
    void testGetRequestById_AsAdmin_Returns200WithFullDetailsAndAuditTrail() throws Exception {
        mockMvc.perform(get("/api/admin/requests/" + pendingRequest.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(pendingRequest.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.hospital.name").value("Apollo Speciality"))
                .andExpect(jsonPath("$.data.auditTrail", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.auditTrail[0].actorRole").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/admin/requests/{id} returns 404 for non-existent request ID")
    void testGetRequestById_NotFound_Returns404NotFound() throws Exception {
        mockMvc.perform(get("/api/admin/requests/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==========================================
    // 4. Allowed State Transitions
    // ==========================================

    @Test
    @DisplayName("Allowed Transition: PENDING -> APPROVED returns 200 OK and logs audit")
    void testTransition_PendingToApproved_Returns200AndRecordsAudit() throws Exception {
        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.APPROVED, "Verified with Apollo Blood Bank");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.auditTrail", hasSize(2)))
                .andExpect(jsonPath("$.data.auditTrail[1].newStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.auditTrail[1].actorRole").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Allowed Transition: PENDING -> REJECTED returns 200 OK and logs audit")
    void testTransition_PendingToRejected_Returns200AndRecordsAudit() throws Exception {
        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.REJECTED, "Unable to verify attendant documentation");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.auditTrail[1].newStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("Allowed Transition: PENDING -> CANCELLED returns 200 OK and logs audit")
    void testTransition_PendingToCancelled_Returns200AndRecordsAudit() throws Exception {
        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.CANCELLED, "Admin cancelled at hospital request");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Allowed Transition: APPROVED -> FULFILLED returns 200 OK and logs audit")
    void testTransition_ApprovedToFulfilled_Returns200AndRecordsAudit() throws Exception {
        // First transition PENDING -> APPROVED
        pendingRequest.transitionTo(RequestStatus.APPROVED);
        bloodRequestRepository.save(pendingRequest);

        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.FULFILLED, "Donation completed and units transfused");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FULFILLED"));
    }

    @Test
    @DisplayName("Allowed Transition: APPROVED -> REJECTED (justified) returns 200 OK")
    void testTransition_ApprovedToRejected_Returns200AndRecordsAudit() throws Exception {
        pendingRequest.transitionTo(RequestStatus.APPROVED);
        bloodRequestRepository.save(pendingRequest);

        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.REJECTED, "Hospital reported patient was discharged before transfusion");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Allowed Transition: APPROVED -> CANCELLED returns 200 OK")
    void testTransition_ApprovedToCancelled_Returns200AndRecordsAudit() throws Exception {
        pendingRequest.transitionTo(RequestStatus.APPROVED);
        bloodRequestRepository.save(pendingRequest);

        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.CANCELLED, "Patient arranged alternate donor");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ==========================================
    // 5. Disallowed State Transitions
    // ==========================================

    @Test
    @DisplayName("Disallowed Transition: FULFILLED -> APPROVED returns 400 Bad Request")
    void testTransition_FulfilledToApproved_Returns400BadRequest() throws Exception {
        pendingRequest.transitionTo(RequestStatus.APPROVED);
        pendingRequest.transitionTo(RequestStatus.FULFILLED);
        bloodRequestRepository.save(pendingRequest);

        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.APPROVED, "Attempting to reopen fulfilled request");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Invalid status transition")));
    }

    @Test
    @DisplayName("Disallowed Transition: FULFILLED -> REJECTED returns 400 Bad Request")
    void testTransition_FulfilledToRejected_Returns400BadRequest() throws Exception {
        pendingRequest.transitionTo(RequestStatus.APPROVED);
        pendingRequest.transitionTo(RequestStatus.FULFILLED);
        bloodRequestRepository.save(pendingRequest);

        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.REJECTED, "Invalid overwrite");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Disallowed Transition: REJECTED -> APPROVED returns 400 Bad Request")
    void testTransition_RejectedToApproved_Returns400BadRequest() throws Exception {
        pendingRequest.transitionTo(RequestStatus.REJECTED);
        bloodRequestRepository.save(pendingRequest);

        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.APPROVED, "Attempting to approve rejected request");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Disallowed Transition: CANCELLED -> APPROVED returns 400 Bad Request")
    void testTransition_CancelledToApproved_Returns400BadRequest() throws Exception {
        pendingRequest.transitionTo(RequestStatus.CANCELLED);
        bloodRequestRepository.save(pendingRequest);

        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.APPROVED, "Attempting to approve cancelled request");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Disallowed Transition: PENDING -> FULFILLED returns 400 Bad Request (cannot skip approval)")
    void testTransition_PendingToFulfilled_Returns400BadRequest() throws Exception {
        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.FULFILLED, "Skipping approval directly to fulfilled");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Invalid status transition")));
    }

    @Test
    @DisplayName("Disallowed Transition: PENDING -> PENDING returns 400 Bad Request (same status)")
    void testTransition_PendingToPending_Returns400BadRequest() throws Exception {
        AdminStatusUpdateRequest body = new AdminStatusUpdateRequest(RequestStatus.PENDING, "Same status transition");

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("same status")));
    }

    @Test
    @DisplayName("Arbitrary invalid status string returns 400 Bad Request")
    void testTransition_ArbitraryInvalidStatus_Returns400BadRequest() throws Exception {
        String invalidBody = "{\"status\":\"BOGUS_STATUS\",\"remarks\":\"arbitrary string\"}";

        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==========================================
    // 6. Checkpoint Verification
    // ==========================================

    @Test
    @DisplayName("Checkpoint: Administrator updates status in MySQL, changes persist and are visible to requester")
    void testCheckpoint_AdminUpdatesStatusPersistsAndIsVisibleToRequester() throws Exception {
        // 1. Initial State: Requester checks status -> PENDING
        mockMvc.perform(get("/api/requests/" + pendingRequest.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // 2. Administrator updates status to APPROVED
        AdminStatusUpdateRequest approveBody = new AdminStatusUpdateRequest(RequestStatus.APPROVED, "Verified by Medical Board");
        mockMvc.perform(patch("/api/admin/requests/" + pendingRequest.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 3. Requester queries their request endpoint directly -> immediately sees APPROVED status
        mockMvc.perform(get("/api/requests/" + pendingRequest.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        // 4. Requester queries their history endpoint (/api/requests/me) -> also sees APPROVED status
        mockMvc.perform(get("/api/requests/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));

        // 5. Verify direct database persistence
        BloodRequest inDb = bloodRequestRepository.findById(pendingRequest.getId()).orElse(null);
        assertNotNull(inDb);
        assertEquals(RequestStatus.APPROVED, inDb.getStatus());
        assertNotNull(inDb.getUpdatedAt());
    }
}
