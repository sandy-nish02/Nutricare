package com.nutricare.repository;

import com.nutricare.config.Database;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Module 04 - Appointment Management, including the SQL Server status-change trigger. */
class AppointmentRepositoryTest extends IntegrationTestBase {

    @Test
    void preventsDoubleBookingWithinTheSameSlot() throws Exception {
        long patientUser = newPatient("Slot Patient A");
        long patientId = patientId(patientUser);
        long doctor = newStaff("Slot Doctor", "DOCTOR");
        LocalDateTime time = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointments.createAppointment(patientId, doctor, time, "Review", patientUser);

        assertThrows(IllegalArgumentException.class,
                () -> appointments.createAppointment(patientId, doctor, time, "Same time", patientUser));
        long otherPatient = patientId(newPatient("Slot Patient B"));
        assertThrows(IllegalArgumentException.class,
                () -> appointments.createAppointment(otherPatient, doctor, time.plusMinutes(15), "Overlapping", patientUser));
        appointments.createAppointment(otherPatient, doctor, time.plusMinutes(30), "Next slot", patientUser);
    }

    @Test
    void patientCannotBeInTwoAppointmentsAtOnce() throws Exception {
        long patientUser = newPatient("Busy Patient");
        long patientId = patientId(patientUser);
        long doctor = newStaff("Busy Doctor", "DOCTOR");
        long nutritionist = newStaff("Busy Nutritionist", "NUTRITIONIST");
        LocalDateTime time = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        appointments.createAppointment(patientId, doctor, time, "Doctor visit", patientUser);
        assertThrows(IllegalArgumentException.class,
                () -> appointments.createAppointment(patientId, nutritionist, time.plusMinutes(10), "Diet visit", patientUser));
    }

    @Test
    void appointmentStatusFollowsTheStateMachine() throws Exception {
        long patientUser = newPatient("State Patient");
        long doctor = newStaff("State Doctor", "DOCTOR");
        long reception = newStaff("State Reception", "RECEPTIONIST");
        long appointmentId = book(patientUser, doctor, 4);

        assertThrows(IllegalArgumentException.class, () -> appointments.updateAppointmentStatus(
                appointmentId, "COMPLETED", reception, "RECEPTIONIST", null), "PENDING cannot jump to COMPLETED");
        assertThrows(IllegalArgumentException.class, () -> appointments.updateAppointmentStatus(
                appointmentId, "CANCELLED", reception, "RECEPTIONIST", " "), "cancelling needs a reason");
        appointments.updateAppointmentStatus(appointmentId, "CANCELLED", reception, "RECEPTIONIST", "Clinic closed");
        assertThrows(IllegalArgumentException.class, () -> appointments.updateAppointmentStatus(
                appointmentId, "CONFIRMED", reception, "RECEPTIONIST", null), "a cancelled visit cannot be revived");
        assertThrows(IllegalArgumentException.class, () -> appointments.rescheduleAppointment(
                appointmentId, LocalDateTime.now().plusDays(5), "Try again", reception, "RECEPTIONIST"));
    }

    @Test
    void cliniciansCanOnlyChangeTheirOwnAppointments() throws Exception {
        long patientUser = newPatient("Own Patient");
        long doctor = newStaff("Own Doctor", "DOCTOR");
        long otherDoctor = newStaff("Other Doctor", "DOCTOR");
        long appointmentId = book(patientUser, doctor, 6);
        assertThrows(IllegalArgumentException.class, () -> appointments.updateAppointmentStatus(
                appointmentId, "CONFIRMED", otherDoctor, "DOCTOR", null));
        appointments.updateAppointmentStatus(appointmentId, "CONFIRMED", doctor, "DOCTOR", null);
        assertEquals("CONFIRMED", appointmentStatus(patientUser));
    }

    @Test
    void rescheduleMovesTheVisitBackToPending() throws Exception {
        long patientUser = newPatient("Move Patient");
        long doctor = newStaff("Move Doctor", "DOCTOR");
        long appointmentId = book(patientUser, doctor, 9);
        appointments.updateAppointmentStatus(appointmentId, "CONFIRMED", doctor, "DOCTOR", null);
        appointments.rescheduleAppointment(appointmentId, LocalDateTime.now().plusDays(4).withSecond(0).withNano(0),
                "Doctor on leave", doctor, "DOCTOR");
        Map<String, Object> row = appointments.appointments(patientUser, "PATIENT").get(0);
        assertEquals("PENDING", row.get("status"));
        assertEquals("Doctor on leave", row.get("change_reason"));
    }

    @Test
    void sqlServerTriggerAuditsOnlyStatusChangesMadeOutsideTheApp() throws Exception {
        assumeTrue(onSqlServer(), "the trigger only exists on SQL Server");
        long patientUser = newPatient("Trigger Patient");
        long doctor = newStaff("Trigger Doctor", "DOCTOR");
        long appointmentId = book(patientUser, doctor, 10);

        appointments.updateAppointmentStatus(appointmentId, "CONFIRMED", doctor, "DOCTOR", null);
        assertEquals(0, triggerEntries(appointmentId), "the app writes its own entry, so the trigger stays silent");

        // A plain connection without NutriCare's session tag behaves like someone editing the table in SSMS.
        try (Connection outside = DriverManager.getConnection(System.getProperty("it.sqlserver.url"));
             PreparedStatement update = outside.prepareStatement("UPDATE appointments SET status='COMPLETED' WHERE id=?")) {
            update.setLong(1, appointmentId);
            update.executeUpdate();
        }
        assertEquals(1, triggerEntries(appointmentId), "a change made outside the app is recorded by the trigger");
    }

    private static long triggerEntries(long appointmentId) throws Exception {
        try (Connection connection = Database.connect()) {
            return BaseRepository.count(connection,"SELECT COUNT(*) FROM audit_logs WHERE action='APPOINTMENT_STATUS_CHANGED' AND user_id IS NULL AND details LIKE ?",
                    "Appointment ID " + appointmentId + " (%outside NutriCare%");
        }
    }
}
