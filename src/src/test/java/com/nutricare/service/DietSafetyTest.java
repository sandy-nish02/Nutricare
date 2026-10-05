package com.nutricare.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DietSafetyTest {
    @Test
    void findsAllergiesIgnoringCase() {
        assertEquals(1, DietSafety.conflictingAllergies("Peanut, shellfish", "Breakfast: PEANUT butter toast").size());
    }

    @Test
    void allowsSafeMealText() {
        assertTrue(DietSafety.conflictingAllergies("peanut", "Rice, chicken, spinach").isEmpty());
    }

    @Test
    void acceptsSemicolonAndLineSeparatedAllergies() {
        assertEquals(2, DietSafety.conflictingAllergies("milk; shellfish\npeanut", "Shellfish soup with milk").size());
    }
}
