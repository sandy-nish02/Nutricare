/* =====================================================================================================
   NutriCare - SQL Server demo and checking queries (for testing and the final viva)

   Open in SSMS:  Server name  localhost\SQLEXPRESS  (or  localhost,1433)
                  Authentication  Windows Authentication
                  then choose the NutriCare database (or run the USE line below).

   Select ONE section and press F5 (Execute) to run only that part.
   Sections 8-10 are demonstrations wrapped in BEGIN TRAN ... ROLLBACK so they change nothing.
   ===================================================================================================== */
USE NutriCare;
GO

/* ---------- 1. Overview: tables, row counts, keys, and database objects ---------- */
SELECT t.name AS table_name, SUM(p.rows) AS row_count
FROM sys.tables t JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
GROUP BY t.name ORDER BY t.name;

SELECT fk.name AS foreign_key, OBJECT_NAME(fk.parent_object_id) AS child_table,
       OBJECT_NAME(fk.referenced_object_id) AS parent_table
FROM sys.foreign_keys fk ORDER BY child_table;

SELECT name, type_desc, create_date, modify_date FROM sys.objects WHERE type IN ('P', 'TR') ORDER BY type_desc;

SELECT i.name AS index_name, OBJECT_NAME(i.object_id) AS table_name, i.is_unique, i.filter_definition
FROM sys.indexes i WHERE i.is_unique = 1 AND OBJECT_SCHEMA_NAME(i.object_id) = 'dbo' ORDER BY table_name;
GO

/* ---------- 2. Module 01 User & Patient: accounts, profiles, and one-time codes ---------- */
SELECT id, full_name, email, phone, role, active, verified, created_at FROM users ORDER BY role, id;

SELECT p.id AS patient_id, u.full_name, u.email, p.date_of_birth, p.gender, p.height_cm, p.weight_kg,
       p.allergies, p.conditions, p.medications
FROM patients p JOIN users u ON u.id = p.user_id;

-- Codes are stored only as SHA-256 hashes; used = 1 once verified, expired, or locked after 5 wrong attempts.
SELECT o.id, u.email, o.purpose, o.attempts, o.used, o.expires_at, LEFT(o.code_hash, 12) + '...' AS hash_preview
FROM otp_codes o JOIN users u ON u.id = o.user_id ORDER BY o.id DESC;
GO

/* ---------- 3. Module 04 Appointments: schedule with patient and care provider ---------- */
SELECT a.id, a.appointment_time, pu.full_name AS patient, su.full_name AS care_provider, su.role,
       a.status, a.change_reason, a.purpose
FROM appointments a
JOIN patients p ON p.id = a.patient_id JOIN users pu ON pu.id = p.user_id
JOIN users su ON su.id = a.staff_id
ORDER BY a.appointment_time DESC;

-- Open appointments that hold a 30-minute slot (PENDING / CONFIRMED).
SELECT staff_id, appointment_time, status FROM appointments WHERE status IN ('PENDING', 'CONFIRMED') ORDER BY staff_id, appointment_time;
GO

/* ---------- 4. Module 03 Health check-ups: BMI and abnormal flags ---------- */
SELECT c.id, c.checked_at, pu.full_name AS patient, du.full_name AS doctor, c.appointment_id,
       c.height_cm, c.weight_kg, c.bmi, c.systolic, c.diastolic, c.blood_sugar, c.abnormal
FROM checkups c
JOIN patients p ON p.id = c.patient_id JOIN users pu ON pu.id = p.user_id
JOIN users du ON du.id = c.doctor_id
ORDER BY c.checked_at DESC;
GO

/* ---------- 5. Module 02 Diet plans: status, review flag, and the one-ACTIVE-plan rule ---------- */
SELECT d.id, pu.full_name AS patient, nu.full_name AS nutritionist, d.title, d.daily_calories,
       d.status, d.review_needed, d.start_date, d.end_date
FROM diet_plans d
JOIN patients p ON p.id = d.patient_id JOIN users pu ON pu.id = p.user_id
JOIN users nu ON nu.id = d.nutritionist_id
ORDER BY d.created_at DESC;

-- Should always return no rows: no patient may have two ACTIVE plans.
SELECT patient_id, COUNT(*) AS active_plans FROM diet_plans WHERE status = 'ACTIVE' GROUP BY patient_id HAVING COUNT(*) > 1;
GO

/* ---------- 6. Module 05 Payments: status, verifier, and slip size ---------- */
SELECT pay.id, pu.full_name AS patient, pay.transaction_ref, pay.amount, pay.method, pay.status,
       pay.appointment_id, vu.full_name AS verified_by, pay.verified_at,
       pay.payment_slip_name, DATALENGTH(pay.payment_slip_data) AS slip_bytes
