package com.nutricare.repository;

import com.nutricare.service.DietSafety;
import com.nutricare.service.HealthRules;
import com.nutricare.service.OtpService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Module 01 - User & Patient Management: accounts, OTPs, password reset, and the patient health profile. */
public class UserPatientRepository extends BaseRepository {

    public Optional<Map<String, Object>> findUserByEmail(String email) throws SQLException {
        return one("SELECT * FROM users WHERE LOWER(email)=LOWER(?)", email);
    }

    public Optional<Map<String, Object>> findUser(long id) throws SQLException {
        return one("SELECT * FROM users WHERE id=?", id);
    }

    public long createPatient(String name, String email, String phone, String passwordHash) throws SQLException {
        return createPatient(name, email, phone, passwordHash, true);
    }

    /** Self-registered patients start inactive and unverified until the OTP is confirmed. */
    public long createPatient(String name, String email, String phone, String passwordHash, boolean verified) throws SQLException {
        return transaction(connection -> {
            long userId = insert(connection,
                    "INSERT INTO users(full_name,email,phone,password_hash,role,active,verified) VALUES(?,?,?,?,?,?,?)",
                    name, email, phone, passwordHash, "PATIENT", verified, verified);
            insert(connection, "INSERT INTO patients(user_id) VALUES(?)", userId);
            log(connection, userId, "PATIENT_REGISTERED", "Patient account created");
            return userId;
        });
    }

    /** Replaces any earlier unused code for the same purpose, so only the newest code works. */
    public void issueOtp(long userId, String purpose, String codeHash, LocalDateTime expiresAt) throws SQLException {
        transaction(connection -> {
            execute(connection, "UPDATE otp_codes SET used=? WHERE user_id=? AND purpose=? AND used=?",
                    true, userId, purpose, false);
            insert(connection, "INSERT INTO otp_codes(user_id,purpose,code_hash,expires_at,attempts,used) VALUES(?,?,?,?,?,?)",
                    userId, purpose, codeHash, Timestamp.valueOf(expiresAt), 0, false);
            return null;
        });
    }

    public OtpService.Result verifyAccount(long userId, String codeHash) throws SQLException {
        return transaction(connection -> {
            OtpService.Result result = consumeOtp(connection, userId, OtpService.VERIFY, codeHash);
            if (result == OtpService.Result.VERIFIED) {
                execute(connection, "UPDATE users SET active=?, verified=? WHERE id=?", true, true, userId);
                log(connection, userId, "OTP_VERIFIED", "Patient phone verification completed");
            } else if (result == OtpService.Result.TOO_MANY_ATTEMPTS) {
                log(connection, userId, "OTP_LOCKED", "Verification code locked after too many attempts");
            }
            return result;
        });
    }

    public OtpService.Result resetPassword(long userId, String codeHash, String newPasswordHash) throws SQLException {
        return transaction(connection -> {
            OtpService.Result result = consumeOtp(connection, userId, OtpService.RESET, codeHash);
            if (result == OtpService.Result.VERIFIED) {
                execute(connection, "UPDATE users SET password_hash=? WHERE id=?", newPasswordHash, userId);
                log(connection, userId, "PASSWORD_RESET", "Password reset with a one-time code");
            } else if (result == OtpService.Result.TOO_MANY_ATTEMPTS) {
                log(connection, userId, "OTP_LOCKED", "Password reset code locked after too many attempts");
            }
            return result;
        });
    }

    /** Checks the newest unused code: expired codes and codes with too many wrong guesses are burned. */
    private static OtpService.Result consumeOtp(Connection connection, long userId, String purpose, String codeHash) throws SQLException {
        Optional<Map<String, Object>> latest = one(connection,
                "SELECT id, code_hash, expires_at, attempts FROM otp_codes WHERE user_id=? AND purpose=? AND used=? ORDER BY id DESC",
                userId, purpose, false);
        if (latest.isEmpty()) return OtpService.Result.EXPIRED;
        Map<String, Object> code = latest.get();
        long id = number(code.get("id"));
        if (((Timestamp) code.get("expires_at")).toLocalDateTime().isBefore(LocalDateTime.now())) {
            execute(connection, "UPDATE otp_codes SET used=? WHERE id=?", true, id);
            return OtpService.Result.EXPIRED;
        }
        if (!OtpService.sameHash((String) code.get("code_hash"), codeHash)) {
            int attempts = ((Number) code.get("attempts")).intValue() + 1;
            boolean burned = attempts >= OtpService.MAX_ATTEMPTS;
            execute(connection, "UPDATE otp_codes SET attempts=?, used=? WHERE id=?", attempts, burned, id);
            return burned ? OtpService.Result.TOO_MANY_ATTEMPTS : OtpService.Result.WRONG_CODE;
        }
        execute(connection, "UPDATE otp_codes SET used=? WHERE id=?", true, id);
        return OtpService.Result.VERIFIED;
    }

