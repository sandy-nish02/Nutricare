package com.nutricare.repository;

import com.nutricare.config.Database;
import com.nutricare.service.PaymentRules;

import java.math.BigDecimal;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Module 05 - Payment & Billing Management: slips, verification state machine, receipts, and refunds. */
public class PaymentRepository extends BaseRepository {

    public List<Map<String, Object>> payments(long userId, String role) throws SQLException {
        String base = "SELECT pay.id, pay.patient_id, pay.appointment_id, pay.amount, pay.transaction_ref, pay.method, pay.payment_slip_name, pay.status, pay.submitted_at, pay.verified_at, pu.full_name AS patient_name, vu.full_name AS verifier_name, a.appointment_time FROM payments pay " +
                "JOIN patients p ON p.id=pay.patient_id JOIN users pu ON pu.id=p.user_id LEFT JOIN users vu ON vu.id=pay.verified_by LEFT JOIN appointments a ON a.id=pay.appointment_id ";
        List<Map<String, Object>> rows = "PATIENT".equals(role)
                ? list(base + "WHERE p.user_id=? ORDER BY pay.submitted_at DESC", userId)
                : list(base + "ORDER BY pay.submitted_at DESC");
        for (Map<String, Object> row : rows) row.put("next_statuses", PaymentRules.nextStatuses(String.valueOf(row.get("status"))));
        return rows;
    }

    /** Appointments a payment can be linked to: the patient's own, or every open/completed one for finance. */
    public List<Map<String, Object>> payableAppointments(long userId, String role) throws SQLException {
        String base = "SELECT a.id, a.appointment_time, a.status, pu.full_name AS patient_name, su.full_name AS staff_name FROM appointments a " +
                "JOIN patients p ON p.id=a.patient_id JOIN users pu ON pu.id=p.user_id JOIN users su ON su.id=a.staff_id " +
                "WHERE a.status IN ('PENDING','CONFIRMED','COMPLETED') ";
        return "PATIENT".equals(role)
                ? list(base + "AND p.user_id=? ORDER BY a.appointment_time DESC", userId)
                : list(base + "ORDER BY a.appointment_time DESC");
    }

    public void createPayment(long patientId, Long appointmentId, BigDecimal amount, String reference,
                              String method, long actorId) throws SQLException {
        createPayment(patientId, appointmentId, amount, reference, method, null, null, null, actorId);
    }

    public void createPayment(long patientId, Long appointmentId, BigDecimal amount, String reference,
                              String method, String slipName, String slipType, byte[] slipData, long actorId) throws SQLException {
        try {
            transaction(connection -> {
                if (appointmentId != null) {
                    Map<String, Object> appointment = one(connection, "SELECT status FROM appointments WHERE id=? AND patient_id=?", appointmentId, patientId)
                            .orElseThrow(() -> new IllegalArgumentException("That appointment does not belong to the selected patient."));
                    String status = String.valueOf(appointment.get("status"));
                    if ("CANCELLED".equals(status) || "NO_SHOW".equals(status)) {
                        throw new IllegalArgumentException("That appointment is " + status + " and cannot be paid for.");
                    }
                }
                insert(connection, "INSERT INTO payments(patient_id,appointment_id,amount,transaction_ref,method,payment_slip_name,payment_slip_type,payment_slip_data,status) VALUES(?,?,?,?,?,?,?,?,?)",
                        patientId, appointmentId, amount, reference, method, slipName, slipType,
                        slipData == null ? new TypedNull(Types.VARBINARY) : slipData, "SUBMITTED");
                log(connection, actorId, "PAYMENT_SUBMITTED", "Payment " + reference + " submitted");
                return null;
            });
        } catch (SQLException exception) {
            if (isDuplicateKey(exception)) {
                throw new IllegalArgumentException("That transaction reference was already submitted.");
            }
            throw exception;
        }
    }

