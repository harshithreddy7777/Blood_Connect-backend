package com.bloodconnect.config;

import com.bloodconnect.entity.Role;
import com.bloodconnect.entity.User;
import com.bloodconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initial Administrator Provisioning Setup:
 * 
 * Configurable via environment variables:
 * - ADMIN_EMAIL (default: admin@bloodconnect.com)
 * - ADMIN_PASSWORD (default: Admin@123)
 * - ADMIN_NAME (default: System Administrator)
 * 
 * This runner checks if any administrative user exists in the database.
 * If none exists, it provisions an initial admin account with a BCrypt-hashed password.
 * If an admin already exists, provisioning is safely skipped.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final com.bloodconnect.repository.HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:${ADMIN_EMAIL:admin@bloodconnect.com}}")
    private String adminEmail;

    @Value("${app.admin.password:${ADMIN_PASSWORD:Admin@123}}")
    private String adminPassword;

    @Value("${app.admin.name:${ADMIN_NAME:System Administrator}}")
    private String adminName;

    public DataInitializer(UserRepository userRepository,
                           com.bloodconnect.repository.HospitalRepository hospitalRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        provisionAdmin();
        provisionHospitals();
    }

    private void provisionAdmin() {
        List<User> existingAdmins = userRepository.findByRole(Role.ROLE_ADMIN);

        if (existingAdmins.isEmpty()) {
            log.info("No administrative accounts found in database. Provisioning initial administrator: {}", adminEmail);

            // If an account with this email exists as a regular user, update its role
            User admin = userRepository.findByEmail(adminEmail.toLowerCase())
                    .orElseGet(() -> new User(
                            adminName,
                            adminEmail.toLowerCase(),
                            passwordEncoder.encode(adminPassword),
                            Role.ROLE_ADMIN
                    ));

            admin.setRole(Role.ROLE_ADMIN);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);

            log.info("Initial administrator account successfully provisioned for: {}", adminEmail);
        } else {
            log.info("Existing administrator account detected. Skipping initial admin provisioning.");
        }
    }

    private void provisionHospitals() {
        if (hospitalRepository.count() == 0) {
            log.info("Seeding initial accredited partner hospitals...");
            hospitalRepository.saveAll(List.of(
                    new com.bloodconnect.entity.Hospital("Apollo Health City", "Road No. 72, Jubilee Hills", "Hyderabad", "040-23607777", true),
                    new com.bloodconnect.entity.Hospital("Care Hospital", "Road No. 1, Banjara Hills", "Hyderabad", "040-61656565", true),
                    new com.bloodconnect.entity.Hospital("Yashoda Hospital", "Raj Bhavan Road, Somajiguda", "Hyderabad", "040-45674567", true),
                    new com.bloodconnect.entity.Hospital("Fortis Hospital", "Mulund Goregaon Link Road", "Mumbai", "022-67994444", true),
                    new com.bloodconnect.entity.Hospital("AIIMS Super Specialty", "Sri Aurobindo Marg, Ansari Nagar", "New Delhi", "011-26588500", true)
            ));
            log.info("Accredited partner hospitals seeded successfully.");
        }
    }
}
