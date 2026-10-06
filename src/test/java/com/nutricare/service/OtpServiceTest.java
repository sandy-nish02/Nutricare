package com.nutricare.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OtpServiceTest {
    @Test
    void otpCodesAreSixDigitsAndCompareByHash() {
        String code = OtpService.generate();
        assertTrue(code.matches("\\d{6}"));
        assertEquals(64, OtpService.hash(code).length(), "SHA-256 hex digest; the plain code is never stored");
        assertTrue(OtpService.sameHash(OtpService.hash(code), OtpService.hash(code)));
        assertFalse(OtpService.sameHash(OtpService.hash("123456"), OtpService.hash("654321")));
    }

    @Test
    void codesExpireAfterFiveMinutesAndFiveAttempts() {
        assertEquals(5, OtpService.VALIDITY_MINUTES);
        assertEquals(5, OtpService.MAX_ATTEMPTS);
    }
}
