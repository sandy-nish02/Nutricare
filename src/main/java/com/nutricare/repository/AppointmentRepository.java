package com.nutricare.repository;

import com.nutricare.service.AppointmentRules;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Module 04 - Appointment Management: booking, clash checks, the status state machine, and rescheduling. */
public class AppointmentRepository extends BaseRepository {
    static final String APPOINTMENT_SELECT =
            "SELECT a.*, p.user_id AS patient_user_id, pu.full_name AS patient_name, su.full_name AS staff_name, su.role AS staff_role, c.id AS checkup_id " +
            "FROM appointments a JOIN patients p ON p.id=a.patient_id JOIN users pu ON pu.id=p.user_id " +
            "JOIN users su ON su.id=a.staff_id LEFT JOIN checkups c ON c.appointment_id=a.id ";

    /** Patients see their own visits, clinicians their assigned schedule, reception and admin everything. */
    private static String appointmentScope(String role) {
        if ("PATIENT".equals(role)) return "p.user_id=?";
        if (isClinician(role)) return "a.staff_id=?";
        return null;
    }

    public List<Map<String, Object>> staffForAppointments() throws SQLException {
        return list("SELECT id, full_name, role FROM users WHERE active=? AND role IN ('DOCTOR','NUTRITIONIST') ORDER BY role, full_name", true);
    }

    public List<Map<String, Object>> appointments(long userId, String role) throws SQLException {
        String scope = appointmentScope(role);
        return scope == null
                ? decorate(list(APPOINTMENT_SELECT + "ORDER BY a.appointment_time DESC"))
                : decorate(list(APPOINTMENT_SELECT + "WHERE " + scope + " ORDER BY a.appointment_time DESC", userId));
    }

