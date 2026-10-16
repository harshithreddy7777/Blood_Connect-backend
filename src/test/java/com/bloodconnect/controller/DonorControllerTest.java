package com.bloodconnect.controller;

import com.bloodconnect.dto.AdminDonorUpdateRequest;
import com.bloodconnect.dto.DonorProfileRequest;
import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.DonorProfile;
import com.bloodconnect.entity.Role;
import com.bloodconnect.entity.User;
import com.bloodconnect.repository.DonorProfileRepository;
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
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DonorControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private User userA;
    private User userB;
    private User adminUser;
    private String tokenA;
    private String tokenB;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean up test data
        donorProfileRepository.deleteAll();
        userRepository.findByEmail("donor.a@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("donor.b@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("admin.donor@example.com").ifPresent(userRepository::delete);

        userA = userRepository.save(new User("User A", "donor.a@example.com", passwordEncoder.encode("Pass123"), Role.ROLE_USER));
        userB = userRepository.save(new User("User B", "donor.b@example.com", passwordEncoder.encode("Pass123"), Role.ROLE_USER));
        adminUser = userRepository.save(new User("Admin User", "admin.donor@example.com", passwordEncoder.encode("AdminPass123"), Role.ROLE_ADMIN));

        tokenA = jwtUtil.generateToken(userA.getEmail(), userA.getRole().name(), userA.getId());
        tokenB = jwtUtil.generateToken(userB.getEmail(), userB.getRole().name(), userB.getId());
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getRole().name(), adminUser.getId());
    }

    @Test
    @DisplayName("Should successfully create a donor profile for the authenticated user")
    void testCreateDonorProfileSuccess() throws Exception {
        DonorProfileRequest request = new DonorProfileRequest(
                BloodGroup.O_POSITIVE,
                "Hyderabad",
                "Hitech City",
                "+919876543210",
                LocalDate.now().minusMonths(3),
                true
        );

        mockMvc.perform(post("/api/donors/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.data.city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.fullName").value("User A"));
    }

    @Test
    @DisplayName("Should reject duplicate donor profile for the same user (HTTP 409)")
    void testCreateDuplicateProfileRejected() throws Exception {
        DonorProfile profile = new DonorProfile(userA, BloodGroup.A_POSITIVE, "Hyderabad", "Madhapur", "+919000000001", true);
        donorProfileRepository.save(profile);

        DonorProfileRequest duplicateRequest = new DonorProfileRequest(
                BloodGroup.A_POSITIVE, "Hyderabad", "Madhapur", "+919000000001", null, true
        );

        mockMvc.perform(post("/api/donors/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Should reject impossible future donation date (HTTP 400)")
    void testFutureDonationDateRejected() throws Exception {
        DonorProfileRequest futureRequest = new DonorProfileRequest(
                BloodGroup.B_POSITIVE,
                "Bangalore",
                "Indiranagar",
                "+919000000002",
                LocalDate.now().plusDays(5),
                true
        );

        mockMvc.perform(post("/api/donors/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should retrieve authenticated user's donor profile")
    void testGetMyProfileSuccess() throws Exception {
        DonorProfile profile = new DonorProfile(userA, BloodGroup.AB_POSITIVE, "Chennai", "Anna Nagar", "+919000000003", true);
        donorProfileRepository.save(profile);

        mockMvc.perform(get("/api/donors/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bloodGroup").value("AB+"))
                .andExpect(jsonPath("$.data.city").value("Chennai"));
    }

    @Test
    @DisplayName("Should return 404 when user has no donor profile")
    void testGetMyProfileNotFound() throws Exception {
        mockMvc.perform(get("/api/donors/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should update authenticated user's donor profile")
    void testUpdateMyProfileSuccess() throws Exception {
        DonorProfile profile = new DonorProfile(userA, BloodGroup.O_NEGATIVE, "Delhi", "Connaught Place", "+919000000004", true);
        donorProfileRepository.save(profile);

        DonorProfileRequest updateRequest = new DonorProfileRequest(
                BloodGroup.O_NEGATIVE,
                "Delhi",
                "South Extension",
                "+919999988888",
                LocalDate.now().minusMonths(1),
                false // Explicitly updated availability
        );

        mockMvc.perform(put("/api/donors/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.area").value("South Extension"))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @DisplayName("Should reject unauthenticated calls to /api/donors/me (HTTP 401)")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/donors/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Cross-user isolation: User B cannot modify User A's profile")
    void testCrossUserAccessBlocked() throws Exception {
        // User A creates a profile
        DonorProfile profileA = new DonorProfile(userA, BloodGroup.A_POSITIVE, "Hyderabad", "Banjara Hills", "+919111111111", true);
        donorProfileRepository.save(profileA);

        // User B tries to update their own profile, but doesn't have one yet
        DonorProfileRequest requestFromB = new DonorProfileRequest(
                BloodGroup.B_POSITIVE, "Mumbai", "Andheri", "+919222222222", null, true
        );

        // User B calling /api/donors/me cannot overwrite User A's profile
        mockMvc.perform(put("/api/donors/me")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestFromB)))
                .andExpect(status().isNotFound());

        // Verify User A's profile is unchanged
        DonorProfile freshProfileA = donorProfileRepository.findByUserId(userA.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Hyderabad", freshProfileA.getCity());
        org.junit.jupiter.api.Assertions.assertEquals(BloodGroup.A_POSITIVE, freshProfileA.getBloodGroup());
    }

    @Test
    @DisplayName("Admin can list and inspect all donor profiles")
    void testAdminCanListAndInspectDonors() throws Exception {
        DonorProfile profileA = new DonorProfile(userA, BloodGroup.A_POSITIVE, "Hyderabad", "Banjara Hills", "+919111111111", true);
        DonorProfile saved = donorProfileRepository.save(profileA);

        mockMvc.perform(get("/api/admin/donors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/admin/donors/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(saved.getId()));
    }

    @Test
    @DisplayName("Non-admin user blocked from administrative donor endpoints (HTTP 403)")
    void testNonAdminBlockedFromAdminDonorEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/donors")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Admin can safely remove donor profile without deleting user account")
    void testAdminDeleteDonorSafely() throws Exception {
        DonorProfile profileA = new DonorProfile(userA, BloodGroup.A_POSITIVE, "Hyderabad", "Banjara Hills", "+919111111111", true);
        DonorProfile saved = donorProfileRepository.save(profileA);

        mockMvc.perform(delete("/api/admin/donors/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Donor profile is deleted
        assertFalse(donorProfileRepository.existsById(saved.getId()));

        // User A still exists in the database!
        assertTrue(userRepository.existsById(userA.getId()));
    }
}
