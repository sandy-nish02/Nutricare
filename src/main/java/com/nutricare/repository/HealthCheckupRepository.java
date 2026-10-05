package com.nutricare.repository;

import com.nutricare.config.Database;
import com.nutricare.service.HealthRules;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Module 03 - Health Check-up Management: vitals, system-calculated BMI, abnormal flags, and FR15 links. */
public class HealthCheckupRepository extends BaseRepository {
    private static final String CHECKUP_SELECT =
            "SELECT c.*, pu.full_name AS patient_name, du.full_name AS doctor_name FROM checkups c " +
            "JOIN patients p ON p.id=c.patient_id JOIN users pu ON pu.id=p.user_id JOIN users du ON du.id=c.doctor_id ";
    /** Check-ups a doctor may read: ones they recorded, or for patients booked with them. */
    static final String DOCTOR_PATIENTS =
            "(c.doctor_id=? OR c.patient_id IN (SELECT patient_id FROM appointments WHERE staff_id=?))";
    private static final String NUTRITIONIST_PATIENTS =
            "(c.patient_id IN (SELECT patient_id FROM appointments WHERE staff_id=?) OR c.patient_id IN (SELECT patient_id FROM diet_plans WHERE nutritionist_id=?))";

    /** What the doctor is told after a check-up is saved. */
    public record CheckupOutcome(double bmi, boolean abnormal, boolean dietReviewFlagged) { }

    /** Clinicians see check-ups for patients assigned to them (proposal access principle). */
    public List<Map<String, Object>> checkups(long userId, String role) throws SQLException {
        List<Map<String, Object>> rows = switch (role) {
            case "PATIENT" -> list(CHECKUP_SELECT + "WHERE p.user_id=? ORDER BY c.checked_at DESC", userId);
            case "DOCTOR" -> list(CHECKUP_SELECT + "WHERE " + DOCTOR_PATIENTS + " ORDER BY c.checked_at DESC", userId, userId);
            case "NUTRITIONIST" -> list(CHECKUP_SELECT + "WHERE " + NUTRITIONIST_PATIENTS + " ORDER BY c.checked_at DESC", userId, userId);
            default -> list(CHECKUP_SELECT + "ORDER BY c.checked_at DESC");
        };
        return withBmiLabels(rows);
    }

    /** Abnormal check-ups from the last seven days, for the doctor's and admin's dashboard alert list. */
    public List<Map<String, Object>> recentAbnormalCheckups(long userId, String role) throws SQLException {
        Timestamp since = timestamp(LocalDateTime.now().minusDays(7));
        return "DOCTOR".equals(role)
                ? withBmiLabels(list(CHECKUP_SELECT + "WHERE c.abnormal=? AND c.checked_at>=? AND " + DOCTOR_PATIENTS
                        + " ORDER BY c.checked_at DESC", true, since, userId, userId))
                : withBmiLabels(list(CHECKUP_SELECT + "WHERE c.abnormal=? AND c.checked_at>=? ORDER BY c.checked_at DESC", true, since));
    }

    private static List<Map<String, Object>> withBmiLabels(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            if (row.get("bmi") instanceof Number bmi) row.put("bmi_label", HealthRules.bmiLabel(bmi.doubleValue()));
        }
        return rows;
    }

    /** FR15: a completed doctor appointment that does not have a check-up record yet. */
    public Optional<Map<String, Object>> appointmentAwaitingCheckup(long appointmentId, long actorId, String actorRole) throws SQLException {
        try (Connection connection = Database.connect()) {
            return appointmentAwaitingCheckup(connection, appointmentId, actorId, actorRole);
        }
    }

    private static Optional<Map<String, Object>> appointmentAwaitingCheckup(Connection connection, long appointmentId,
                                                                           long actorId, String actorRole) throws SQLException {
        String sql = "SELECT a.id, a.patient_id, a.appointment_time, a.purpose, pu.full_name AS patient_name FROM appointments a " +
                "JOIN patients p ON p.id=a.patient_id JOIN users pu ON pu.id=p.user_id JOIN users su ON su.id=a.staff_id " +
                "WHERE a.id=? AND a.status='COMPLETED' AND su.role='DOCTOR' " +
                "AND NOT EXISTS (SELECT 1 FROM checkups c WHERE c.appointment_id=a.id)";
        return "DOCTOR".equals(actorRole)
                ? one(connection, sql + " AND a.staff_id=?", appointmentId, actorId)
                : one(connection, sql, appointmentId);
    }

    public CheckupOutcome createCheckup(long patientId, Long appointmentId, long doctorId, String actorRole,
                                        double height, double weight, Integer systolic, Integer diastolic,
                                        Double sugar, String notes) throws SQLException {
        double bmi = HealthRules.bmi(weight, height);
        boolean abnormal = HealthRules.abnormal(bmi, systolic, diastolic, sugar);
        return transaction(connection -> {
            long patient = patientId;
            if (appointmentId != null) {
                Map<String, Object> appointment = appointmentAwaitingCheckup(connection, appointmentId, doctorId, actorRole)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "That appointment is not a completed doctor visit awaiting a check-up."));
                patient = number(appointment.get("patient_id"));
            }
            Map<String, Object> patientRow = one(connection, "SELECT weight_kg FROM patients WHERE id=?", patient)
                    .orElseThrow(() -> new IllegalArgumentException("Patient was not found."));
            Double oldWeight = patientRow.get("weight_kg") instanceof Number value ? value.doubleValue() : null;
            insert(connection, "INSERT INTO checkups(patient_id,doctor_id,appointment_id,height_cm,weight_kg,bmi,systolic,diastolic,blood_sugar,notes,abnormal) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    patient, doctorId, appointmentId, height, weight, bmi, systolic, diastolic, sugar, notes, abnormal);
            execute(connection, "UPDATE patients SET height_cm=?, weight_kg=? WHERE id=?", height, weight, patient);
            boolean reviewFlagged = false;
            if (oldWeight != null && HealthRules.significantWeightChange(oldWeight, weight)) {
                reviewFlagged = execute(connection,
                        "UPDATE diet_plans SET review_needed=? WHERE patient_id=? AND status='ACTIVE'", true, patient) > 0;
            }
            log(connection, doctorId, "CHECKUP_RECORDED", "Check-up recorded for patient #" + patient + "; BMI " + bmi
                    + (abnormal ? "; ABNORMAL reading flagged" : "")
                    + (appointmentId != null ? "; linked to appointment #" + appointmentId : ""));
            return new CheckupOutcome(bmi, abnormal, reviewFlagged);
        });
    }
}
