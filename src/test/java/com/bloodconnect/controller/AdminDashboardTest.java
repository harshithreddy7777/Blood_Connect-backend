package com.bloodconnect.controller;

import com.bloodconnect.dto.AdminDashboardResponse;
import com.bloodconnect.entity.*;
import com.bloodconnect.repository.*;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AdminDashboardTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

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
    private Hospital sampleHospital;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        requestStatusAuditRepository.deleteAll();
        bloodRequestRepository.deleteAll();
        donorProfileRepository.deleteAll();
        hospitalRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminUser = new User("System Administrator", "admin.dash." + suffix + "@bloodconnect.com",
                passwordEncoder.encode("Admin@123"), Role.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);

        regularUser = new User("Regular Patient", "patient.dash." + suffix + "@bloodconnect.com",
                passwordEncoder.encode("Patient@123"), Role.ROLE_USER);
        regularUser = userRepository.save(regularUser);

        adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getRole().name(), adminUser.getId());
        userToken = jwtUtil.generateToken(regularUser.getEmail(), regularUser.getRole().name(), regularUser.getId());

        sampleHospital = new Hospital("Apollo Jubilee Hills", "Road No 72", "Hyderabad", "040-23607777", true);
        sampleHospital = hospitalRepository.save(sampleHospital);
    }

    // ==========================================
    // 1. Authorization & Role Checks
    // ==========================================

    @Test
    @DisplayName("Admin retrieves summary statistics: GET /api/admin/dashboard returns 200 OK")
    void testGetDashboardSummary_AsAdmin_Returns200() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.totalBloodRequests").isNumber())
                .andExpect(jsonPath("$.data.pendingRequests").isNumber())
                .andExpect(jsonPath("$.data.approvedRequests").isNumber())
                .andExpect(jsonPath("$.data.fulfilledRequests").isNumber())
                .andExpect(jsonPath("$.data.totalDonors").isNumber())
                .andExpect(jsonPath("$.data.activeDonorProfiles").isNumber())
                .andExpect(jsonPath("$.data.availableDonors").isNumber())
                .andExpect(jsonPath("$.data.recentRequests").isArray())
                .andExpect(jsonPath("$.data.definitions.totalDonors").exists())
                .andExpect(jsonPath("$.data.definitions.activeDonorProfiles").exists())
                .andExpect(jsonPath("$.data.definitions.availableDonors").exists());
    }

    @Test
    @DisplayName("Non-admin user blocked from dashboard: returns 403 Forbidden")
    void testGetDashboardSummary_AsRegularUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request blocked from dashboard: returns 401 Unauthorized")
    void testGetDashboardSummary_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. Data Accuracy & Zero-Default Verification
    // ==========================================

    @Test
    @DisplayName("Summary statistics strictly match known database records with zero-defaults")
    void testDashboard_MatchesActualDatabaseRecords() throws Exception {
        // Seed 1 active & available donor
        DonorProfile donor1 = new DonorProfile(regularUser, BloodGroup.O_POSITIVE, "Hyderabad", "Banjara Hills", "+919888877771", true);
        donor1.setActive(true);
        donorProfileRepository.save(donor1);

        // Seed 1 user without donor profile
        User user3 = new User("Second User", "user3." + UUID.randomUUID() + "@test.com", passwordEncoder.encode("Pass@123"), Role.ROLE_USER);
        userRepository.save(user3);

        // Seed 2 blood requests: 1 PENDING, 1 APPROVED
        BloodRequest req1 = new BloodRequest(regularUser, BloodGroup.O_POSITIVE, 2, sampleHospital, "Hyderabad", UrgencyLevel.HIGH, "+919000000001", "Notes 1");
        req1.setStatus(RequestStatus.PENDING);
        bloodRequestRepository.save(req1);

        BloodRequest req2 = new BloodRequest(user3, BloodGroup.A_POSITIVE, 1, sampleHospital, "Hyderabad", UrgencyLevel.CRITICAL, "+919000000002", "Notes 2");
        req2.setStatus(RequestStatus.APPROVED);
        bloodRequestRepository.save(req2);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value((int) userRepository.count()))
                .andExpect(jsonPath("$.data.totalDonors").value(1))
                .andExpect(jsonPath("$.data.activeDonorProfiles").value(1))
                .andExpect(jsonPath("$.data.availableDonors").value(1))
                .andExpect(jsonPath("$.data.totalBloodRequests").value(2))
                .andExpect(jsonPath("$.data.pendingRequests").value(1))
                .andExpect(jsonPath("$.data.approvedRequests").value(1))
                .andExpect(jsonPath("$.data.fulfilledRequests").value(0))
                .andExpect(jsonPath("$.data.rejectedRequests").value(0))
                .andExpect(jsonPath("$.data.cancelledRequests").value(0))
                .andExpect(jsonPath("$.data.totalHospitals").value(1))
                .andExpect(jsonPath("$.data.activeHospitals").value(1))
                .andExpect(jsonPath("$.data.recentRequests", hasSize(2)));
    }

    // ==========================================
    // 3. Dynamic Updates After Operations (Checkpoint)
    // ==========================================

    @Test
    @DisplayName("Checkpoint: summary metrics update after records change in MySQL")
    void testCheckpoint_SummaryNumbersUpdateAfterOperations() throws Exception {
        // Initial state check
        MvcResult initialResult = mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // 1. Perform operation: Add a new blood request
        BloodRequest newReq = new BloodRequest(regularUser, BloodGroup.B_POSITIVE, 3, sampleHospital, "Secunderabad", UrgencyLevel.CRITICAL, "+919876543210", "Urgent transfusion");
        newReq.setStatus(RequestStatus.PENDING);
        bloodRequestRepository.save(newReq);

        // Verify counts immediately updated
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBloodRequests").value(1))
                .andExpect(jsonPath("$.data.pendingRequests").value(1))
                .andExpect(jsonPath("$.data.approvedRequests").value(0));

        // 2. Perform operation: Admin approves the blood request
        newReq.setStatus(RequestStatus.APPROVED);
        bloodRequestRepository.save(newReq);

        // Verify status distribution updated
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingRequests").value(0))
                .andExpect(jsonPath("$.data.approvedRequests").value(1))
                .andExpect(jsonPath("$.data.fulfilledRequests").value(0));

        // 3. Perform operation: Admin marks fulfilled
        newReq.setStatus(RequestStatus.FULFILLED);
        bloodRequestRepository.save(newReq);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvedRequests").value(0))
                .andExpect(jsonPath("$.data.fulfilledRequests").value(1));

        // 4. Perform operation: Register a donor profile
        DonorProfile newDonor = new DonorProfile(regularUser, BloodGroup.AB_POSITIVE, "Hyderabad", "Hitec City", "+919000111222", true);
        newDonor.setActive(true);
        donorProfileRepository.save(newDonor);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDonors").value(1))
                .andExpect(jsonPath("$.data.activeDonorProfiles").value(1))
                .andExpect(jsonPath("$.data.availableDonors").value(1));

        // 5. Perform operation: Donor marks unavailable (e.g. cooldown / busy)
        newDonor.setAvailable(false);
        donorProfileRepository.save(newDonor);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDonors").value(1))
                .andExpect(jsonPath("$.data.activeDonorProfiles").value(1))
                .andExpect(jsonPath("$.data.availableDonors").value(0));

        // 6. Perform operation: Deactivate donor profile
        newDonor.setActive(false);
        donorProfileRepository.save(newDonor);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDonors").value(1))
                .andExpect(jsonPath("$.data.activeDonorProfiles").value(0))
                .andExpect(jsonPath("$.data.availableDonors").value(0));
    }

    // ==========================================
    // 4. Deterministic Ordering & Privacy Redaction
    // ==========================================

    @Test
    @DisplayName("Recent requests use deterministic ordering (createdAt DESC, id DESC)")
    void testRecentRequests_DeterministicOrdering() throws Exception {
        BloodRequest r1 = new BloodRequest(regularUser, BloodGroup.O_POSITIVE, 1, sampleHospital, "Hyderabad", UrgencyLevel.LOW, "+919111111111", "Notes 1");
        r1.setStatus(RequestStatus.PENDING);
        r1 = bloodRequestRepository.save(r1);

        BloodRequest r2 = new BloodRequest(regularUser, BloodGroup.A_POSITIVE, 2, sampleHospital, "Hyderabad", UrgencyLevel.HIGH, "+919222222222", "Notes 2");
        r2.setStatus(RequestStatus.APPROVED);
        r2 = bloodRequestRepository.save(r2);

        BloodRequest r3 = new BloodRequest(regularUser, BloodGroup.B_POSITIVE, 3, sampleHospital, "Hyderabad", UrgencyLevel.CRITICAL, "+919333333333", "Notes 3");
        r3.setStatus(RequestStatus.PENDING);
        r3 = bloodRequestRepository.save(r3);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentRequests", hasSize(3)))
                .andExpect(jsonPath("$.data.recentRequests[0].id").value(r3.getId()))
                .andExpect(jsonPath("$.data.recentRequests[1].id").value(r2.getId()))
                .andExpect(jsonPath("$.data.recentRequests[2].id").value(r1.getId()));
    }

    @Test
    @DisplayName("Recent requests redact personal privacy fields (requester email, attendant phone, clinical notes)")
    void testRecentRequests_RedactsPersonalInformation() throws Exception {
        BloodRequest req = new BloodRequest(regularUser, BloodGroup.AB_NEGATIVE, 4, sampleHospital, "Hyderabad", UrgencyLevel.HIGH, "SECRET_PHONE_999", "CONFIDENTIAL_CLINICAL_NOTES");
        req.setStatus(RequestStatus.PENDING);
        bloodRequestRepository.save(req);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentRequests[0].bloodGroup").value("AB-"))
                .andExpect(jsonPath("$.data.recentRequests[0].unitsRequired").value(4))
                .andExpect(jsonPath("$.data.recentRequests[0].hospitalName").value("Apollo Jubilee Hills"))
                // Ensure private fields are NOT present
                .andExpect(jsonPath("$.data.recentRequests[0].requesterEmail").doesNotExist())
                .andExpect(jsonPath("$.data.recentRequests[0].contactNumber").doesNotExist())
                .andExpect(jsonPath("$.data.recentRequests[0].notes").doesNotExist())
                .andExpect(jsonPath("$.data.recentRequests[0].passwordHash").doesNotExist());
    }
}
