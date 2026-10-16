package com.bloodconnect.repository;

import com.bloodconnect.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long>, JpaSpecificationExecutor<Hospital> {
    List<Hospital> findByActiveTrue();
    List<Hospital> findByCityIgnoreCase(String city);
    List<Hospital> findByCityIgnoreCaseAndActiveTrue(String city);
    Page<Hospital> findByActiveTrue(Pageable pageable);
    Page<Hospital> findByCityIgnoreCaseAndActiveTrue(String city, Pageable pageable);
    Page<Hospital> findByCityIgnoreCase(String city, Pageable pageable);
    Optional<Hospital> findByNameIgnoreCase(String name);
    Optional<Hospital> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);
    long countByActiveTrue();
}
