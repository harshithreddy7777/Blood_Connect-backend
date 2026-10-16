package com.bloodconnect.service;

import com.bloodconnect.dto.AuthResponse;
import com.bloodconnect.dto.LoginRequest;
import com.bloodconnect.dto.RegisterRequest;
import com.bloodconnect.dto.UserSummaryResponse;
import com.bloodconnect.entity.Role;
import com.bloodconnect.entity.User;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.exception.ResourceNotFoundException;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Reject duplicate emails
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email address is already registered: " + normalizedEmail);
        }

        // 2. Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Guarantee that public registration can ONLY create ROLE_USER
        User user = new User(
                request.getFullName().trim(),
                normalizedEmail,
                hashedPassword,
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);

        // 4. Generate JWT
        String token = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getId()
        );

        return new AuthResponse(token, UserSummaryResponse.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return new AuthResponse(token, UserSummaryResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return UserSummaryResponse.fromEntity(user);
    }
}
