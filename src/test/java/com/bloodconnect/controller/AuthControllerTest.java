package com.bloodconnect.controller;

import com.bloodconnect.dto.LoginRequest;
import com.bloodconnect.dto.RegisterRequest;
import com.bloodconnect.entity.Role;
import com.bloodconnect.entity.User;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Ensure test user is clean
        userRepository.findByEmail("auth.test@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("admin.test@example.com").ifPresent(userRepository::delete);
        userRepository.findByEmail("duplicate@example.com").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Should successfully register a new user and return JWT and safe DTO")
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", "auth.test@example.com", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("auth.test@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should reject registration with duplicate email (HTTP 409)")
    void testDuplicateEmailRegistration() throws Exception {
        User existing = new User("Existing User", "duplicate@example.com", passwordEncoder.encode("Pass123"), Role.ROLE_USER);
        userRepository.save(existing);

        RegisterRequest duplicateRequest = new RegisterRequest("New Person", "duplicate@example.com", "AnotherPass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("Should reject invalid registration inputs with validation errors (HTTP 400)")
    void testInvalidRegistrationInput() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "invalid-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Should successfully login and return JWT for valid credentials")
    void testSuccessfulLogin() throws Exception {
        User user = new User("Login User", "auth.test@example.com", passwordEncoder.encode("ValidPassword"), Role.ROLE_USER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("auth.test@example.com", "ValidPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("auth.test@example.com"));
    }

    @Test
    @DisplayName("Should reject login with invalid password (HTTP 401)")
    void testLoginWithInvalidPassword() throws Exception {
        User user = new User("Login User", "auth.test@example.com", passwordEncoder.encode("ValidPassword"), Role.ROLE_USER);
        userRepository.save(user);

        LoginRequest badRequest = new LoginRequest("auth.test@example.com", "WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Should fetch current user profile when valid JWT is supplied")
    void testGetCurrentUserWithValidToken() throws Exception {
        User user = new User("Current User", "auth.test@example.com", passwordEncoder.encode("Password123"), Role.ROLE_USER);
        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole().name(), saved.getId());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("auth.test@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Current User"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should reject access to /api/auth/me when token is missing (HTTP 401)")
    void testMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should reject access to /api/auth/me with invalid or tampered token (HTTP 401)")
    void testMeWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.token.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should reject access to /api/auth/me with expired token (HTTP 401)")
    void testMeWithExpiredToken() throws Exception {
        User user = new User("Expired User", "auth.test@example.com", passwordEncoder.encode("Password123"), Role.ROLE_USER);
        User saved = userRepository.save(user);

        // Generate token expired 5 seconds ago
        String expiredToken = jwtUtil.generateTokenWithCustomExpiration(
                saved.getEmail(), saved.getRole().name(), saved.getId(), -5000L
        );

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should enforce role authorization: USER token blocked from ADMIN endpoint (HTTP 403)")
    void testRoleEnforcementUserBlockedFromAdmin() throws Exception {
        User normalUser = new User("Regular User", "auth.test@example.com", passwordEncoder.encode("Password123"), Role.ROLE_USER);
        User saved = userRepository.save(normalUser);

        String userToken = jwtUtil.generateToken(saved.getEmail(), saved.getRole().name(), saved.getId());

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Access Denied")));
    }

    @Test
    @DisplayName("Should allow ADMIN token to access ADMIN endpoint (HTTP 200)")
    void testRoleEnforcementAdminAllowed() throws Exception {
        User adminUser = new User("Admin User", "admin.test@example.com", passwordEncoder.encode("AdminPass123"), Role.ROLE_ADMIN);
        User saved = userRepository.save(adminUser);

        String adminToken = jwtUtil.generateToken(saved.getEmail(), saved.getRole().name(), saved.getId());

        mockMvc.perform(get("/api/admin/ping")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access").value("GRANTED"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }
}
