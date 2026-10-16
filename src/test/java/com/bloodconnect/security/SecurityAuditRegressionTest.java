package com.bloodconnect.security;

import com.bloodconnect.dto.BloodRequestCreateRequest;
import com.bloodconnect.dto.DonorProfileRequest;
import com.bloodconnect.dto.HospitalRequest;
import com.bloodconnect.dto.RegisterRequest;
import com.bloodconnect.entity.*;
import com.bloodconnect.repository.BloodRequestRepository;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.HospitalRepository;
import com.bloodconnect.repository.UserRepository;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Session 15 Security Audit Regression Test Suite.
 * Validates authentication barriers, authorization boundaries, cross-user isolation,
 * mass assignment prevention, privacy leak mitigation, and safe error handling.
 */
@SpringBootTest
@Transactional
class SecurityAuditRegressionTest {

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

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User aliceUser;
    private User bobUser;
    private User adminUser;
    private String aliceToken;
    private String bobToken;
    private String adminToken;
    private Hospital sampleHospital;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        bloodRequestRepository.deleteAll();
        donorProfileRepository.deleteAll();
        hospitalRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Regular User Alice
        aliceUser = new User("Alice Walker", "alice.audit@example.com", passwordEncoder.encode("SecretPass123"), Role.ROLE_USER);
        aliceUser = userRepository.save(aliceUser);
        aliceToken = jwtUtil.generateToken(aliceUser.getEmail(), aliceUser.getRole().name(), aliceUser.getId());

        // 2. Regular User Bob
        bobUser = new User("Bob Smith", "bob.audit@example.com", passwordEncoder.encode("SecretPass123"), Role.ROLE_USER);
        bobUser = userRepository.save(bobUser);
        bobToken = jwtUtil.generateToken(bobUser.getEmail(), bobUser.getRole().name(), bobUser.getId());

        // 3. Admin User
        adminUser = new User("System Administrator", "admin.audit@example.com", passwordEncoder.encode("AdminPass123"), Role.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getRole().name(), adminUser.getId());

