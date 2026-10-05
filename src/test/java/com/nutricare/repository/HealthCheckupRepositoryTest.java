package com.nutricare.repository;

import com.nutricare.repository.HealthCheckupRepository.CheckupOutcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Module 03 - Health Check-up Management. */
class HealthCheckupRepositoryTest extends IntegrationTestBase {

    @Test
    void checkupLinksToCompletedDoctorAppointmentOnce() throws Exception {
        long patientUser = newPatient("Checkup Patient");
        long doctor = newStaff("Checkup Doctor", "DOCTOR");
        long appointmentId = book(patientUser, doctor, 7);
        long patientId = patientId(patientUser);

        assertThrows(IllegalArgumentException.class, () -> checkups.createCheckup(patientId, appointmentId, doctor,
                "DOCTOR", 170, 70, 120, 80, 100.0, null), "a pending visit cannot have a check-up yet");
        appointments.updateAppointmentStatus(appointmentId, "CONFIRMED", doctor, "DOCTOR", null);
        appointments.updateAppointmentStatus(appointmentId, "COMPLETED", doctor, "DOCTOR", null);
        assertEquals(1, appointments.appointmentsAwaitingCheckup(doctor).size());

        CheckupOutcome outcome = checkups.createCheckup(0, appointmentId, doctor, "DOCTOR", 170, 70, 150, null, null, "Follow up");
        assertTrue(outcome.abnormal(), "systolic 150 alone is abnormal");
        assertEquals(24.22, outcome.bmi());
        assertTrue(appointments.appointmentsAwaitingCheckup(doctor).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> checkups.createCheckup(0, appointmentId, doctor,
                "DOCTOR", 170, 70, null, null, null, null), "one check-up per appointment");
        assertEquals(1, checkups.checkups(patientUser, "PATIENT").size());
    }

    @Test
    void significantWeightChangeFlagsActiveDietPlan() throws Exception {
        long patientUser = newPatient("Weight Patient");
        long patientId = patientId(patientUser);
        long doctor = newStaff("Weight Doctor", "DOCTOR");
        long nutritionist = newStaff("Weight Nutritionist", "NUTRITIONIST");
        checkups.createCheckup(patientId, null, doctor, "DOCTOR", 170, 80, null, null, null, null);
        diets.createDietPlan(patientId, nutritionist, "Weight plan", 1800, "Oats", LocalDate.now(), LocalDate.now().plusDays(7));
        diets.updateDietStatus(planId(diets.dietPlans(nutritionist, "NUTRITIONIST"), "Weight plan"), "ACTIVE", nutritionist, "NUTRITIONIST");

        assertFalse(checkups.createCheckup(patientId, null, doctor, "DOCTOR", 170, 82, null, null, null, null).dietReviewFlagged(),
                "2.5% is below the threshold");
        assertTrue(checkups.createCheckup(patientId, null, doctor, "DOCTOR", 170, 87, null, null, null, null).dietReviewFlagged());
        assertEquals(1, diets.dietPlansNeedingReview(nutritionist, "NUTRITIONIST").size());
    }

    @Test
    void nutritionistSeesCheckupsOfTheirOwnPatientsOnly() throws Exception {
        long patientUser = newPatient("Shared Patient");
        long patientId = patientId(patientUser);
        long doctor = newStaff("Shared Doctor", "DOCTOR");
        long nutritionist = newStaff("Shared Nutritionist", "NUTRITIONIST");
        long stranger = newStaff("Stranger Nutritionist", "NUTRITIONIST");
        checkups.createCheckup(patientId, null, doctor, "DOCTOR", 165, 60, null, null, null, null);
        diets.createDietPlan(patientId, nutritionist, "Shared plan", 1800, "Rice", LocalDate.now(), LocalDate.now().plusDays(7));

        assertEquals(1, checkups.checkups(nutritionist, "NUTRITIONIST").size());
        assertTrue(checkups.checkups(stranger, "NUTRITIONIST").isEmpty());
    }
}
