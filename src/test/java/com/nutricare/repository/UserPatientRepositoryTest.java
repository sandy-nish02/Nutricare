package com.nutricare.repository;

import com.nutricare.service.OtpService;
import com.nutricare.util.Passwords;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Module 01 - User & Patient Management. */
class UserPatientRepositoryTest extends IntegrationTestBase {

    @Test
    void patientRegistrationCreatesSharedProfile() throws Exception {
        long userId = patients.createPatient("Test Patient", email("test"), "0770000000", PASSWORD);
        assertTrue(patients.patientByUser(userId).isPresent());
        assertEquals("Test Patient", patients.patientByUser(userId).orElseThrow().get("full_name"));
    }

    @Test
    void otpActivatesAccountAndLocksAfterTooManyWrongCodes() throws Exception {
        long userId = patients.createPatient("Otp Patient", email("otp"), "0770000001", PASSWORD, false);
        assertEquals(false, patients.findUser(userId).orElseThrow().get("active"));

        patients.issueOtp(userId, OtpService.VERIFY, OtpService.hash("111111"), OtpService.expiry());
        for (int attempt = 1; attempt < OtpService.MAX_ATTEMPTS; attempt++) {
            assertEquals(OtpService.Result.WRONG_CODE, patients.verifyAccount(userId, OtpService.hash("999999")));
        }
        assertEquals(OtpService.Result.TOO_MANY_ATTEMPTS, patients.verifyAccount(userId, OtpService.hash("999999")));
        assertEquals(OtpService.Result.EXPIRED, patients.verifyAccount(userId, OtpService.hash("111111")),
                "a locked code must not work even when correct");

        patients.issueOtp(userId, OtpService.VERIFY, OtpService.hash("222222"), OtpService.expiry());
        assertEquals(OtpService.Result.VERIFIED, patients.verifyAccount(userId, OtpService.hash("222222")));
        Map<String, Object> user = patients.findUser(userId).orElseThrow();
        assertEquals(true, user.get("active"));
        assertEquals(true, user.get("verified"));
    }

    @Test
    void expiredOtpIsRejectedAndOnlyNewestCodeWorks() throws Exception {
        long userId = patients.createPatient("Expiry Patient", email("expiry"), "0770000002", PASSWORD, false);
        patients.issueOtp(userId, OtpService.VERIFY, OtpService.hash("123456"), LocalDateTime.now().minusMinutes(1));
        assertEquals(OtpService.Result.EXPIRED, patients.verifyAccount(userId, OtpService.hash("123456")));

        patients.issueOtp(userId, OtpService.VERIFY, OtpService.hash("111111"), OtpService.expiry());
        patients.issueOtp(userId, OtpService.VERIFY, OtpService.hash("222222"), OtpService.expiry());
        assertEquals(OtpService.Result.WRONG_CODE, patients.verifyAccount(userId, OtpService.hash("111111")));
        assertEquals(OtpService.Result.VERIFIED, patients.verifyAccount(userId, OtpService.hash("222222")));
    }

    @Test
    void passwordResetCodeChangesPassword() throws Exception {
        long userId = patients.createPatient("Reset Patient", email("reset"), "0770000003", PASSWORD);
        patients.issueOtp(userId, OtpService.RESET, OtpService.hash("654321"), OtpService.expiry());
        assertEquals(OtpService.Result.WRONG_CODE,
                patients.resetPassword(userId, OtpService.hash("000000"), Passwords.hash("NewPassword9")));
        assertEquals(OtpService.Result.VERIFIED,
                patients.resetPassword(userId, OtpService.hash("654321"), Passwords.hash("NewPassword9")));
        String hash = String.valueOf(patients.findUser(userId).orElseThrow().get("password_hash"));
        assertTrue(Passwords.matches("NewPassword9", hash));
        assertFalse(Passwords.matches("Password1", hash));
    }

    @Test
    void newAllergyFlagsExistingDietPlans() throws Exception {
        long patientUser = patients.createPatient("Allergy Patient", email("allergy"), "0770000006", PASSWORD);
        long nutritionist = newStaff("Profile Nutritionist", "NUTRITIONIST");
        diets.createDietPlan(patientId(patientUser), nutritionist, "Peanut plan", 2000, "Breakfast: peanut butter toast",
                LocalDate.now(), LocalDate.now().plusDays(7));

        int flagged = patients.updateProfile(patientUser, "Allergy Patient", "0770000006", null, null,
                null, null, "Peanut; shellfish", null, null, null);
        assertEquals(1, flagged);
        assertEquals(1, diets.dietPlansNeedingReview(nutritionist, "NUTRITIONIST").size());
    }

    @Test
    void selfDeactivationBlocksTheAccount() throws Exception {
        long patientUser = newPatient("Leaving Patient");
        patients.deactivatePatient(patientUser);
        assertEquals(false, patients.findUser(patientUser).orElseThrow().get("active"));
    }
}
