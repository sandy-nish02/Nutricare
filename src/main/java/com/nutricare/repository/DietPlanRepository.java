package com.nutricare.repository;

import com.nutricare.service.DietSafety;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Module 02 - Diet Plan Management: allergy-checked plans, approval, and one active plan per patient. */
public class DietPlanRepository extends BaseRepository {
    private static final String DIET_SELECT =
            "SELECT d.*, pu.full_name AS patient_name, nu.full_name AS nutritionist_name FROM diet_plans d " +
            "JOIN patients p ON p.id=d.patient_id JOIN users pu ON pu.id=p.user_id JOIN users nu ON nu.id=d.nutritionist_id ";

    /** Patients only ever see their approved (ACTIVE) plan. */
    public List<Map<String, Object>> dietPlans(long userId, String role) throws SQLException {
        if ("PATIENT".equals(role)) {
            return list(DIET_SELECT + "WHERE p.user_id=? AND d.status='ACTIVE' ORDER BY d.created_at DESC", userId);
        }
        if ("NUTRITIONIST".equals(role)) return list(DIET_SELECT + "WHERE d.nutritionist_id=? ORDER BY d.created_at DESC", userId);
        return list(DIET_SELECT + "ORDER BY d.created_at DESC");
    }

    public List<Map<String, Object>> dietPlansNeedingReview(long userId, String role) throws SQLException {
        String where = "WHERE d.review_needed=? AND d.status IN ('DRAFT','ACTIVE') ";
        return "NUTRITIONIST".equals(role)
                ? list(DIET_SELECT + where + "AND d.nutritionist_id=? ORDER BY d.created_at DESC", true, userId)
                : list(DIET_SELECT + where + "ORDER BY d.created_at DESC", true);
    }

    /** The allergy check lives here, next to the insert, so no caller can skip it. */
    public void createDietPlan(long patientId, long nutritionistId, String title, int calories,
                               String meals, LocalDate start, LocalDate end) throws SQLException {
        transaction(connection -> {
            Map<String, Object> patient = one(connection, "SELECT allergies FROM patients WHERE id=?", patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient was not found."));
            requireNoAllergyConflict((String) patient.get("allergies"), meals);
            insert(connection, "INSERT INTO diet_plans(patient_id,nutritionist_id,title,daily_calories,meals,start_date,end_date,status) VALUES(?,?,?,?,?,?,?,?)",
                    patientId, nutritionistId, title, calories, meals, start, end, "DRAFT");
            log(connection, nutritionistId, "DIET_PLAN_CREATED", "Diet plan created for patient #" + patientId);
            return null;
        });
    }

    public void updateDietStatus(long id, String status, long actorId, String actorRole) throws SQLException {
        transaction(connection -> {
            String lookup = "SELECT d.patient_id, d.meals, p.allergies FROM diet_plans d JOIN patients p ON p.id=d.patient_id WHERE d.id=?";
            Map<String, Object> plan = ("NUTRITIONIST".equals(actorRole)
                    ? one(connection, lookup + " AND d.nutritionist_id=?", id, actorId)
                    : one(connection, lookup, id))
                    .orElseThrow(() -> new IllegalArgumentException("Diet plan was not found."));
            long patientId = number(plan.get("patient_id"));
            if ("ACTIVE".equals(status)) {
                // Allergies may have changed since the draft was written, so approval re-runs the check.
                requireNoAllergyConflict((String) plan.get("allergies"), (String) plan.get("meals"));
                execute(connection,
                        "UPDATE diet_plans SET status='ARCHIVED' WHERE patient_id=? AND id<>? AND status='ACTIVE'",
                        patientId, id);
            }
            execute(connection, "UPDATE diet_plans SET status=?, review_needed=? WHERE id=?", status, false, id);
            log(connection, actorId, "DIET_STATUS", "Diet plan #" + id + " changed to " + status);
            return null;
        });
    }

    private static void requireNoAllergyConflict(String allergies, String meals) {
        List<String> conflicts = DietSafety.conflictingAllergies(allergies, meals);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Unsafe diet: meal text contains patient allergy: " + String.join(", ", conflicts));
        }
    }
}
