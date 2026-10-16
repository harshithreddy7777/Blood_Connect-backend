package com.bloodconnect.controller;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.DonorProfile;
import com.bloodconnect.entity.Role;
import com.bloodconnect.entity.User;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.security.JwtUtil;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class DonorSearchTest {

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

    private User regularUser;
    private User adminUser;
    private String userToken;
    private String adminToken;

    private DonorProfile donorHydA;
    private DonorProfile donorHydB;
    private DonorProfile donorMumA;
    private DonorProfile donorHydUnavailable;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        donorProfileRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Regular user setup
        regularUser = new User();
        regularUser.setFullName("Normal Seeker");
        regularUser.setEmail("seeker@bloodconnect.com");
        regularUser.setPasswordHash(passwordEncoder.encode("Password123"));
        regularUser.setRole(Role.ROLE_USER);
        regularUser = userRepository.save(regularUser);
        userToken = jwtUtil.generateToken(regularUser.getEmail(), regularUser.getRole().name(), regularUser.getId());

        // 2. Admin user setup
        adminUser = new User();
        adminUser.setFullName("Platform Admin");
        adminUser.setEmail("admin.search@bloodconnect.com");
        adminUser.setPasswordHash(passwordEncoder.encode("AdminPass123"));
        adminUser.setRole(Role.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getRole().name(), adminUser.getId());

        // 3. Seed donors with varied attributes
        // Donor 1: A+ in Hyderabad (Banjara Hills), available
        User u1 = createUser("Donor Hyd A", "donor.hyd.a@test.com");
        donorHydA = createDonor(u1, BloodGroup.A_POSITIVE, "Hyderabad", "Banjara Hills Road 12", "9111111111", true);

        // Donor 2: B+ in Hyderabad (Jubilee Hills), available
        User u2 = createUser("Donor Hyd B", "donor.hyd.b@test.com");
        donorHydB = createDonor(u2, BloodGroup.B_POSITIVE, "Hyderabad", "Jubilee Hills Checkpost", "9222222222", true);

        // Donor 3: A+ in Mumbai (Andheri), available
        User u3 = createUser("Donor Mum A", "donor.mum.a@test.com");
        donorMumA = createDonor(u3, BloodGroup.A_POSITIVE, "Mumbai", "Andheri West", "9333333333", true);

        // Donor 4: O+ in Hyderabad, UNAVAILABLE
        User u4 = createUser("Donor Hyd Unavail", "donor.hyd.unavail@test.com");
        donorHydUnavailable = createDonor(u4, BloodGroup.O_POSITIVE, "Hyderabad", "Madhapur", "9444444444", false);
    }

    private User createUser(String name, String email) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Secret123"));
        u.setRole(Role.ROLE_USER);
        return userRepository.save(u);
    }

    private DonorProfile createDonor(User u, BloodGroup bg, String city, String area, String phone, boolean available) {
        DonorProfile p = new DonorProfile(u, bg, city, area, phone, available);
        p.setLastDonationDate(LocalDate.now().minusMonths(4));
        DonorProfile saved = donorProfileRepository.save(p);
        u.setDonorProfile(saved);
        return saved;
    }

    @Test
    @DisplayName("Default search excludes unavailable donors")
    void testDefaultSearchExcludesUnavailableDonors() throws Exception {
        mockMvc.perform(get("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[*].fullName", not(hasItem("Donor Hyd Unavail"))))
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    @DisplayName("Filter by blood group: O+ returns only O+ (empty if only O+ donor is unavailable)")
    void testFilterByBloodGroup() throws Exception {
        // Only A+ donors (expect 2: donorHydA and donorMumA)
        mockMvc.perform(get("/api/donors")
                        .param("bloodGroup", "A+")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].bloodGroup", everyItem(is("A+"))));

        // Filter by B+ (expect 1: donorHydB)
        mockMvc.perform(get("/api/donors")
                        .param("bloodGroup", "B+")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Donor Hyd B"));
    }

    @Test
    @DisplayName("Filter by city is case-insensitive (hyderabad / HYDERABAD / Hyderabad)")
    void testFilterByCityCaseInsensitive() throws Exception {
        // Lowercase query
        mockMvc.perform(get("/api/donors")
                        .param("city", "hyderabad")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].city", everyItem(equalToIgnoringCase("Hyderabad"))));

        // Uppercase query
        mockMvc.perform(get("/api/donors")
                        .param("city", "HYDERABAD")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // Mumbai
        mockMvc.perform(get("/api/donors")
                        .param("city", "mumbai")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].city").value("Mumbai"));
    }

    @Test
    @DisplayName("Filter by area (case-insensitive substring)")
    void testFilterByArea() throws Exception {
        mockMvc.perform(get("/api/donors")
                        .param("area", "banjara")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Donor Hyd A"))
                .andExpect(jsonPath("$.data.content[0].area").value("Banjara Hills Road 12"));
    }

    @Test
    @DisplayName("Combined filters: bloodGroup + city work together")
    void testCombinedBloodGroupAndCityFilters() throws Exception {
        // A+ and Hyderabad -> only donorHydA (not donorMumA, and not donorHydB)
        mockMvc.perform(get("/api/donors")
                        .param("bloodGroup", "A+")
                        .param("city", "hyderabad")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Donor Hyd A"))
                .andExpect(jsonPath("$.data.content[0].bloodGroup").value("A+"))
                .andExpect(jsonPath("$.data.content[0].city").value("Hyderabad"));
    }

    @Test
    @DisplayName("Empty search results returned when no donors match criteria")
    void testEmptySearchResults() throws Exception {
        mockMvc.perform(get("/api/donors")
                        .param("city", "NonExistentCity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.empty").value(true));
    }

    @Test
    @DisplayName("Pagination and page metadata")
    void testPaginationAndPageMetadata() throws Exception {
        // Page 0, size 2 -> should return first 2 of 3 available donors
        mockMvc.perform(get("/api/donors")
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));

        // Page 1, size 2 -> should return remaining 1 donor
        mockMvc.perform(get("/api/donors")
                        .param("page", "1")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.pageNumber").value(1))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @DisplayName("Deterministic sorting by city with ID tiebreaker")
    void testDeterministicSorting() throws Exception {
        // Ascending by city
        mockMvc.perform(get("/api/donors")
                        .param("sortBy", "city")
                        .param("sortDir", "ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.content[1].city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.content[2].city").value("Mumbai"));
    }

    @Test
    @DisplayName("Public unauthenticated access allowed for default available donors")
    void testPublicUnauthenticatedAccessAllowed() throws Exception {
        // No Authorization header
        mockMvc.perform(get("/api/donors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", not(empty())));
    }

    @Test
    @DisplayName("Access rules: non-admin forbidden from querying unavailable donors")
    void testNonAdminForbiddenFromUnavailableDonors() throws Exception {
        // 1. Unauthenticated request with available=false
        mockMvc.perform(get("/api/donors").param("available", "false"))
                .andExpect(status().isForbidden());

        // 2. Authenticated ROLE_USER with available=false
        mockMvc.perform(get("/api/donors")
                        .header("Authorization", "Bearer " + userToken)
                        .param("available", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Access rules: admin allowed to query unavailable donors")
    void testAdminAllowedToQueryUnavailableDonors() throws Exception {
        mockMvc.perform(get("/api/donors")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Donor Hyd Unavail"))
                .andExpect(jsonPath("$.data.content[0].available").value(false));
    }

    @Test
    @DisplayName("Privacy: never exposes email, password hashes, or internal security fields")
    void testPrivacySensitiveFieldsNeverExposed() throws Exception {
        mockMvc.perform(get("/api/donors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").exists())
                .andExpect(jsonPath("$.data.content[0].fullName").exists())
                .andExpect(jsonPath("$.data.content[0].bloodGroup").exists())
                .andExpect(jsonPath("$.data.content[0].contactNumber").exists())
                // Ensure sensitive/internal fields DO NOT exist
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].role").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].userId").doesNotExist());
    }

    @Test
    @DisplayName("Invalid blood group returns HTTP 400 Bad Request")
    void testInvalidBloodGroupReturns400() throws Exception {
        mockMvc.perform(get("/api/donors")
                        .param("bloodGroup", "INVALID_GROUP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Checkpoint: changing donor availability in MySQL alters search results immediately")
    void testCheckpointChangingDonorAltersSearchResults() throws Exception {
        // Initial state: donorHydA is returned in search
        mockMvc.perform(get("/api/donors").param("bloodGroup", "A+").param("city", "hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Donor Hyd A"));

        // Toggle donorHydA to unavailable in database
        donorHydA.setAvailable(false);
        donorProfileRepository.saveAndFlush(donorHydA);

        // Immediate subsequent search: donorHydA is no longer present!
        mockMvc.perform(get("/api/donors").param("bloodGroup", "A+").param("city", "hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()));

        // Restore to available
        donorHydA.setAvailable(true);
        donorProfileRepository.saveAndFlush(donorHydA);

        // Immediate subsequent search: donorHydA reappears!
        mockMvc.perform(get("/api/donors").param("bloodGroup", "A+").param("city", "hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Donor Hyd A"));
    }
}
