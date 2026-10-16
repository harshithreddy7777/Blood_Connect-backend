package com.bloodconnect.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * State machine definition for Blood Request lifecycle:
 * 
 * Allowed State Transitions:
 * - PENDING   -> APPROVED, REJECTED, CANCELLED
 * - APPROVED  -> FULFILLED, CANCELLED
 * - REJECTED  -> (Terminal State, no transitions)
 * - FULFILLED -> (Terminal State, no transitions)
 * - CANCELLED -> (Terminal State, no transitions)
 */
public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    FULFILLED,
    CANCELLED;

    private static final Map<RequestStatus, Set<RequestStatus>> VALID_TRANSITIONS = Map.of(
        PENDING, EnumSet.of(APPROVED, REJECTED, CANCELLED),
        APPROVED, EnumSet.of(FULFILLED, REJECTED, CANCELLED),
        REJECTED, Collections.emptySet(),
        FULFILLED, Collections.emptySet(),
        CANCELLED, Collections.emptySet()
    );

    /**
     * Validates whether transition from this status to the given next status is legally permitted.
     *
     * @param nextStatus the proposed target status
     * @return true if transition is permitted, false otherwise
     */
    public boolean canTransitionTo(RequestStatus nextStatus) {
        if (nextStatus == null) {
            return false;
        }
        Set<RequestStatus> allowed = VALID_TRANSITIONS.getOrDefault(this, Collections.emptySet());
        return allowed.contains(nextStatus);
    }
}
