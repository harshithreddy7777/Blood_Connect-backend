package com.bloodconnect.controller;

import com.bloodconnect.dto.BloodRequestCreateRequest;
import com.bloodconnect.dto.HospitalRequest;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class AdminHospitalTest {

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
    private Hospital sampleHospital;

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

        // Admin User
        adminUser = new User("System Admin", "admin.hosp@example.com", passwordEncoder.encode("AdminPass123"), Role.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), adminUser.getRole().name(), adminUser.getId());

        // Regular User
        regularUser = new User("Patient User", "user.hosp@example.com", passwordEncoder.encode("UserPass123"), Role.ROLE_USER);
        regularUser = userRepository.save(regularUser);
        userToken = jwtUtil.generateToken(regularUser.getEmail(), regularUser.getRole().name(), regularUser.getId());

        // Baseline Hospital
        sampleHospital = new Hospital("Sunshine Multi-Speciality", "MG Road Secunderabad", "Secunderabad", "040-27801234", true);
        sampleHospital = hospitalRepository.save(sampleHospital);
    }

    // ==========================================
    // 1. Role Authorization & Authentication
    // ==========================================

    @Test
    @DisplayName("Admin creates partner hospital: POST /api/admin/hospitals returns 201 Created")
    void testCreateHospital_AsAdmin_Returns201Created() throws Exception {
        HospitalRequest body = new HospitalRequest(
                "Rainbow Childrens Hospital",
                "Road No 2 Banjara Hills",
                "Hyderabad",
                "040-44665555",
                true
        );

        mockMvc.perform(post("/api/admin/hospitals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Rainbow Childrens Hospital"))
                .andExpect(jsonPath("$.data.city").value("Hyderabad"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("Non-admin user blocked from creating hospital: returns 403 Forbidden")
    void testCreateHospital_AsUser_Returns403Forbidden() throws Exception {
        HospitalRequest body = new HospitalRequest(
                "Unauthorized Hospital",
                "Test Address",
                "Hyderabad",
                "040-12345678",
                true
        );

        mockMvc.perform(post("/api/admin/hospitals")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Unauthenticated caller blocked from creating hospital: returns 401 Unauthorized")
    void testCreateHospital_Unauthenticated_Returns401Unauthorized() throws Exception {
        HospitalRequest body = new HospitalRequest("Unauth Hospital", "Address", "City", "040-1111111", true);

        mockMvc.perform(post("/api/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. Validation Constraints
    // ==========================================

    @Test
    @DisplayName("Hospital creation rejects blank mandatory fields: returns 400 Bad Request")
    void testCreateHospital_InvalidInput_Returns400BadRequest() throws Exception {
        HospitalRequest invalidBody = new HospitalRequest("", "", "", "", true);

        mockMvc.perform(post("/api/admin/hospitals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==========================================
    // 3. Retrieval (Public & Admin)
    // ==========================================

    @Test
    @DisplayName("Public inspection of single hospital: GET /api/hospitals/{id} returns 200 OK")
    void testGetHospitalById_Returns200() throws Exception {
        mockMvc.perform(get("/api/hospitals/" + sampleHospital.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sunshine Multi-Speciality"))
                .andExpect(jsonPath("$.data.city").value("Secunderabad"));
    }

    @Test
    @DisplayName("Single hospital not found returns 404")
    void testGetHospitalById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/hospitals/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Predictable pagination and sorting on hospital directory")
    void testGetAllHospitals_PagedAndSorted_Returns200() throws Exception {
        Hospital hosp2 = new Hospital("Alpha Heart Center", "Somajiguda", "Hyderabad", "040-23314455", true);
        hospitalRepository.save(hosp2);

        mockMvc.perform(get("/api/hospitals")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.content[0].name").value("Alpha Heart Center"));
    }

    // ==========================================
    // 4. Update & Status Toggle
    // ==========================================

    @Test
    @DisplayName("Admin updates hospital details: PUT /api/admin/hospitals/{id} returns 200 OK")
    void testUpdateHospital_AsAdmin_Returns200() throws Exception {
        HospitalRequest updateBody = new HospitalRequest(
                "Sunshine Hospitals Renamed",
                "New Address Secunderabad",
                "Secunderabad",
                "040-99998888",
                true
        );

        mockMvc.perform(put("/api/admin/hospitals/" + sampleHospital.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sunshine Hospitals Renamed"))
                .andExpect(jsonPath("$.data.contactNumber").value("040-99998888"));
    }

    @Test
    @DisplayName("Admin toggles hospital active status: PATCH /api/admin/hospitals/{id}/status returns 200 OK")
    void testToggleHospitalStatus_AsAdmin_Returns200() throws Exception {
        mockMvc.perform(patch("/api/admin/hospitals/" + sampleHospital.getId() + "/status")
                        .param("active", "false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        Hospital inDb = hospitalRepository.findById(sampleHospital.getId()).orElse(null);
        assertNotNull(inDb);
        assertFalse(inDb.isActive());
    }

    // ==========================================
    // 5. Deletion vs Safe Deactivation Fallback
    // ==========================================

    @Test
    @DisplayName("Deleting unreferenced hospital permanently deletes it from database")
    void testDeleteHospital_Unreferenced_PermanentlyDeleted() throws Exception {
        Hospital unreferenced = new Hospital("Temporary Clinic", "Banjara Hills", "Hyderabad", "040-7777777", true);
        unreferenced = hospitalRepository.save(unreferenced);

        mockMvc.perform(delete("/api/admin/hospitals/" + unreferenced.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deactivated").value(false));

        assertFalse(hospitalRepository.existsById(unreferenced.getId()));
    }

    @Test
    @DisplayName("Deleting hospital referenced by historical blood request triggers safe deactivation to preserve history")
    void testDeleteHospital_ReferencedByBloodRequest_SafelyDeactivatedAndHistoryIntact() throws Exception {
        // Create blood request linked to sampleHospital
        BloodRequest req = new BloodRequest(
                regularUser,
                BloodGroup.A_POSITIVE,
                2,
                sampleHospital,
                "Secunderabad",
                UrgencyLevel.HIGH,
                "9876543210",
                "Historical cardiac requisition"
        );
        req = bloodRequestRepository.save(req);

        // Attempt deletion of sampleHospital
        mockMvc.perform(delete("/api/admin/hospitals/" + sampleHospital.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deactivated").value(true))
                .andExpect(jsonPath("$.message", containsString("safely deactivated rather than deleted")));

        // Hospital still exists in database, but is inactive
        Hospital hospitalInDb = hospitalRepository.findById(sampleHospital.getId()).orElse(null);
        assertNotNull(hospitalInDb);
        assertFalse(hospitalInDb.isActive());

        // Historical blood request remains completely intact with hospital reference
        BloodRequest reqInDb = bloodRequestRepository.findById(req.getId()).orElse(null);
        assertNotNull(reqInDb);
        assertEquals(sampleHospital.getId(), reqInDb.getHospital().getId());
        assertEquals("Sunshine Multi-Speciality", reqInDb.getHospital().getName());
    }

    // ==========================================
    // 6. Request Submission with Active Hospitals Check
    // ==========================================

    @Test
    @DisplayName("Submitting request with inactive hospital is rejected with 400 Bad Request")
    void testCreateBloodRequest_WithInactiveHospital_Returns400BadRequest() throws Exception {
        // Deactivate sampleHospital
        sampleHospital.setActive(false);
        hospitalRepository.save(sampleHospital);

        BloodRequestCreateRequest requestBody = new BloodRequestCreateRequest(
                BloodGroup.O_POSITIVE,
                2,
                sampleHospital.getId(),
                "Secunderabad",
                UrgencyLevel.HIGH,
                "9876543210",
                "Attempt to requisition at inactive center"
        );

        mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("inactive or not accredited")));
    }

    // ==========================================
    // 7. Checkpoint Verification
    // ==========================================

    @Test
    @DisplayName("Checkpoint: Hospital managed through APIs and existing request history remains intact")
    void testCheckpoint_HospitalManagedAndRequestHistoryIntact() throws Exception {
        // 1. User submits requisition referencing active hospital
        BloodRequest req = new BloodRequest(
                regularUser,
                BloodGroup.B_POSITIVE,
                1,
                sampleHospital,
                "Secunderabad",
                UrgencyLevel.CRITICAL,
                "9876543210",
                "Patient ICU transfer requisition"
        );
        req = bloodRequestRepository.save(req);

        // 2. Admin inspects hospital
        mockMvc.perform(get("/api/admin/hospitals/" + sampleHospital.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sunshine Multi-Speciality"));

        // 3. Admin updates hospital contact details
        HospitalRequest updateDto = new HospitalRequest(
                "Sunshine Multi-Speciality (Updated)",
                "MG Road Secunderabad",
                "Secunderabad",
                "040-88887777",
                true
        );
        mockMvc.perform(put("/api/admin/hospitals/" + sampleHospital.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sunshine Multi-Speciality (Updated)"));

        // 4. Admin tries to delete hospital -> Safe deactivation preserves historical request
        mockMvc.perform(delete("/api/admin/hospitals/" + sampleHospital.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deactivated").value(true));

        // 5. Requester queries their request -> hospital name and details remain fully accessible
        mockMvc.perform(get("/api/requests/" + req.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hospitalId").value(sampleHospital.getId()))
                .andExpect(jsonPath("$.data.hospitalName").value("Sunshine Multi-Speciality (Updated)"));
    }
}
