package com.nutricare.repository;

import com.nutricare.config.Database;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Module 06 - User & Staff Management (Admin): staff accounts, roles, account status, and the audit trail. */
public class StaffAdminRepository extends BaseRepository {

    public long createStaff(String name, String email, String phone, String passwordHash, String role, long actorId) throws SQLException {
        return transaction(connection -> {
            long id = insert(connection,
                    "INSERT INTO users(full_name,email,phone,password_hash,role,active,verified) VALUES(?,?,?,?,?,?,?)",
                    name, email, phone, passwordHash, role, true, true);
            log(connection, actorId > 0 ? actorId : id, "STAFF_CREATED", role + " account created for " + email);
            return id;
        });
    }

    public List<Map<String, Object>> staff() throws SQLException {
        return list("SELECT id, full_name, email, phone, role, active, created_at FROM users WHERE role<>'PATIENT' ORDER BY role, full_name");
    }

    /** Every patient account, for the administrator's reactivation screen. */
    public List<Map<String, Object>> patientAccounts() throws SQLException {
        return list("SELECT u.id, u.full_name, u.email, u.phone, u.active, u.verified, u.created_at FROM users u WHERE u.role='PATIENT' ORDER BY u.active, u.full_name");
    }

    public void setUserActive(long id, boolean active, long actorId) throws SQLException {
        if (id == actorId && !active) throw new IllegalArgumentException("You cannot deactivate your own account.");
        transaction(connection -> {
            int changed = execute(connection, "UPDATE users SET active=? WHERE id=? AND role<>'PATIENT'", active, id);
            if (changed == 0) throw new IllegalArgumentException("Staff account was not found.");
            log(connection, actorId, "STAFF_STATUS", "Staff account #" + id + " active=" + active);
            return null;
        });
    }

    /** Lets an administrator restore (or suspend) a verified patient account. */
    public void setPatientActive(long id, boolean active, long actorId) throws SQLException {
        transaction(connection -> {
            int changed = execute(connection, "UPDATE users SET active=? WHERE id=? AND role='PATIENT' AND verified=?", active, id, true);
            if (changed == 0) {
                throw new IllegalArgumentException("Patient account was not found or has not completed phone verification.");
            }
            log(connection, actorId, "PATIENT_STATUS", "Patient account #" + id + " active=" + active);
            return null;
        });
    }

    public void updateStaffAccess(long id, String role, String passwordHash, long actorId) throws SQLException {
        if (id == actorId && !"ADMIN".equals(role)) throw new IllegalArgumentException("You cannot remove your own admin role.");
        transaction(connection -> {
            int changed = passwordHash == null
                    ? execute(connection, "UPDATE users SET role=? WHERE id=? AND role<>'PATIENT'", role, id)
                    : execute(connection, "UPDATE users SET role=?, password_hash=? WHERE id=? AND role<>'PATIENT'", role, passwordHash, id);
            if (changed == 0) throw new IllegalArgumentException("Staff account was not found.");
            log(connection, actorId, "STAFF_ACCESS_UPDATED", "Staff account #" + id + " role changed to " + role + (passwordHash == null ? "" : " and password reset"));
            return null;
        });
    }

    public List<Map<String, Object>> auditLogs() throws SQLException {
        String query = Database.isSqlServer()
                ? "SELECT TOP (150) a.*, u.full_name FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id ORDER BY a.created_at DESC, a.id DESC"
                : "SELECT a.*, u.full_name FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id ORDER BY a.created_at DESC, a.id DESC LIMIT 150";
        return list(query);
    }
}
