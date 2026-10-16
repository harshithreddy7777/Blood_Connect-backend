package com.bloodconnect.specification;

import com.bloodconnect.entity.BloodGroup;
import com.bloodconnect.entity.DonorProfile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DonorSpecifications {

    /**
     * Builds a composite JPA Specification dynamically combining all supplied filters.
     *
     * @param bloodGroup optional BloodGroup enum filter
     * @param city optional city filter (evaluated case-insensitively)
     * @param area optional area/locality filter (evaluated case-insensitively)
     * @param available optional availability flag
     * @return Specification for querying DonorProfile entities
     */
    public static Specification<DonorProfile> withFilters(
            BloodGroup bloodGroup,
            String city,
            String area,
            Boolean available) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (bloodGroup != null) {
                predicates.add(cb.equal(root.get("bloodGroup"), bloodGroup));
            }

            if (city != null && !city.trim().isEmpty()) {
                // Case-insensitive exact city matching
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
            }

            if (area != null && !area.trim().isEmpty()) {
                // Case-insensitive substring matching for area/address
                predicates.add(cb.like(cb.lower(root.get("area")), "%" + area.trim().toLowerCase() + "%"));
            }

            if (available != null) {
                predicates.add(cb.equal(root.get("available"), available));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
