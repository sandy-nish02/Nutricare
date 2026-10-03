package com.nutricare.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRulesTest {
    @Test
    void paymentsAreReviewedOnceAndOnlyVerifiedMoneyIsRefunded() {
        assertTrue(PaymentRules.canTransition("SUBMITTED", "VERIFIED"));
        assertTrue(PaymentRules.canTransition("SUBMITTED", "REJECTED"));
        assertTrue(PaymentRules.canTransition("VERIFIED", "REFUNDED"));
        assertFalse(PaymentRules.canTransition("SUBMITTED", "REFUNDED"));
        assertFalse(PaymentRules.canTransition("REJECTED", "VERIFIED"));
        assertFalse(PaymentRules.canTransition("VERIFIED", "REJECTED"));
        assertTrue(PaymentRules.nextStatuses("REFUNDED").isEmpty());
    }
}
