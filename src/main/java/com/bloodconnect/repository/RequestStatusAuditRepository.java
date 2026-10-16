package com.bloodconnect.repository;

import com.bloodconnect.entity.RequestStatusAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestStatusAuditRepository extends JpaRepository<RequestStatusAudit, Long> {
    List<RequestStatusAudit> findByBloodRequestIdOrderByChangedAtAsc(Long requestId);
}