FROM payments pay
JOIN patients p ON p.id = pay.patient_id JOIN users pu ON pu.id = p.user_id
LEFT JOIN users vu ON vu.id = pay.verified_by
ORDER BY pay.submitted_at DESC;

-- Verified revenue this month (same figure as the Finance dashboard).
SELECT COALESCE(SUM(amount), 0) AS verified_this_month
FROM payments WHERE status = 'VERIFIED' AND verified_at >= DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1);
GO

/* ---------- 7. Module 06 Admin: audit trail (newest first) ---------- */
SELECT TOP (50) a.created_at, COALESCE(u.full_name, 'Database (outside NutriCare)') AS actor, a.action, a.details
FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id
ORDER BY a.created_at DESC, a.id DESC;
GO

/* ---------- 8. DEMO: trigger trg_AuditAppointmentStatusChange ----------
   Changes made through the NutriCare website are audited by the application (with the signed-in user and
   reason), so the trigger stays silent for them. A change made directly here in SSMS has no NutriCare
   session tag, so the trigger records it with your SQL login. Needs at least one PENDING appointment. */
DECLARE @appointment BIGINT = (SELECT TOP 1 id FROM appointments WHERE status = 'PENDING' ORDER BY id DESC);
IF @appointment IS NULL
    SELECT 'No PENDING appointment yet - book one on the website first.' AS note;
ELSE
BEGIN
    BEGIN TRAN;
        SELECT @appointment AS appointment_used;
        UPDATE appointments SET status = 'CONFIRMED' WHERE id = @appointment;
        SELECT TOP 3 created_at, user_id, action, details FROM audit_logs ORDER BY id DESC;   -- new trigger row, user_id NULL
    ROLLBACK;   -- undoes both the update and the audit row
END;
GO

/* ---------- 9. DEMO: stored procedure sp_VerifyPaymentSlip ----------
   Verifies a SUBMITTED payment, writes the audit entry, and confirms the linked PENDING appointment, all in one
   transaction. It refuses non-finance verifiers and payments that are not SUBMITTED. Needs one SUBMITTED payment. */
DECLARE @payment BIGINT = (SELECT TOP 1 id FROM payments WHERE status = 'SUBMITTED' ORDER BY id DESC);
DECLARE @finance BIGINT = (SELECT TOP 1 id FROM users WHERE role = 'FINANCE' AND active = 1 ORDER BY id);
IF @payment IS NULL
    SELECT 'No SUBMITTED payment yet - submit one on the website first.' AS note;
ELSE
BEGIN
    BEGIN TRAN;
        SELECT @payment AS payment_used, @finance AS verifier;
        EXEC dbo.sp_VerifyPaymentSlip @PaymentID = @payment, @VerifierID = @finance, @Remarks = N'Viva demonstration';
        SELECT id, status, verified_by, verified_at FROM payments WHERE id = @payment;
        SELECT a.id, a.status FROM appointments a JOIN payments p ON p.appointment_id = a.id WHERE p.id = @payment;
    IF @@TRANCOUNT > 0 ROLLBACK;
END;
GO

-- Error case: a doctor is not allowed to verify payments. The procedure rejects it with
-- "Authorization Error ..." (reported as error 50000) and rolls everything back.
DECLARE @payment2 BIGINT = (SELECT TOP 1 id FROM payments ORDER BY id DESC);
DECLARE @doctor BIGINT = (SELECT TOP 1 id FROM users WHERE role = 'DOCTOR' ORDER BY id);
BEGIN TRY
    EXEC dbo.sp_VerifyPaymentSlip @PaymentID = @payment2, @VerifierID = @doctor;
END TRY
BEGIN CATCH
    SELECT ERROR_NUMBER() AS error_number, ERROR_MESSAGE() AS error_message;
END CATCH;
GO

/* ---------- 10. DEMO: constraints protecting the data ---------- */
-- Unique email (UQ on users.email): the second insert fails with error 2627.
BEGIN TRAN;
    BEGIN TRY
        INSERT INTO users (full_name, email, password_hash, role) VALUES (N'Copy', N'patient@nutricare.lk', N'x', N'PATIENT');
    END TRY
    BEGIN CATCH
        SELECT ERROR_NUMBER() AS error_number, ERROR_MESSAGE() AS error_message;
    END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK;
GO

-- Foreign key: an appointment for a patient that does not exist fails with error 547.
BEGIN TRAN;
    BEGIN TRY
        INSERT INTO appointments (patient_id, staff_id, appointment_time, purpose) VALUES (999999, 1, SYSDATETIME(), N'Invalid');
    END TRY
    BEGIN CATCH
        SELECT ERROR_NUMBER() AS error_number, ERROR_MESSAGE() AS error_message;
    END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK;
GO
