package com.nutricare.repository;

import com.nutricare.config.Database;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Module 05 - Payment & Billing Management, including the SQL Server stored procedure. */
class PaymentRepositoryTest extends IntegrationTestBase {

    @Test
    void refundRequiresVerifiedPaymentAndCancelledAppointmentWithReason() throws Exception {
        long patientUser = newPatient("Refund Patient");
        long patientId = patientId(patientUser);
        long doctor = newStaff("Refund Doctor", "DOCTOR");
        long finance = newStaff("Refund Finance", "FINANCE");
        long appointmentId = book(patientUser, doctor, 3);
        payments.createPayment(patientId, appointmentId, new BigDecimal("2500.00"), "REFUND-" + RUN, "CASH", patientUser);
        long paymentId = paymentId(patientUser);

        assertThrows(IllegalArgumentException.class, () -> payments.updatePaymentStatus(paymentId, "REFUNDED", finance),
                "unverified money cannot be refunded");
        assertTrue(payments.updatePaymentStatus(paymentId, "VERIFIED", finance), "verification confirms the visit");
        assertEquals("CONFIRMED", appointmentStatus(patientUser));
        assertThrows(IllegalArgumentException.class, () -> payments.updatePaymentStatus(paymentId, "REFUNDED", finance),
                "the visit has not been cancelled");

        appointments.cancelOwnAppointment(appointmentId, patientUser, "Patient requested cancellation");
        payments.updatePaymentStatus(paymentId, "REFUNDED", finance);
        Map<String, Object> payment = payments.payments(patientUser, "PATIENT").get(0);
        assertEquals("REFUNDED", payment.get("status"));
        assertEquals("Refund Finance", payment.get("verifier_name"), "the original verifier stays on record");
    }

    @Test
    void reviewedPaymentsCannotBeFlippedAndDuplicatesAreRejected() throws Exception {
        long patientUser = newPatient("Flip Patient");
        long patientId = patientId(patientUser);
        long finance = newStaff("Flip Finance", "FINANCE");
        payments.createPayment(patientId, null, new BigDecimal("1000.00"), "FLIP-" + RUN, "CASH", patientUser);
        long paymentId = paymentId(patientUser);

        payments.updatePaymentStatus(paymentId, "REJECTED", finance);
        assertThrows(IllegalArgumentException.class, () -> payments.updatePaymentStatus(paymentId, "VERIFIED", finance));
        assertThrows(IllegalArgumentException.class, () -> payments.createPayment(patientId, null,
                new BigDecimal("1000.00"), "FLIP-" + RUN, "CASH", patientUser), "duplicate reference");
    }

    @Test
    void bankTransferSlipIsStoredAndReturned() throws Exception {
        long patientUser = newPatient("Slip Patient");
        byte[] slip = {37, 80, 68, 70, 45, 49};
        payments.createPayment(patientId(patientUser), null, new BigDecimal("750.50"), "SLIP-" + RUN,
                "BANK_TRANSFER", "slip.pdf", "application/pdf", slip, patientUser);
        long paymentId = paymentId(patientUser);
        assertArrayEquals(slip, (byte[]) payments.paymentDocument(paymentId, patientUser, "PATIENT")
                .orElseThrow().get("payment_slip_data"));
        long otherPatient = newPatient("Nosy Patient");
        assertTrue(payments.paymentDocument(paymentId, otherPatient, "PATIENT").isEmpty(),
                "patients cannot open someone else's slip");
    }

    @Test
    void cancelledAppointmentsCannotBePaidFor() throws Exception {
        long patientUser = newPatient("Late Patient");
        long doctor = newStaff("Late Doctor", "DOCTOR");
        long appointmentId = book(patientUser, doctor, 11);
        appointments.cancelOwnAppointment(appointmentId, patientUser, "Changed plans");
        assertThrows(IllegalArgumentException.class, () -> payments.createPayment(patientId(patientUser), appointmentId,
                new BigDecimal("500.00"), "LATE-" + RUN, "CASH", patientUser));
    }

    @Test
    void sqlServerProcedureVerifiesSlipAndConfirmsAppointment() throws Exception {
        assumeTrue(onSqlServer(), "the stored procedure only exists on SQL Server");
        long patientUser = newPatient("Procedure Patient");
        long doctor = newStaff("Procedure Doctor", "DOCTOR");
        long finance = newStaff("Procedure Finance", "FINANCE");
        long appointmentId = book(patientUser, doctor, 12);
        payments.createPayment(patientId(patientUser), appointmentId, new BigDecimal("3000.00"), "PROC-" + RUN, "CASH", patientUser);
        long paymentId = paymentId(patientUser);

        assertThrows(SQLException.class, () -> verifyWithProcedure(paymentId, doctor), "doctors lack financial authority");
        verifyWithProcedure(paymentId, finance);
        assertEquals("VERIFIED", payments.payments(patientUser, "PATIENT").get(0).get("status"));
        assertEquals("CONFIRMED", appointmentStatus(patientUser));
        assertThrows(SQLException.class, () -> verifyWithProcedure(paymentId, finance), "already verified");
    }

    private static void verifyWithProcedure(long paymentId, long verifierId) throws SQLException {
        try (Connection connection = Database.connect();
             CallableStatement call = connection.prepareCall("{call dbo.sp_VerifyPaymentSlip(?, ?)}")) {
            call.setLong(1, paymentId);
            call.setLong(2, verifierId);
            call.execute();
        }
    }
}
