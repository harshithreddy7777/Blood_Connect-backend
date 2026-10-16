package com.bloodconnect.controller;

import com.bloodconnect.dto.BloodRequestCreateRequest;
import com.bloodconnect.entity.*;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.HospitalRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class BloodRequestTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User userA;
    private User userB;
    private User adminUser;
    private String tokenA;
    private String tokenB;
    private String adminToken;

    private Hospital activeHospital;
    private Hospital inactiveHospital;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        bloodRequestRepository.deleteAll();
        hospitalRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Setup Users
        userA = new User("Patient Alpha", "patient.a@example.com", passwordEncoder.encode("Password123"), Role.ROLE_USER);
        userA = userRepository.save(userA);
        tokenA = jwtUtil.generateToken(userA.getEmail(), userA.getRole().name(), userA.getId());

        userB = new User("Patient Beta", "patient.b@example.com", passwordEncoder.encode("Password123"), Role.ROLE_USER);
        userB = userRepository.save(userB);
        tokenB = jwtUtil.generateToken(userB.getEmail(), userB.getRole().name(), userB.getId());

        adminUser = new User("System Admin", "admin.req@example.com", passwordEncoder.encode("AdminPass123"), Role.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getRole().name(), adminUser.getId());

        // 2. Setup Hospitals
        activeHospital = new Hospital("City Care Hospital", "123 Main Street", "Hyderabad", "040-11112222", true);
        activeHospital = hospitalRepository.save(activeHospital);

        inactiveHospital = new Hospital("Closed Clinic", "456 Old Bypass", "Secunderabad", "040-33334444", false);
        inactiveHospital = hospitalRepository.save(inactiveHospital);
    }

    @Test
    @DisplayName("Valid submission persists in MySQL with generated ID and PENDING status")
    void testValidSubmissionPersistsAndReturnsPending() throws Exception {
        BloodRequestCreateRequest req = new BloodRequestCreateRequest(
                BloodGroup.O_POSITIVE,
                2,
                activeHospital.getId(),
                "Hyderabad",
                UrgencyLevel.CRITICAL,
                "9876543210",
                "Patient in emergency ICU ward 4"
        );

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.data.unitsRequired").value(2))
                .andExpect(jsonPath("$.data.hospitalName").value("City Care Hospital"))
                .andExpect(jsonPath("$.data.requesterName").value("Patient Alpha"))
                .andExpect(jsonPath("$.data.urgency").value("CRITICAL"));

        assertEquals(1, bloodRequestRepository.count());
    }

    @Test
    @DisplayName("Invalid units required (< 1) returns HTTP 400 Bad Request")
    void testInvalidUnitsRejected() throws Exception {
        BloodRequestCreateRequest req = new BloodRequestCreateRequest(
                BloodGroup.A_POSITIVE,
                0, // invalid: must be >= 1
                activeHospital.getId(),
                "Hyderabad",
                UrgencyLevel.HIGH,
                "9876543210",
                "Invalid zero units"
        );

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Invalid non-existent hospital ID returns HTTP 404 Not Found")
    void testInvalidHospitalIdReturns404() throws Exception {
        BloodRequestCreateRequest req = new BloodRequestCreateRequest(
                BloodGroup.B_POSITIVE,
                1,
                999999L, // Non-existent hospital
                "Hyderabad",
                UrgencyLevel.MEDIUM,
                "9876543210",
                "Notes"
        );

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Inactive hospital returns HTTP 400 Bad Request")
    void testInactiveHospitalRejected() throws Exception {
        BloodRequestCreateRequest req = new BloodRequestCreateRequest(
                BloodGroup.AB_POSITIVE,
                1,
                inactiveHospital.getId(),
                "Secunderabad",
                UrgencyLevel.LOW,
                "9876543210",
                "Notes"
        );

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Missing authentication on POST /api/requests returns HTTP 401 Unauthorized")
    void testMissingAuthenticationReturns401() throws Exception {
        BloodRequestCreateRequest req = new BloodRequestCreateRequest(
                BloodGroup.O_NEGATIVE,
                1,
                activeHospital.getId(),
                "Hyderabad",
                UrgencyLevel.HIGH,
                "9876543210",
                "Unauthenticated"
        );

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Duplicate submission returns HTTP 409 Conflict")
    void testDuplicateSubmissionReturns409() throws Exception {
        BloodRequestCreateRequest req = new BloodRequestCreateRequest(
                BloodGroup.O_POSITIVE,
                1,
                activeHospital.getId(),
                "Hyderabad",
                UrgencyLevel.HIGH,
                "9876543210",
                "First request"
        );

        // 1st submission succeeds
        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // 2nd identical submission while 1st is still PENDING -> 409 Conflict
        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/requests/me shows only the requesting user's records")
    void testGetMyRequestsShowsOnlyCallerRecords() throws Exception {
        // User A creates 2 requests
        createSampleRequest(userA, BloodGroup.A_POSITIVE, activeHospital);
        createSampleRequest(userA, BloodGroup.B_POSITIVE, activeHospital);

        // User B creates 1 request
        createSampleRequest(userB, BloodGroup.O_POSITIVE, activeHospital);

        // User A fetches history -> exactly 2 requests
        mockMvc.perform(get("/api/requests/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].requesterName", everyItem(is("Patient Alpha"))));

        // User B fetches history -> exactly 1 request
        mockMvc.perform(get("/api/requests/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].requesterName").value("Patient Beta"));
    }

    @Test
    @DisplayName("Prevent access to another user's private request by ID guessing (HTTP 403 Forbidden)")
    void testPreventAccessToAnotherUserRequestByIdGuessing() throws Exception {
        BloodRequest reqA = createSampleRequest(userA, BloodGroup.A_NEGATIVE, activeHospital);

        // User B attempts to access User A's private request
        mockMvc.perform(get("/api/requests/" + reqA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // User A can access own request
        mockMvc.perform(get("/api/requests/" + reqA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reqA.getId()));

        // Admin can access any request
        mockMvc.perform(get("/api/requests/" + reqA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reqA.getId()));
    }

    @Test
    @DisplayName("User can cancel own pending request (transitions to CANCELLED)")
    void testUserCanCancelOwnPendingRequest() throws Exception {
        BloodRequest reqA = createSampleRequest(userA, BloodGroup.O_POSITIVE, activeHospital);
        assertEquals(RequestStatus.PENDING, reqA.getStatus());

        mockMvc.perform(put("/api/requests/" + reqA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Second attempt to cancel already cancelled request returns HTTP 400
        mockMvc.perform(put("/api/requests/" + reqA.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Checkpoint: submitted request is persisted in MySQL and appears in user's request history")
    void testCheckpointSubmittedRequestAppearsInUserHistory() throws Exception {
        BloodRequestCreateRequest req = new BloodRequestCreateRequest(
                BloodGroup.AB_NEGATIVE,
                3,
                activeHospital.getId(),
                "Hyderabad",
                UrgencyLevel.HIGH,
                "9998887776",
                "Emergency heart surgery"
        );

        // 1. Submit request
        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()));

        // 2. Immediately inspect personal request history
        mockMvc.perform(get("/api/requests/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bloodGroup").value("AB-"))
                .andExpect(jsonPath("$.data[0].unitsRequired").value(3))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].hospitalName").value("City Care Hospital"));
    }

    private BloodRequest createSampleRequest(User u, BloodGroup bg, Hospital h) {
        BloodRequest r = new BloodRequest(u, bg, 1, h, "Hyderabad", UrgencyLevel.HIGH, "9876543210", "Sample");
        return bloodRequestRepository.save(r);
    }
}