        // 4. Sample Hospital
        sampleHospital = new Hospital("General Hospital", "789 Health Blvd", "Hyderabad", "040-98765432", true);
        sampleHospital = hospitalRepository.save(sampleHospital);
    }

    // =========================================================================
    // 1. Unauthenticated Access Boundaries (HTTP 401 Unauthorized)
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated requests to protected user and admin endpoints return HTTP 401")
    void testUnauthenticatedRequestsReturn401() throws Exception {
        // Protected User endpoints
        mockMvc.perform(get("/api/requests/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/donors/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        // Protected Admin endpoints
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/requests"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/donors"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. Role Privilege Escalation Barriers (HTTP 403 Forbidden)
    // =========================================================================

    @Test
    @DisplayName("Standard users (ROLE_USER) cannot access administrative endpoints (HTTP 403)")
    void testStandardUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/admin/requests")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/admin/donors")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        HospitalRequest hospitalReq = new HospitalRequest("Malicious Hospital", "Street 1", "City", "1234567890", true);
        mockMvc.perform(post("/api/admin/hospitals")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hospitalReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 3. Ownership and Cross-User Isolation (HTTP 403 Forbidden)
    // =========================================================================

    @Test
    @DisplayName("Cross-user isolation: Bob cannot view or cancel Alice's blood request (HTTP 403)")
    void testCrossUserOwnershipIsolation() throws Exception {
        BloodRequest aliceRequest = new BloodRequest(
                aliceUser,
                BloodGroup.O_POSITIVE,
                2,
                sampleHospital,
                "Hyderabad",
                UrgencyLevel.CRITICAL,
                "9876543210",
                "Alice's private emergency"
        );
        aliceRequest = bloodRequestRepository.save(aliceRequest);

        // Bob attempts to view Alice's request by ID
        mockMvc.perform(get("/api/requests/" + aliceRequest.getId())
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Access denied")));

        // Bob attempts to cancel Alice's request
        mockMvc.perform(put("/api/requests/" + aliceRequest.getId() + "/cancel")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Alice can access her own request
        mockMvc.perform(get("/api/requests/" + aliceRequest.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(aliceRequest.getId()));

        // Admin has authorized override access
        mockMvc.perform(get("/api/requests/" + aliceRequest.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(aliceRequest.getId()));
    }

    // =========================================================================
    // 4. Mass Assignment Immunization
    // =========================================================================

    @Test
    @DisplayName("Registration prevents mass assignment of privileged ROLE_ADMIN")
    void testRegistrationPreventsRoleMassAssignment() throws Exception {
        String payload = """
                {
                    "fullName": "Attacker",
                    "email": "attacker@example.com",
                    "password": "StrongPassword123",
                    "role": "ROLE_ADMIN",
                    "roles": ["ROLE_ADMIN"],
                    "isAdmin": true
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_USER"));

        User savedUser = userRepository.findByEmail("attacker@example.com").orElseThrow();
        assertEquals(Role.ROLE_USER, savedUser.getRole());
    }

    @Test
    @DisplayName("Blood request creation prevents mass assignment of status and requesterId")
    void testBloodRequestCreationPreventsMassAssignment() throws Exception {
        String payload = String.format("""
                {
                    "bloodGroup": "A_POSITIVE",
                    "unitsRequired": 2,
                    "hospitalId": %d,
                    "location": "Hyderabad",
                    "urgency": "HIGH",
                    "contactNumber": "9876543210",
                    "notes": "Emergency blood needed",
                    "status": "APPROVED",
                    "requesterId": %d
                }
                """, sampleHospital.getId(), bobUser.getId());

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requesterName").value("Alice Walker"));
    }

    // =========================================================================
    // 5. Safe Error Responses (Zero Stack Traces / Secrets Exposed)
    // =========================================================================

    @Test
    @DisplayName("DTO validation failures return HTTP 400 with clean error list and no stack traces")
    void testValidationFailureSafeResponse() throws Exception {
        String invalidPayload = """
                {
                    "name": "",
                    "email": "not-an-email",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("Resource not found returns safe HTTP 404 without internal details")
    void testResourceNotFoundSafeResponse() throws Exception {
        mockMvc.perform(get("/api/hospitals/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("not found")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("Malformed JSON returns HTTP 400 safe response")
    void testMalformedJsonSafeResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed: json,,}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    // =========================================================================
    // 6. Privacy & Data Minimization
    // =========================================================================

    @Test
    @DisplayName("Public donor search strictly omits passwords, emails, and user IDs")
    void testPublicDonorSearchOmitsSensitiveData() throws Exception {
        DonorProfile profile = new DonorProfile(
                aliceUser,
                BloodGroup.O_POSITIVE,
                "Hyderabad",
                "Banjara Hills",
                "9876543210",
                true
        );
        profile.setLastDonationDate(LocalDate.now().minusMonths(4));
        donorProfileRepository.save(profile);

        mockMvc.perform(get("/api/donors?city=Hyderabad&bloodGroup=O+"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Alice Walker"))
                .andExpect(jsonPath("$.data.content[0].bloodGroup").value("O+"))
                .andExpect(jsonPath("$.data.content[0].city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.content[0].available").value(true))
                // Privacy checks:
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].user").doesNotExist());
    }

    // =========================================================================
    // 7. Request Status Transition Safeguards
    // =========================================================================

    @Test
    @DisplayName("Requester cannot cancel a request that is already CANCELLED or FULFILLED")
    void testInvalidStatusTransitionGuard() throws Exception {
        BloodRequest request = new BloodRequest(
                aliceUser,
                BloodGroup.B_POSITIVE,
                1,
                sampleHospital,
                "Hyderabad",
                UrgencyLevel.LOW,
                "9876543210",
                "Test request"
        );
        request = bloodRequestRepository.save(request);

        // Cancel first time -> succeeds
        mockMvc.perform(put("/api/requests/" + request.getId() + "/cancel")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Cancel second time -> rejected HTTP 400
        mockMvc.perform(put("/api/requests/" + request.getId() + "/cancel")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("CANCELLED")));
    }

    // =========================================================================
    // 8. Bounded Query & Pagination Guard
    // =========================================================================

    @Test
    @DisplayName("Pagination clamps excessive page sizes to maximum safe limit (100)")
    void testPaginationClampsExcessivePageSize() throws Exception {
        // Requesting size=500 should be accepted and clamped to <= 100 without database overload
        mockMvc.perform(get("/api/donors?size=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(100));
    }
}
