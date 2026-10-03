package com.nutricare.repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared dashboard: headline figures from all six modules for the signed-in role. */
public class DashboardRepository extends BaseRepository {

    /** Headline figures as label -> value, in display order. */
    public Map<String, Object> dashboardCounts(long userId, String role) throws SQLException {
        Map<String, Object> counts = new LinkedHashMap<>();
        Timestamp now = timestamp(LocalDateTime.now());
        Timestamp dayAhead = timestamp(LocalDateTime.now().plusHours(24));
        Timestamp monthStart = timestamp(LocalDate.now().withDayOfMonth(1).atStartOfDay());
        String upcoming = "SELECT COUNT(*) FROM appointments WHERE status IN " + OPEN_APPOINTMENT + " AND appointment_time>=?";
        String verifiedRevenue = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='VERIFIED' AND verified_at>=?";
        switch (role) {
            case "PATIENT" -> {
                long patientId = one("SELECT id FROM patients WHERE user_id=?", userId).map(p -> number(p.get("id"))).orElse(0L);
                counts.put("Upcoming appointments", scalar(upcoming + " AND patient_id=?", now, patientId));
                counts.put("Check-ups recorded", scalar("SELECT COUNT(*) FROM checkups WHERE patient_id=?", patientId));
                counts.put("Active diet plan", scalar("SELECT COUNT(*) FROM diet_plans WHERE patient_id=? AND status='ACTIVE'", patientId));
                counts.put("Payments awaiting verification", scalar("SELECT COUNT(*) FROM payments WHERE patient_id=? AND status='SUBMITTED'", patientId));
            }
            case "DOCTOR" -> {
                counts.put("My upcoming appointments", scalar(upcoming + " AND staff_id=?", now, userId));
                counts.put("Visits awaiting check-up", scalar("SELECT COUNT(*) FROM appointments a WHERE a.staff_id=? AND a.status='COMPLETED' AND NOT EXISTS (SELECT 1 FROM checkups c WHERE c.appointment_id=a.id)", userId));
                counts.put("Abnormal check-ups (7 days)", scalar("SELECT COUNT(*) FROM checkups c WHERE c.abnormal=? AND c.checked_at>=? AND " + HealthCheckupRepository.DOCTOR_PATIENTS,
                        true, timestamp(LocalDateTime.now().minusDays(7)), userId, userId));
            }
            case "NUTRITIONIST" -> {
                counts.put("My upcoming appointments", scalar(upcoming + " AND staff_id=?", now, userId));
                counts.put("Plans needing review", scalar("SELECT COUNT(*) FROM diet_plans WHERE nutritionist_id=? AND review_needed=? AND status IN ('DRAFT','ACTIVE')", userId, true));
                counts.put("Draft plans", scalar("SELECT COUNT(*) FROM diet_plans WHERE nutritionist_id=? AND status='DRAFT'", userId));
            }
            case "RECEPTIONIST" -> {
                counts.put("Requests to confirm", scalar("SELECT COUNT(*) FROM appointments WHERE status='PENDING' AND appointment_time>=?", now));
                counts.put("Appointments in next 24h", scalar(upcoming + " AND appointment_time<?", now, dayAhead));
                counts.put("Active patients", scalar("SELECT COUNT(*) FROM users WHERE role='PATIENT' AND active=?", true));
            }
            case "FINANCE" -> {
                counts.put("Payments awaiting verification", scalar("SELECT COUNT(*) FROM payments WHERE status='SUBMITTED'"));
                counts.put("Verified this month (LKR)", money(verifiedRevenue, monthStart));
                counts.put("Refunds issued", scalar("SELECT COUNT(*) FROM payments WHERE status='REFUNDED'"));
            }
            default -> {
                counts.put("Active patients", scalar("SELECT COUNT(*) FROM users WHERE role='PATIENT' AND active=?", true));
                counts.put("Active staff", scalar("SELECT COUNT(*) FROM users WHERE role<>'PATIENT' AND active=?", true));
                counts.put("Upcoming appointments", scalar(upcoming, now));
                counts.put("Payments awaiting verification", scalar("SELECT COUNT(*) FROM payments WHERE status='SUBMITTED'"));
                counts.put("Diet plans needing review", scalar("SELECT COUNT(*) FROM diet_plans WHERE review_needed=? AND status IN ('DRAFT','ACTIVE')", true));
                counts.put("Verified this month (LKR)", money(verifiedRevenue, monthStart));
            }
        }
        return counts;
    }
}
