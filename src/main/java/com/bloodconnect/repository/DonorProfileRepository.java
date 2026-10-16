package com.bloodconnect.repository;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long>, JpaSpecificationExecutor<DonorProfile> {
    Optional<DonorProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<DonorProfile> findByAvailableTrue();
    List<DonorProfile> findByBloodGroupAndAvailableTrue(BloodGroup bloodGroup);
    List<DonorProfile> findByCityIgnoreCaseAndAvailableTrue(String city);
    List<DonorProfile> findByBloodGroupAndCityIgnoreCaseAndAvailableTrue(BloodGroup bloodGroup, String city);
    long countByAvailableTrue();
    long countByActiveTrue();
    long countByActiveTrueAndAvailableTrue();
}
