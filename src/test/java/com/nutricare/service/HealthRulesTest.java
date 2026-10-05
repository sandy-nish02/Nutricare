package com.nutricare.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthRulesTest {
    @Test
    void calculatesBmiToTwoDecimalPlaces() {
        assertEquals(22.86, HealthRules.bmi(70, 175));
    }

    @Test
    void rejectsImpossibleMeasurements() {
        assertThrows(IllegalArgumentException.class, () -> HealthRules.bmi(0, 175));
        assertThrows(IllegalArgumentException.class, () -> HealthRules.bmi(70, 0));
    }

    @Test
    void identifiesAbnormalReadings() {
        assertTrue(HealthRules.abnormal(31, 120, 80, 100.0));
        assertTrue(HealthRules.abnormal(22, 145, 95, 100.0));
        assertFalse(HealthRules.abnormal(22, 120, 80, 100.0));
    }

    @Test
    void judgesEachVitalReadingOnItsOwn() {
        assertTrue(HealthRules.abnormal(22, 200, null, null), "systolic alone");
        assertTrue(HealthRules.abnormal(22, null, 95, null), "diastolic alone");
        assertTrue(HealthRules.abnormal(22, null, null, 160.0), "blood sugar alone");
        assertFalse(HealthRules.abnormal(22, null, null, null), "no vitals taken");
        assertEquals("Overweight", HealthRules.bmiLabel(27.3));
    }

    @Test
    void flagsFivePercentWeightChange() {
        assertTrue(HealthRules.significantWeightChange(80, 84));
        assertFalse(HealthRules.significantWeightChange(80, 82));
    }
}