    /** One payment including the uploaded slip bytes; patients can only open their own. */
    public Optional<Map<String, Object>> paymentDocument(long id, long userId, String role) throws SQLException {
        String sql = "SELECT pay.id, pay.patient_id, pay.appointment_id, pay.amount, pay.transaction_ref, pay.method, pay.payment_slip_name, pay.payment_slip_type, pay.payment_slip_data, pay.status, pay.submitted_at, pay.verified_at, pu.full_name AS patient_name, vu.full_name AS verifier_name, a.appointment_time " +
                "FROM payments pay JOIN patients p ON p.id=pay.patient_id JOIN users pu ON pu.id=p.user_id LEFT JOIN users vu ON vu.id=pay.verified_by LEFT JOIN appointments a ON a.id=pay.appointment_id WHERE pay.id=?" +
                ("PATIENT".equals(role) ? " AND p.user_id=?" : "");
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if ("PATIENT".equals(role)) bind(statement, id, userId); else bind(statement, id);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) return Optional.empty();
                Map<String, Object> row = new LinkedHashMap<>();
                ResultSetMetaData meta = results.getMetaData();
                for (int column = 1; column <= meta.getColumnCount(); column++) {
                    String label = meta.getColumnLabel(column).toLowerCase(Locale.ROOT);
                    row.put(label, "payment_slip_data".equals(label) ? results.getBytes(column) : value(results.getObject(column)));
                }
                return Optional.of(row);
            }
        }
    }

    /**
     * Applies one step of the payment state machine (see PaymentRules). Verifying a payment confirms its
     * linked pending appointment (design document, sequence 5.5).
     *
     * @return true when a linked appointment was confirmed as a result
     */
    public boolean updatePaymentStatus(long id, String status, long verifierId) throws SQLException {
        return transaction(connection -> {
            Map<String, Object> payment = one(connection,
                    "SELECT pay.status, pay.appointment_id, a.status AS appointment_status, a.change_reason FROM payments pay LEFT JOIN appointments a ON a.id=pay.appointment_id WHERE pay.id=?", id)
                    .orElseThrow(() -> new IllegalArgumentException("Payment was not found."));
            String current = String.valueOf(payment.get("status"));
            if (!PaymentRules.canTransition(current, status)) {
                throw new IllegalArgumentException(current.equals(status)
                        ? "This payment is already " + status + "."
                        : "A " + current + " payment cannot be marked " + status + ".");
            }
            String appointmentStatus = (String) payment.get("appointment_status");
            if ("REFUNDED".equals(status)) {
                String reason = (String) payment.get("change_reason");
                if (!"CANCELLED".equals(appointmentStatus) || reason == null || reason.isBlank()) {
                    throw new IllegalArgumentException("Refunds require a linked cancelled appointment with a recorded reason.");
                }
            }
            // A refund keeps the original verifier on record; who refunded is written to the audit log.
            int changed = "REFUNDED".equals(status)
                    ? execute(connection, "UPDATE payments SET status=? WHERE id=? AND status=?", status, id, current)
                    : execute(connection, "UPDATE payments SET status=?, verified_by=?, verified_at=CURRENT_TIMESTAMP WHERE id=? AND status=?",
                            status, verifierId, id, current);
            if (changed == 0) throw new IllegalArgumentException("This payment was just changed by someone else. Refresh and try again.");
            boolean appointmentConfirmed = false;
            if ("VERIFIED".equals(status) && "PENDING".equals(appointmentStatus)) {
                Object appointmentId = payment.get("appointment_id");
                appointmentConfirmed = execute(connection,
                        "UPDATE appointments SET status='CONFIRMED' WHERE id=? AND status='PENDING'", appointmentId) > 0;
                if (appointmentConfirmed) {
                    log(connection, verifierId, "APPOINTMENT_STATUS",
                            "Appointment #" + appointmentId + " confirmed after payment #" + id + " was verified");
                }
            }
            log(connection, verifierId, "PAYMENT_STATUS", "Payment #" + id + " changed from " + current + " to " + status);
            return appointmentConfirmed;
        });
    }
}
