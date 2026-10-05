package com.nutricare.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentRulesTest {
    @Test
    void appointmentsMoveForwardOnly() {
        assertEquals(List.of("CONFIRMED", "CANCELLED"), AppointmentRules.nextStatuses("PENDING"));
        assertTrue(AppointmentRules.canTransition("CONFIRMED", "COMPLETED"));
        assertTrue(AppointmentRules.canTransition("CONFIRMED", "NO_SHOW"));
        assertFalse(AppointmentRules.canTransition("PENDING", "COMPLETED"));
        assertFalse(AppointmentRules.canTransition("CANCELLED", "CONFIRMED"));
        assertFalse(AppointmentRules.canTransition("COMPLETED", "PENDING"));
    }

    @Test
    void cancellationsAndNoShowsNeedAReason() {
        assertTrue(AppointmentRules.requiresReason("CANCELLED"));
        assertTrue(AppointmentRules.requiresReason("NO_SHOW"));
        assertFalse(AppointmentRules.requiresReason("CONFIRMED"));
        assertEquals(30, AppointmentRules.SLOT_MINUTES);
    }
}
