package com.bloodconnect.integration;

import com.bloodconnect.dto.*;
import com.bloodconnect.entity.*;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.HospitalRepository;
import com.bloodconnect.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Session 16 End-to-End Integration Scenario Test.
 * Tests the complete multi-actor lifecycle against real MySQL database:
 * 1. Register a normal user.
 * 2. Log in.
 * 3. Create a donor profile.
 * 4. Search for the donor.
 * 5. Submit a blood request.
 * 6. Log in as ADMIN.
 * 7. Review and update the request.
 * 8. Log back in as the requesting user.
 * 9. Confirm the status change.
 * 10. Verify persistent data in MySQL.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndIntegrationScenarioTest {

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
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String userEmail = "scenario16.user@bloodconnect.test";
    private String userPassword = "UserPass123!";
    private String userFullName = "Integration User";

    private String adminEmail = "scenario16.admin@bloodconnect.test";
    private String adminPassword = "AdminPass123!";

    private String userToken;
    private String adminToken;
    private Hospital sampleHospital;
    private Long createdRequestId;

    @BeforeAll
    void init() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Ensure clean scenario accounts
        userRepository.findByEmail(userEmail).ifPresent(u -> {
            bloodRequestRepository.findByRequesterIdOrderByCreatedAtDesc(u.getId()).forEach(bloodRequestRepository::delete);
            donorProfileRepository.findByUserId(u.getId()).ifPresent(donorProfileRepository::delete);
            userRepository.delete(u);
        });
        userRepository.findByEmail(adminEmail).ifPresent(userRepository::delete);

        // Pre-create an active partner hospital
        sampleHospital = hospitalRepository.findByNameIgnoreCase("Scenario General Hospital")
                .orElseGet(() -> hospitalRepository.save(new Hospital(
                        "Scenario General Hospital",
                        "100 Medical Enclave",
                        "Hyderabad",
                        "040-55551234",
                        true
                )));

        // Pre-create scenario Admin user
        User admin = new User("Scenario Admin", adminEmail, passwordEncoder.encode(adminPassword), Role.ROLE_ADMIN);
        userRepository.save(admin);
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: Register a normal user via POST /api/auth/register")
    void step1_registerNormalUser() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(userFullName, userEmail, userPassword);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(userEmail))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_USER"));

        assertTrue(userRepository.findByEmail(userEmail).isPresent());
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Log in via POST /api/auth/login and obtain JWT")
    void step2_loginNormalUser() throws Exception {
        LoginRequest loginReq = new LoginRequest(userEmail, userPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = root.path("data").path("token").asText();
        assertNotNull(userToken);
        assertFalse(userToken.isBlank());
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Create a donor profile via POST /api/donors/me")
    void step3_createDonorProfile() throws Exception {
        DonorProfileRequest profileReq = new DonorProfileRequest(
                BloodGroup.O_POSITIVE,
                "Hyderabad",
                "Banjara Hills",
                "9876543210",
                LocalDate.now().minusMonths(3),
                true
        );

        mockMvc.perform(post("/api/donors/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.data.city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Search for the donor via GET /api/donors with combined filters")
    void step4_searchForDonor() throws Exception {
        mockMvc.perform(get("/api/donors?bloodGroup=O+&city=Hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[*].fullName", hasItem(userFullName)))
                .andExpect(jsonPath("$.data.content[*].city", everyItem(is("Hyderabad"))))
                .andExpect(jsonPath("$.data.content[*].bloodGroup", everyItem(is("O+"))));
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Submit a blood request via POST /api/requests")
    void step5_submitBloodRequest() throws Exception {
        BloodRequestCreateRequest req = new BloodRequestCreateRequest(
                BloodGroup.O_POSITIVE,
                2,
                sampleHospital.getId(),
                "Hyderabad",
                UrgencyLevel.CRITICAL,
                "9876543210",
                "Scenario 16 Emergency ICU requisition"
        );

        MvcResult result = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.hospitalName").value("Scenario General Hospital"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        createdRequestId = root.path("data").path("id").asLong();
        assertTrue(createdRequestId > 0);
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Log in as ADMIN via POST /api/auth/login")
    void step6_loginAsAdmin() throws Exception {
        LoginRequest adminLogin = new LoginRequest(adminEmail, adminPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_ADMIN"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = root.path("data").path("token").asText();
        assertNotNull(adminToken);
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Review and update the request status via PATCH /api/admin/requests/{id}/status")
    void step7_adminReviewAndUpdateRequest() throws Exception {
        // Admin reviews queue
        mockMvc.perform(get("/api/admin/requests?status=PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id", hasItem(createdRequestId.intValue())));

        // Admin updates status to APPROVED
        AdminStatusUpdateRequest statusUpdate = new AdminStatusUpdateRequest(
                RequestStatus.APPROVED,
                "Clinically verified with blood bank inventory."
        );

        mockMvc.perform(patch("/api/admin/requests/" + createdRequestId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Log back in as the requesting user")
    void step8_reloginRequestingUser() throws Exception {
        LoginRequest loginReq = new LoginRequest(userEmail, userPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = root.path("data").path("token").asText();
        assertNotNull(userToken);
    }

    @Test
    @Order(9)
    @DisplayName("Step 9: Confirm the status change in user's history (GET /api/requests/me)")
    void step9_confirmStatusChange() throws Exception {
        mockMvc.perform(get("/api/requests/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.id == " + createdRequestId + ")].status").value("APPROVED"));
    }

    @Test
    @Order(10)
    @DisplayName("Step 10: Verify persistent MySQL entities across the session")
    void step10_verifyPersistentDataInMySQL() {
        // Verify User persistence in MySQL
        User user = userRepository.findByEmail(userEmail).orElse(null);
        assertNotNull(user, "User must be persisted in MySQL");
        assertEquals(userFullName, user.getFullName());

        // Verify Donor Profile persistence in MySQL
        DonorProfile profile = donorProfileRepository.findByUserId(user.getId()).orElse(null);
        assertNotNull(profile, "DonorProfile must be persisted in MySQL");
        assertEquals(BloodGroup.O_POSITIVE, profile.getBloodGroup());
        assertEquals("Hyderabad", profile.getCity());

        // Verify Blood Request persistence and updated APPROVED status in MySQL
        BloodRequest req = bloodRequestRepository.findById(createdRequestId).orElse(null);
        assertNotNull(req, "BloodRequest must be persisted in MySQL");
        assertEquals(RequestStatus.APPROVED, req.getStatus(), "Status in MySQL must be APPROVED");
        assertEquals(user.getId(), req.getRequester().getId());
        assertEquals(sampleHospital.getId(), req.getHospital().getId());
    }
}