    /** FR14: open appointments in the next 24 hours, shown as reminders on the dashboard. */
    public List<Map<String, Object>> upcomingAppointments(long userId, String role) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        String where = "WHERE a.status IN " + OPEN_APPOINTMENT + " AND a.appointment_time>=? AND a.appointment_time<? ";
        String scope = appointmentScope(role);
        return scope == null
                ? decorate(list(APPOINTMENT_SELECT + where + "ORDER BY a.appointment_time",
                        timestamp(now), timestamp(now.plusHours(24))))
                : decorate(list(APPOINTMENT_SELECT + where + "AND " + scope + " ORDER BY a.appointment_time",
                        timestamp(now), timestamp(now.plusHours(24)), userId));
    }

    /** FR15: completed visits of this doctor that still need their check-up recorded. */
    public List<Map<String, Object>> appointmentsAwaitingCheckup(long doctorId) throws SQLException {
        return decorate(list(APPOINTMENT_SELECT
                + "WHERE a.staff_id=? AND a.status='COMPLETED' AND c.id IS NULL ORDER BY a.appointment_time DESC", doctorId));
    }

    /** Adds the values the JSP needs: the 24-hour reminder flag and which status changes are allowed next. */
    private static List<Map<String, Object>> decorate(List<Map<String, Object>> appointments) {
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> appointment : appointments) {
            Object raw = appointment.get("appointment_time");
            LocalDateTime time = raw instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : null;
            String status = String.valueOf(appointment.get("status"));
            appointment.put("reminder", time != null && time.isAfter(now) && time.isBefore(now.plusHours(24))
                    && AppointmentRules.OPEN.contains(status));
            appointment.put("next_statuses", AppointmentRules.nextStatuses(status));
            appointment.put("reschedulable", AppointmentRules.OPEN.contains(status));
        }
        return appointments;
    }

    public void createAppointment(long patientId, long staffId, LocalDateTime time, String purpose, long actorId) throws SQLException {
        // Serializable isolation closes the gap between the clash check and the insert.
        transaction(Connection.TRANSACTION_SERIALIZABLE, connection -> {
            if (count(connection, "SELECT COUNT(*) FROM users WHERE id=? AND active=? AND role IN ('DOCTOR','NUTRITIONIST')", staffId, true) == 0) {
                throw new IllegalArgumentException("Select an active doctor or nutritionist.");
            }
            if (count(connection, "SELECT COUNT(*) FROM patients p JOIN users u ON u.id=p.user_id WHERE p.id=? AND u.active=?", patientId, true) == 0) {
                throw new IllegalArgumentException("Select an active patient.");
            }
            requireFreeSlot(connection, staffId, patientId, time, null);
            insert(connection, "INSERT INTO appointments(patient_id,staff_id,appointment_time,purpose,status) VALUES(?,?,?,?,?)",
                    patientId, staffId, timestamp(time), purpose, "PENDING");
            log(connection, actorId, "APPOINTMENT_CREATED", "Appointment requested for " + time);
            return null;
        });
    }

    /** A slot is taken when an open appointment for the same staff member or patient starts within SLOT_MINUTES. */
    private static void requireFreeSlot(Connection connection, long staffId, long patientId,
                                        LocalDateTime time, Long ignoredAppointment) throws SQLException {
        Timestamp from = timestamp(time.minusMinutes(AppointmentRules.SLOT_MINUTES));
        Timestamp to = timestamp(time.plusMinutes(AppointmentRules.SLOT_MINUTES));
        String overlap = " AND status IN " + OPEN_APPOINTMENT + " AND appointment_time>? AND appointment_time<?"
                + (ignoredAppointment == null ? "" : " AND id<>?");
        if (count(connection, "SELECT COUNT(*) FROM appointments WHERE staff_id=?" + overlap,
                withOptional(ignoredAppointment, staffId, from, to)) > 0) {
            throw new IllegalArgumentException("That staff member already has an appointment within "
                    + AppointmentRules.SLOT_MINUTES + " minutes of this time. Choose another slot.");
        }
        if (count(connection, "SELECT COUNT(*) FROM appointments WHERE patient_id=?" + overlap,
                withOptional(ignoredAppointment, patientId, from, to)) > 0) {
            throw new IllegalArgumentException("This patient already has another appointment within "
                    + AppointmentRules.SLOT_MINUTES + " minutes of this time.");
        }
    }

    public void rescheduleAppointment(long id, LocalDateTime newTime, String reason,
                                      long actorId, String actorRole) throws SQLException {
        transaction(Connection.TRANSACTION_SERIALIZABLE, connection -> {
            Map<String, Object> appointment = one(connection, "SELECT staff_id, patient_id, status FROM appointments WHERE id=?", id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment was not found."));
            long staffId = number(appointment.get("staff_id"));
            if (isClinician(actorRole) && staffId != actorId) {
                throw new IllegalArgumentException("You can only reschedule your assigned appointments.");
            }
            if (!AppointmentRules.OPEN.contains(String.valueOf(appointment.get("status")))) {
                throw new IllegalArgumentException("Only pending or confirmed appointments can be rescheduled.");
            }
            requireFreeSlot(connection, staffId, number(appointment.get("patient_id")), newTime, id);
            execute(connection, "UPDATE appointments SET appointment_time=?, status='PENDING', change_reason=? WHERE id=?",
                    timestamp(newTime), reason, id);
            log(connection, actorId, "APPOINTMENT_RESCHEDULED",
                    "Appointment #" + id + " moved to " + newTime + "; reason: " + reason);
            return null;
        });
    }

    /** Applies one step of the appointment state machine (see AppointmentRules). */
    public void updateAppointmentStatus(long id, String status, long actorId,
                                        String actorRole, String reason) throws SQLException {
        transaction(connection -> {
            Map<String, Object> appointment = one(connection, "SELECT staff_id, status FROM appointments WHERE id=?", id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment was not found."));
            if (isClinician(actorRole) && number(appointment.get("staff_id")) != actorId) {
                throw new IllegalArgumentException("Appointment was not found.");
            }
            String current = String.valueOf(appointment.get("status"));
            if (!AppointmentRules.canTransition(current, status)) {
                throw new IllegalArgumentException(current.equals(status)
                        ? "This appointment is already " + status + "."
                        : "A " + current + " appointment cannot be changed to " + status + ".");
            }
            boolean hasReason = reason != null && !reason.isBlank();
            if (AppointmentRules.requiresReason(status) && !hasReason) {
                throw new IllegalArgumentException("Enter a reason for this appointment status.");
            }
            int changed = hasReason
                    ? execute(connection, "UPDATE appointments SET status=?, change_reason=? WHERE id=? AND status=?", status, reason, id, current)
                    : execute(connection, "UPDATE appointments SET status=? WHERE id=? AND status=?", status, id, current);
            if (changed == 0) throw new IllegalArgumentException("This appointment was just changed by someone else. Refresh and try again.");
            log(connection, actorId, "APPOINTMENT_STATUS", "Appointment #" + id + " changed from " + current + " to " + status
                    + (hasReason ? "; reason: " + reason : ""));
            return null;
        });
    }

    public void cancelOwnAppointment(long id, long patientUserId, String reason) throws SQLException {
        transaction(connection -> {
            int changed = execute(connection,
                    "UPDATE appointments SET status='CANCELLED', change_reason=? WHERE id=? AND patient_id IN (SELECT id FROM patients WHERE user_id=?) AND status IN " + OPEN_APPOINTMENT,
                    reason, id, patientUserId);
            if (changed == 0) throw new IllegalArgumentException("This appointment cannot be cancelled.");
            log(connection, patientUserId, "APPOINTMENT_CANCELLED",
                    "Patient cancelled appointment #" + id + "; reason: " + reason);
            return null;
        });
    }
}
