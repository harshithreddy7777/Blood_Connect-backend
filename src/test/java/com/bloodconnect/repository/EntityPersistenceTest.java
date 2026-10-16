package com.bloodconnect.repository;

import com.bloodconnect.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EntityPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Test
    @DisplayName("Should persist User and find by email successfully")
    void testUserPersistence() {
        User user = new User("John Doe", "john.doe@test.com", "hash_bcrypt_xyz", Role.ROLE_USER);
        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getCreatedAt());
        assertEquals("John Doe", savedUser.getFullName());
        assertEquals("john.doe@test.com", savedUser.getEmail());

        Optional<User> found = userRepository.findByEmail("john.doe@test.com");
        assertTrue(found.isPresent());
        assertEquals(savedUser.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Should enforce unique email constraint on User entity")
    void testUserUniqueEmailConstraint() {
        User user1 = new User("Alice One", "alice.unique@test.com", "hash1", Role.ROLE_USER);
        userRepository.saveAndFlush(user1);

        User user2 = new User("Alice Two", "alice.unique@test.com", "hash2", Role.ROLE_USER);
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    @DisplayName("Should persist DonorProfile linked 1:1 to User and query by availability")
    void testDonorProfilePersistenceAndSearch() {
        User donorUser = userRepository.save(
            new User("Bob Donor", "bob.donor@test.com", "hash_bob", Role.ROLE_USER)
        );

        DonorProfile profile = new DonorProfile(
            donorUser,
            BloodGroup.O_POSITIVE,
            "Hyderabad",
            "Madhapur",
            "+919876543210",
            true
        );
        profile.setLastDonationDate(LocalDate.now().minusMonths(4));
        DonorProfile savedProfile = donorProfileRepository.save(profile);

        assertNotNull(savedProfile.getId());
        assertEquals(donorUser.getId(), savedProfile.getUser().getId());
        assertEquals(BloodGroup.O_POSITIVE, savedProfile.getBloodGroup());
        assertTrue(savedProfile.isAvailable());

        // Test search query
        List<DonorProfile> foundDonors = donorProfileRepository
            .findByBloodGroupAndCityIgnoreCaseAndAvailableTrue(BloodGroup.O_POSITIVE, "Hyderabad");
        assertFalse(foundDonors.isEmpty());
        assertEquals("Hyderabad", foundDonors.get(0).getCity());
        assertEquals(BloodGroup.O_POSITIVE, foundDonors.get(0).getBloodGroup());
    }

    @Test
    @DisplayName("Should persist Hospital and search by city")
    void testHospitalPersistence() {
        Hospital hospital = new Hospital(
            "Apollo Health Center",
            "Jubilee Hills, Road No 36",
            "Hyderabad",
            "+914023456789"
        );
        Hospital saved = hospitalRepository.save(hospital);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        List<Hospital> hospitalsInCity = hospitalRepository.findByCityIgnoreCase("hyderabad");
        assertFalse(hospitalsInCity.isEmpty());
        assertTrue(hospitalsInCity.stream().anyMatch(h -> "Apollo Health Center".equals(h.getName())));
    }

    @Test
    @DisplayName("Should persist BloodRequest with relationships and validate status lifecycle")
    void testBloodRequestLifecycleAndTransitions() {
        User requester = userRepository.save(
            new User("Patient Caregiver", "caregiver@test.com", "hash_cg", Role.ROLE_USER)
        );
        Hospital hospital = hospitalRepository.save(
            new Hospital("Care Hospital", "Banjara Hills", "Hyderabad", "+914022223333")
        );

        BloodRequest request = new BloodRequest(
            requester,
            BloodGroup.B_POSITIVE,
            2,
            hospital,
            "Hyderabad",
            UrgencyLevel.CRITICAL,
            "+919123456780",
            "Urgent platelets and whole blood requisition"
        );

        BloodRequest saved = bloodRequestRepository.save(request);
        assertNotNull(saved.getId());
        assertEquals(RequestStatus.PENDING, saved.getStatus());
        assertEquals(2, saved.getUnitsRequired());
        assertEquals(requester.getId(), saved.getRequester().getId());
        assertEquals(hospital.getId(), saved.getHospital().getId());

        // Test valid state transition: PENDING -> APPROVED
        assertTrue(saved.getStatus().canTransitionTo(RequestStatus.APPROVED));
        saved.transitionTo(RequestStatus.APPROVED);
        assertEquals(RequestStatus.APPROVED, saved.getStatus());

        // Test valid state transition: APPROVED -> FULFILLED
        assertTrue(saved.getStatus().canTransitionTo(RequestStatus.FULFILLED));
        saved.transitionTo(RequestStatus.FULFILLED);
        assertEquals(RequestStatus.FULFILLED, saved.getStatus());

        // Test terminal state: FULFILLED cannot transition back to PENDING or APPROVED
        assertFalse(saved.getStatus().canTransitionTo(RequestStatus.PENDING));
        assertFalse(saved.getStatus().canTransitionTo(RequestStatus.APPROVED));
        assertThrows(IllegalStateException.class, () -> {
            saved.transitionTo(RequestStatus.PENDING);
        });
    }

    @Test
    @DisplayName("Should reject invalid direct transition from PENDING to FULFILLED")
    void testInvalidStatusTransition() {
        User requester = userRepository.save(
            new User("Requester Direct", "requester.direct@test.com", "hash_req", Role.ROLE_USER)
        );

        BloodRequest request = new BloodRequest(
            requester,
            BloodGroup.A_POSITIVE,
            1,
            null,
            "Hyderabad",
            UrgencyLevel.MEDIUM,
            "+919000000000",
            "Standard request"
        );
        BloodRequest saved = bloodRequestRepository.save(request);

        // PENDING cannot jump directly to FULFILLED without administrative approval
        assertFalse(saved.getStatus().canTransitionTo(RequestStatus.FULFILLED));
        assertThrows(IllegalStateException.class, () -> {
            saved.transitionTo(RequestStatus.FULFILLED);
        });
    }
}
