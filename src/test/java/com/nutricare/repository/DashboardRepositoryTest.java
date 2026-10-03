package com.nutricare.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Shared dashboard plus a smoke test that every role-specific query runs on the target database. */
class DashboardRepositoryTest extends IntegrationTestBase {

    @Test
    void everyRoleDashboardAndListingQueryRuns() throws Exception {
        long patientUser = newPatient("Dash Patient");
        long doctor = newStaff("Dash Doctor", "DOCTOR");
        book(patientUser, doctor, 1);
        long[] users = {patientUser, doctor, newStaff("Dash Nutritionist", "NUTRITIONIST"),
                newStaff("Dash Reception", "RECEPTIONIST"), newStaff("Dash Finance", "FINANCE"), newStaff("Dash Admin", "ADMIN")};
        String[] roles = {"PATIENT", "DOCTOR", "NUTRITIONIST", "RECEPTIONIST", "FINANCE", "ADMIN"};
        for (int i = 0; i < roles.length; i++) {
            assertFalse(dashboard.dashboardCounts(users[i], roles[i]).isEmpty(), roles[i]);
            assertNotNull(appointments.appointments(users[i], roles[i]));
            assertNotNull(appointments.upcomingAppointments(users[i], roles[i]));
            assertNotNull(checkups.checkups(users[i], roles[i]));
            assertNotNull(checkups.recentAbnormalCheckups(users[i], roles[i]));
            assertNotNull(diets.dietPlans(users[i], roles[i]));
            assertNotNull(diets.dietPlansNeedingReview(users[i], roles[i]));
            assertNotNull(payments.payments(users[i], roles[i]));
            assertNotNull(payments.payableAppointments(users[i], roles[i]));
        }
        assertFalse(appointments.upcomingAppointments(patientUser, "PATIENT").isEmpty(), "booked within 24 hours");
        assertEquals(1L, dashboard.dashboardCounts(patientUser, "PATIENT").get("Upcoming appointments"));
        assertFalse(staff.auditLogs().isEmpty());
        assertFalse(patients.patients().isEmpty());
        assertFalse(staff.patientAccounts().isEmpty());
        assertFalse(appointments.staffForAppointments().isEmpty());
    }
}