    public Optional<Map<String, Object>> patientByUser(long userId) throws SQLException {
        return one("SELECT p.*, u.full_name, u.email, u.phone FROM patients p JOIN users u ON u.id=p.user_id WHERE p.user_id=?", userId);
    }

    public Optional<Map<String, Object>> patient(long patientId) throws SQLException {
        return one("SELECT p.*, u.full_name, u.email, u.phone FROM patients p JOIN users u ON u.id=p.user_id WHERE p.id=?", patientId);
    }

    /** Active patients for staff pick-lists, with the BMI from their latest measurements. */
    public List<Map<String, Object>> patients() throws SQLException {
        List<Map<String, Object>> rows = list("SELECT p.id, u.full_name, u.email, p.allergies, p.height_cm, p.weight_kg FROM patients p JOIN users u ON u.id=p.user_id WHERE u.active=? ORDER BY u.full_name", true);
        for (Map<String, Object> row : rows) {
            if (row.get("height_cm") instanceof Number height && row.get("weight_kg") instanceof Number weight
                    && height.doubleValue() > 0 && weight.doubleValue() > 0) {
                row.put("bmi", HealthRules.bmi(weight.doubleValue(), height.doubleValue()));
            }
        }
        return rows;
    }

    /**
     * Saves the patient's profile. When the allergy list changes, existing draft and active diet plans are
     * re-checked and any plan that now contains an allergen is flagged for review.
     *
     * @return the number of diet plans flagged because of the new allergies
     */
    public int updateProfile(long userId, String name, String phone, LocalDate dob, String gender,
                             Double height, Double weight, String allergies, String conditions,
                             String medications, String emergency) throws SQLException {
        return transaction(connection -> {
            Map<String, Object> before = one(connection, "SELECT id, allergies FROM patients WHERE user_id=?", userId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient profile was not found."));
            execute(connection, "UPDATE users SET full_name=?, phone=? WHERE id=?", name, phone, userId);
            execute(connection, "UPDATE patients SET date_of_birth=?, gender=?, height_cm=?, weight_kg=?, allergies=?, conditions=?, medications=?, emergency_contact=? WHERE user_id=?",
                    dob, gender, height, weight, allergies, conditions, medications, emergency, userId);
            String oldAllergies = (String) before.get("allergies");
            int flagged = 0;
            if (!Objects.equals(blankToEmpty(oldAllergies), blankToEmpty(allergies))) {
                flagged = flagAllergyConflicts(connection, number(before.get("id")), allergies);
                log(connection, userId, "ALLERGIES_UPDATED", "Allergies changed from '" + blankToEmpty(oldAllergies)
                        + "' to '" + blankToEmpty(allergies) + "'"
                        + (flagged > 0 ? "; " + flagged + " diet plan(s) flagged for review" : ""));
            }
            log(connection, userId, "PROFILE_UPDATED", "Patient updated health profile");
            return flagged;
        });
    }

    private static int flagAllergyConflicts(Connection connection, long patientId, String allergies) throws SQLException {
        int flagged = 0;
        for (Map<String, Object> plan : list(connection,
                "SELECT id, meals FROM diet_plans WHERE patient_id=? AND status IN ('DRAFT','ACTIVE')", patientId)) {
            if (!DietSafety.conflictingAllergies(allergies, (String) plan.get("meals")).isEmpty()) {
                execute(connection, "UPDATE diet_plans SET review_needed=? WHERE id=?", true, plan.get("id"));
                flagged++;
            }
        }
        return flagged;
    }

    public void updatePassword(long userId, String passwordHash) throws SQLException {
        transaction(connection -> {
            execute(connection, "UPDATE users SET password_hash=? WHERE id=?", passwordHash, userId);
            log(connection, userId, "PASSWORD_CHANGED", "User changed own account password");
            return null;
        });
    }

    public void deactivatePatient(long userId) throws SQLException {
        transaction(connection -> {
            int changed = execute(connection, "UPDATE users SET active=? WHERE id=? AND role='PATIENT'", false, userId);
            if (changed == 0) throw new IllegalArgumentException("Patient account was not found.");
            log(connection, userId, "PATIENT_DEACTIVATED", "Patient deactivated own account");
            return null;
        });
    }
}
