package com.nutricare.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    @Test
    void passwordsNeedEightCharactersALetterAndANumber() {
        assertEquals("Password1", Validation.password("Password1", "Password"));
        assertThrows(IllegalArgumentException.class, () -> Validation.password("password", "Password"));
        assertThrows(IllegalArgumentException.class, () -> Validation.password("12345678", "Password"));
        assertThrows(IllegalArgumentException.class, () -> Validation.password("Pass1", "Password"));
    }

    @Test
    void validatesPhonesAndBirthDates() {
        assertEquals("077 123 4567", Validation.phone("077 123 4567", true));
        assertEquals("+94771234567", Validation.phone("+94771234567", true));
        assertNull(Validation.phone("", false));
        assertThrows(IllegalArgumentException.class, () -> Validation.phone("call me", true));
        assertThrows(IllegalArgumentException.class, () -> Validation.dateOfBirth(LocalDate.now().plusDays(1).toString()));
        assertEquals(LocalDate.of(2000, 1, 31), Validation.dateOfBirth("2000-01-31"));
    }

    @Test
    void validatesEmailsAndMoney() {
        assertEquals("patient@nutricare.lk", Validation.email(" Patient@NutriCare.lk "));
        assertThrows(IllegalArgumentException.class, () -> Validation.email("not-an-email"));
        assertEquals(new BigDecimal("3500.00"), Validation.money("3500"));
        assertThrows(IllegalArgumentException.class, () -> Validation.money("-1"));
        assertThrows(IllegalArgumentException.class, () -> Validation.money("10.555"));
    }
}
