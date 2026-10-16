package com.bloodconnect.specification;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.BloodRequest;
import com.bloodconnect.entity.RequestStatus;
import com.bloodconnect.entity.UrgencyLevel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BloodRequestSpecifications {

    /**
     * Builds dynamic JPA Specification for querying BloodRequest entities.
     *
     * @param status optional RequestStatus filter
     * @param bloodGroup optional BloodGroup filter
     * @param urgency optional UrgencyLevel filter
     * @param hospitalId optional hospital ID filter
     * @param startDate optional creation start date
     * @param endDate optional creation end date
     * @param search optional text search matching location, hospital name, or requester name
     * @return composite Specification
     */
    public static Specification<BloodRequest> withFilters(
            RequestStatus status,
            BloodGroup bloodGroup,
            UrgencyLevel urgency,
            Long hospitalId,
            LocalDate startDate,
            LocalDate endDate,
            String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (bloodGroup != null) {
                predicates.add(cb.equal(root.get("bloodGroup"), bloodGroup));
            }

            if (urgency != null) {
                predicates.add(cb.equal(root.get("urgency"), urgency));
            }

            if (hospitalId != null) {
                predicates.add(cb.equal(root.get("hospital").get("id"), hospitalId));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(LocalTime.MAX)));
            }

            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate locationMatch = cb.like(cb.lower(root.get("location")), term);
                Predicate hospitalMatch = cb.like(cb.lower(root.get("hospital").get("name")), term);
                Predicate requesterMatch = cb.like(cb.lower(root.get("requester").get("fullName")), term);
                predicates.add(cb.or(locationMatch, hospitalMatch, requesterMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
