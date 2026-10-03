package com.nutricare.service;

import java.util.List;
import java.util.Map;

/** Payment state machine: a slip is verified or rejected once, and only verified money can be refunded. */
public final class PaymentRules {
    private static final Map<String, List<String>> NEXT = Map.of(
            "SUBMITTED", List.of("VERIFIED", "REJECTED"),
            "VERIFIED", List.of("REFUNDED"),
            "REJECTED", List.of(),
            "REFUNDED", List.of());

    private PaymentRules() { }

    public static List<String> nextStatuses(String current) {
        return NEXT.getOrDefault(current, List.of());
    }

    public static boolean canTransition(String current, String next) {
        return nextStatuses(current).contains(next);
    }
}
