package com.nutricare.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Appointment state machine (FR12) and slot rules (FR11/FR13). */
public final class AppointmentRules {
    /** Every appointment reserves this many minutes of the staff member's and patient's time. */
    public static final int SLOT_MINUTES = 30;

    /** Statuses that still occupy a time slot. */
    public static final Set<String> OPEN = Set.of("PENDING", "CONFIRMED");

    private static final Map<String, List<String>> NEXT = Map.of(
            "PENDING", List.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", List.of("COMPLETED", "CANCELLED", "NO_SHOW"),
            "COMPLETED", List.of(),
            "CANCELLED", List.of(),
            "NO_SHOW", List.of());

    private AppointmentRules() { }

    public static List<String> nextStatuses(String current) {
        return NEXT.getOrDefault(current, List.of());
    }

    public static boolean canTransition(String current, String next) {
        return nextStatuses(current).contains(next);
    }

    public static boolean requiresReason(String status) {
        return "CANCELLED".equals(status) || "NO_SHOW".equals(status);
    }
}
