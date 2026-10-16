package com.bloodconnect.repository;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.BloodRequest;
import com.bloodconnect.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long>, JpaSpecificationExecutor<BloodRequest> {
    List<BloodRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<BloodRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    List<BloodRequest> findByBloodGroupAndStatus(BloodGroup bloodGroup, RequestStatus status);
    boolean existsByRequesterIdAndBloodGroupAndHospitalIdAndStatus(Long requesterId, BloodGroup bloodGroup, Long hospitalId, RequestStatus status);
    long countByStatus(RequestStatus status);
    long countByHospitalId(Long hospitalId);
    boolean existsByHospitalId(Long hospitalId);
}
