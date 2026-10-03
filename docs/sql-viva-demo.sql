/* =====================================================================================================
   NutriCare - SQL commands demonstration for the viva
   (CREATE, ALTER, INSERT, SELECT, UPDATE ... SET, DELETE, INNER/LEFT JOIN, GROUP BY, VIEW, transactions)

   How to run (SSMS):
     1. Start "SQL Server Management Studio 22".
     2. Connect: Server name  localhost\SQLEXPRESS   Authentication  Windows Authentication
        (tick "Trust server certificate" if it is offered) -> Connect.
     3. File -> Open -> File... -> this file (docs\sql-viva-demo.sql).
     4. Select ONE part with the mouse (from its title line down to the next part) and press F5 (Execute).
        Results appear in the "Results" tab, row counts and errors in the "Messages" tab.

   PART 0  loads sample patients, appointments, check-ups, diet plans and payments (run ONCE, before the viva).
   PART 1  our own demo table (food_items): CREATE TABLE, INSERT, SELECT, UPDATE ... SET, ALTER, DELETE, INDEX.
   PART 2  reports on the real NutriCare tables: INNER JOIN, LEFT JOIN, CASE, GROUP BY, HAVING, subqueries.
   PART 3  UPDATE / DELETE / INSERT on real app data inside BEGIN TRAN ... ROLLBACK (nothing is kept).
   PART 4  CREATE VIEW.
   PART 5  the stored procedure and the trigger.
   PART 6  constraints refusing bad data.
   PART 7  clean-up: removes the sample data and the demo objects again.

   Sample patients can also sign in to the website with password NutriCare123!
   ===================================================================================================== */
USE NutriCare;
GO
-- SSMS uses these settings by default; they are required because checkups has a filtered unique index.
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

/* ===================================== PART 0 - SAMPLE DATA (run once) ==================================== */
SET NOCOUNT ON;
IF EXISTS (SELECT 1 FROM dbo.users WHERE email = N'nimal.perera@demo.lk')
BEGIN
    PRINT 'Sample data is already loaded - nothing to do.';
END
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;

        -- Staff accounts created by NutriCare on first start, and the demo password hash (NutriCare123!).
        DECLARE @doctor BIGINT       = (SELECT id FROM dbo.users WHERE email = N'doctor@nutricare.lk');
        DECLARE @doctor2 BIGINT      = (SELECT id FROM dbo.users WHERE email = N'appointments@nutricare.lk');
        DECLARE @nutritionist BIGINT = (SELECT id FROM dbo.users WHERE email = N'nutritionist@nutricare.lk');
        DECLARE @finance BIGINT      = (SELECT id FROM dbo.users WHERE email = N'finance@nutricare.lk');
        DECLARE @hash NVARCHAR(100)  = (SELECT password_hash FROM dbo.users WHERE email = N'patient@nutricare.lk');
        IF @doctor IS NULL OR @hash IS NULL
            THROW 50010, 'Start NutriCare once first so it creates the demo accounts.', 1;

        DECLARE @today DATETIME2(6) = CAST(CAST(GETDATE() AS DATE) AS DATETIME2(6));   -- today 00:00
        DECLARE @u1 BIGINT, @u2 BIGINT, @u3 BIGINT, @p1 BIGINT, @p2 BIGINT, @p3 BIGINT;
        DECLARE @a1 BIGINT, @a2 BIGINT, @a3 BIGINT, @a4 BIGINT, @a5 BIGINT;

        -- Patients: a users row (login) plus a patients row (health profile).
        INSERT INTO dbo.users (full_name, email, phone, password_hash, role, active, verified)
        VALUES (N'Nimal Perera', N'nimal.perera@demo.lk', N'0771112233', @hash, N'PATIENT', 1, 1);
        SET @u1 = SCOPE_IDENTITY();
        INSERT INTO dbo.patients (user_id, date_of_birth, gender, height_cm, weight_kg, allergies, conditions, medications, emergency_contact)
        VALUES (@u1, '1988-04-12', N'Male', 172, 84, N'Peanut, Shellfish', N'Type 2 diabetes', N'Metformin 500 mg', N'Sunethra Perera 0779998877');
        SET @p1 = SCOPE_IDENTITY();

        INSERT INTO dbo.users (full_name, email, phone, password_hash, role, active, verified)
        VALUES (N'Kavindi Silva', N'kavindi.silva@demo.lk', N'0712223344', @hash, N'PATIENT', 1, 1);
        SET @u2 = SCOPE_IDENTITY();
        INSERT INTO dbo.patients (user_id, date_of_birth, gender, height_cm, weight_kg, allergies)
        VALUES (@u2, '1995-09-03', N'Female', 160, 52, N'Milk');
        SET @p2 = SCOPE_IDENTITY();

        INSERT INTO dbo.users (full_name, email, phone, password_hash, role, active, verified)
        VALUES (N'Ashan Fernando', N'ashan.fernando@demo.lk', N'0763334455', @hash, N'PATIENT', 1, 1);
        SET @u3 = SCOPE_IDENTITY();
        INSERT INTO dbo.patients (user_id, date_of_birth, gender, height_cm, weight_kg, conditions, medications)
        VALUES (@u3, '1979-01-25', N'Male', 168, 95, N'Hypertension', N'Amlodipine 5 mg');
        SET @p3 = SCOPE_IDENTITY();

        -- Appointments in every status (DATEADD keeps them relative to today).
        INSERT INTO dbo.appointments (patient_id, staff_id, appointment_time, purpose, status)
        VALUES (@p1, @doctor, DATEADD(HOUR, 9, DATEADD(DAY, -7, @today)), N'Diabetes and weight review', N'COMPLETED');
        SET @a1 = SCOPE_IDENTITY();
        INSERT INTO dbo.appointments (patient_id, staff_id, appointment_time, purpose, status)
        VALUES (@p1, @nutritionist, DATEADD(HOUR, 10, DATEADD(DAY, 2, @today)), N'Diet plan follow-up', N'CONFIRMED');
        SET @a2 = SCOPE_IDENTITY();
        INSERT INTO dbo.appointments (patient_id, staff_id, appointment_time, purpose, status)
        VALUES (@p2, @nutritionist, DATEADD(HOUR, 11, DATEADD(DAY, 1, @today)), N'First nutrition consultation', N'PENDING');
        SET @a3 = SCOPE_IDENTITY();
        INSERT INTO dbo.appointments (patient_id, staff_id, appointment_time, purpose, status)
        VALUES (@p3, @doctor, DATEADD(HOUR, 14, DATEADD(DAY, -3, @today)), N'Blood pressure check', N'COMPLETED');
        SET @a4 = SCOPE_IDENTITY();
        INSERT INTO dbo.appointments (patient_id, staff_id, appointment_time, purpose, status, change_reason)
        VALUES (@p3, @doctor2, DATEADD(MINUTE, 930, DATEADD(DAY, 5, @today)), N'Medication review', N'CANCELLED', N'Patient travelling abroad');
        SET @a5 = SCOPE_IDENTITY();

        -- Check-ups (BMI = kg / m^2). Two are linked to completed appointments (FR15), one is a walk-in.
        INSERT INTO dbo.checkups (patient_id, doctor_id, appointment_id, checked_at, height_cm, weight_kg, bmi, systolic, diastolic, blood_sugar, notes, abnormal)
        VALUES (@p1, @doctor, @a1, DATEADD(MINUTE, 570, DATEADD(DAY, -7, @today)), 172, 84, 28.39, 135, 85, 162, N'Fasting sugar high; refer to nutritionist.', 1),
               (@p3, @doctor, @a4, DATEADD(MINUTE, 870, DATEADD(DAY, -3, @today)), 168, 95, 33.66, 150, 95, 110, N'Blood pressure above target.', 1),
               (@p2, @doctor2, NULL, DATEADD(HOUR, 16, DATEADD(DAY, -14, @today)), 160, 52, 20.31, 112, 72, 92, N'All readings normal.', 0);

        -- Diet plans: one ACTIVE, one ACTIVE that needs review, one DRAFT (meals avoid each patient's allergies).
        INSERT INTO dbo.diet_plans (patient_id, nutritionist_id, title, daily_calories, meals, start_date, end_date, status, review_needed)
        VALUES (@p1, @nutritionist, N'Low-GI diabetic plan', 1800,
                CONCAT(N'Breakfast: oats with low-fat milk', NCHAR(10), N'Lunch: red rice, dhal, green salad', NCHAR(10), N'Dinner: grilled chicken and vegetables'),
                CAST(DATEADD(DAY, -6, @today) AS DATE), CAST(DATEADD(DAY, 24, @today) AS DATE), N'ACTIVE', 0),
               (@p3, @nutritionist, N'Low-salt heart plan', 1700,
                CONCAT(N'Breakfast: string hoppers with dhal', NCHAR(10), N'Lunch: brown rice, fish curry (no added salt)', NCHAR(10), N'Dinner: vegetable soup'),
                CAST(DATEADD(DAY, -2, @today) AS DATE), CAST(DATEADD(DAY, 28, @today) AS DATE), N'ACTIVE', 1),
               (@p2, @nutritionist, N'Balanced weight-gain plan', 2200,
                CONCAT(N'Breakfast: egg hoppers and banana', NCHAR(10), N'Lunch: rice, chicken curry, beans', NCHAR(10), N'Snack: peanut butter toast'),
                CAST(DATEADD(DAY, 1, @today) AS DATE), CAST(DATEADD(DAY, 31, @today) AS DATE), N'DRAFT', 0);

        -- Payments in every status. Bank transfers carry a small PNG "slip" (stored as VARBINARY).
        DECLARE @slip VARBINARY(MAX) = 0x89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000A49444154789C63000100000500010D0A2DB40000000049454E44AE426082;
        INSERT INTO dbo.payments (patient_id, appointment_id, amount, transaction_ref, method, payment_slip_name, payment_slip_type, payment_slip_data, status, submitted_at, verified_by, verified_at)
        VALUES (@p1, @a1, 3500.00, N'BOC-2026-0001', N'BANK_TRANSFER', N'nimal-slip.png', N'image/png', @slip, N'VERIFIED', DATEADD(DAY, -8, @today), @finance, DATEADD(DAY, -7, @today)),
               (@p2, @a3, 2500.00, N'CASH-2026-0002', N'CASH', NULL, NULL, NULL, N'SUBMITTED', SYSDATETIME(), NULL, NULL),
               (@p3, @a4, 3000.00, N'SAMPATH-2026-0003', N'BANK_TRANSFER', N'ashan-slip.png', N'image/png', @slip, N'REJECTED', DATEADD(DAY, -4, @today), @finance, DATEADD(DAY, -4, @today)),
               (@p3, @a4, 3000.00, N'CARD-2026-0004', N'CARD', NULL, NULL, NULL, N'VERIFIED', DATEADD(DAY, -3, @today), @finance, DATEADD(DAY, -3, @today));

        INSERT INTO dbo.audit_logs (user_id, action, details)
        VALUES (NULL, N'SAMPLE_DATA_LOADED', N'Viva sample data inserted from SSMS (3 patients, 5 appointments, 3 check-ups, 3 diet plans, 4 payments)');

        COMMIT TRANSACTION;
        PRINT 'Sample data loaded.';
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        PRINT CONCAT('Nothing was loaded: ', ERROR_MESSAGE());
    END CATCH;
END;
GO

/* ===================================== PART 1 - OUR OWN TABLE: food_items ================================= */
-- The class diagram has a Food Item entity with allergen tags. This demo table shows how it could be stored.
-- (The web application does not use this table yet - it is only for demonstrating SQL.)

-- 1a. CREATE TABLE with a primary key, UNIQUE, CHECK and DEFAULT constraints
DROP TABLE IF EXISTS dbo.food_items;
CREATE TABLE dbo.food_items (
    id                INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_food_items PRIMARY KEY,
    name              NVARCHAR(80)  NOT NULL CONSTRAINT UQ_food_items_name UNIQUE,
    category          NVARCHAR(30)  NOT NULL,
    calories_per_100g INT           NOT NULL CONSTRAINT CK_food_items_calories CHECK (calories_per_100g BETWEEN 0 AND 900),
    allergen          NVARCHAR(40)  NULL,
    is_active         BIT           NOT NULL CONSTRAINT DF_food_items_active DEFAULT 1,
    created_at        DATETIME2(0)  NOT NULL CONSTRAINT DF_food_items_created DEFAULT SYSDATETIME()
);

-- 1b. INSERT several rows at once
INSERT INTO dbo.food_items (name, category, calories_per_100g, allergen)
VALUES (N'Rolled oats', N'Grain', 379, NULL),
       (N'Red rice', N'Grain', 356, NULL),
       (N'Low-fat milk', N'Dairy', 42, N'Milk'),
       (N'Peanut butter', N'Spread', 588, N'Peanut'),
       (N'Prawns', N'Seafood', 99, N'Shellfish'),
       (N'Dhal (red lentils)', N'Legume', 116, NULL),
       (N'Chicken breast', N'Meat', 165, NULL),
       (N'Instant noodles', N'Processed', 436, N'Wheat');

-- 1c. SELECT with WHERE and ORDER BY
SELECT id, name, category, calories_per_100g, allergen FROM dbo.food_items ORDER BY category, name;
SELECT name, calories_per_100g FROM dbo.food_items WHERE allergen IS NULL AND calories_per_100g < 200;

-- 1d. UPDATE ... SET (change one value, then several rows)
UPDATE dbo.food_items SET calories_per_100g = 389 WHERE name = N'Rolled oats';
UPDATE dbo.food_items SET is_active = 0 WHERE category = N'Processed';
SELECT name, calories_per_100g, is_active FROM dbo.food_items WHERE name = N'Rolled oats' OR category = N'Processed';

-- 1e. ALTER TABLE (add a column) and fill it with UPDATE ... SET ... CASE
ALTER TABLE dbo.food_items ADD protein_g DECIMAL(5,1) NULL;
GO
UPDATE dbo.food_items
SET protein_g = CASE name WHEN N'Chicken breast' THEN 31.0 WHEN N'Dhal (red lentils)' THEN 9.0 WHEN N'Prawns' THEN 24.0 ELSE 5.0 END;
SELECT name, protein_g FROM dbo.food_items ORDER BY protein_g DESC;

-- 1f. DELETE a row
DELETE FROM dbo.food_items WHERE name = N'Instant noodles';
SELECT COUNT(*) AS food_items_left FROM dbo.food_items;

-- 1g. CREATE INDEX to speed up searches by category
CREATE INDEX IX_food_items_category ON dbo.food_items (category);

-- 1h. INNER JOIN our table with the real patients: which foods are unsafe for which patient?
SELECT u.full_name AS patient, p.allergies, f.name AS unsafe_food, f.allergen
FROM dbo.patients AS p
INNER JOIN dbo.users AS u ON u.id = p.user_id
INNER JOIN dbo.food_items AS f ON p.allergies LIKE N'%' + f.allergen + N'%'
ORDER BY patient, unsafe_food;
GO

/* ===================================== PART 2 - REPORTS ON THE REAL TABLES (read only) ==================== */
-- 2a. INNER JOIN: the appointment schedule (users is joined twice: once for the patient, once for the staff member)
SELECT a.id, a.appointment_time, pu.full_name AS patient, su.full_name AS care_provider, su.role, a.status, a.purpose
FROM dbo.appointments AS a
INNER JOIN dbo.patients AS p  ON p.id  = a.patient_id
INNER JOIN dbo.users    AS pu ON pu.id = p.user_id
INNER JOIN dbo.users    AS su ON su.id = a.staff_id
ORDER BY a.appointment_time;

-- 2b. LEFT JOIN: every appointment with its payment, if there is one (NULL = not paid yet)
SELECT a.id AS appointment_id, pu.full_name AS patient, a.status AS appointment_status,
       pay.transaction_ref, pay.amount, pay.status AS payment_status
FROM dbo.appointments AS a
INNER JOIN dbo.patients AS p  ON p.id = a.patient_id
INNER JOIN dbo.users    AS pu ON pu.id = p.user_id
LEFT JOIN  dbo.payments AS pay ON pay.appointment_id = a.id
ORDER BY a.id;

-- 2c. CASE: BMI category for every check-up (same rule as HealthRules.bmiLabel in Java)
SELECT pu.full_name AS patient, c.checked_at, c.bmi,
       CASE WHEN c.bmi < 18.5 THEN 'Underweight' WHEN c.bmi < 25 THEN 'Healthy'
            WHEN c.bmi < 30 THEN 'Overweight' ELSE 'Obesity range' END AS bmi_category,
       CASE c.abnormal WHEN 1 THEN 'REVIEW' ELSE 'normal' END AS assessment
FROM dbo.checkups AS c
INNER JOIN dbo.patients AS p  ON p.id = c.patient_id
INNER JOIN dbo.users    AS pu ON pu.id = p.user_id
ORDER BY c.checked_at DESC;

-- 2d. GROUP BY + COUNT + HAVING: workload per care provider
SELECT su.full_name AS care_provider, su.role, COUNT(*) AS appointments,
       SUM(CASE WHEN a.status IN ('PENDING', 'CONFIRMED') THEN 1 ELSE 0 END) AS still_open
FROM dbo.appointments AS a
INNER JOIN dbo.users AS su ON su.id = a.staff_id
GROUP BY su.full_name, su.role
HAVING COUNT(*) >= 1
ORDER BY appointments DESC;

-- 2e. SUM: verified revenue by payment method
SELECT method, COUNT(*) AS payments, SUM(amount) AS verified_total
FROM dbo.payments
WHERE status = 'VERIFIED'
GROUP BY method;

-- 2f. Subquery with NOT EXISTS: patients who have never had a check-up
SELECT u.full_name, u.email
FROM dbo.patients AS p
INNER JOIN dbo.users AS u ON u.id = p.user_id
WHERE NOT EXISTS (SELECT 1 FROM dbo.checkups AS c WHERE c.patient_id = p.id);

-- 2g. Aggregates: average, highest and lowest BMI recorded
SELECT COUNT(*) AS checkups, AVG(bmi) AS average_bmi, MAX(bmi) AS highest_bmi, MIN(bmi) AS lowest_bmi FROM dbo.checkups;
GO

/* ===================================== PART 3 - CHANGING REAL DATA SAFELY (rolled back) ================== */
-- Everything between BEGIN TRANSACTION and ROLLBACK is undone at the end, so the demo changes nothing.
DECLARE @pendingBefore INT = (SELECT COUNT(*) FROM dbo.appointments WHERE status = 'PENDING');
DECLARE @draftsBefore INT  = (SELECT COUNT(*) FROM dbo.diet_plans WHERE status = 'DRAFT');
DECLARE @appointment BIGINT;

BEGIN TRANSACTION;
    -- UPDATE ... SET: confirm the next pending appointment (the trigger records this change, see the audit row)
    SET @appointment = (SELECT TOP 1 id FROM dbo.appointments WHERE status = 'PENDING' ORDER BY appointment_time);
    UPDATE dbo.appointments SET status = 'CONFIRMED' WHERE id = @appointment;
    SELECT @appointment AS confirmed_appointment, @@ROWCOUNT AS rows_updated;
    SELECT TOP 1 action, details FROM dbo.audit_logs ORDER BY id DESC;

    -- UPDATE with arithmetic on several rows
    UPDATE dbo.patients SET weight_kg = weight_kg + 1.5 WHERE weight_kg IS NOT NULL;
    SELECT @@ROWCOUNT AS patients_weight_updated;

    -- DELETE: remove all draft diet plans
    DELETE FROM dbo.diet_plans WHERE status = 'DRAFT';
    SELECT @@ROWCOUNT AS draft_plans_deleted;

    -- INSERT
    INSERT INTO dbo.audit_logs (user_id, action, details) VALUES (NULL, N'VIVA_DEMO', N'Inserted inside a transaction');
    SELECT COUNT(*) AS viva_rows_inside_transaction FROM dbo.audit_logs WHERE action = 'VIVA_DEMO';
ROLLBACK TRANSACTION;

-- Prove that nothing was kept
SELECT @pendingBefore AS pending_before, (SELECT COUNT(*) FROM dbo.appointments WHERE status = 'PENDING') AS pending_after_rollback,
       @draftsBefore AS drafts_before, (SELECT COUNT(*) FROM dbo.diet_plans WHERE status = 'DRAFT') AS drafts_after_rollback,
       (SELECT COUNT(*) FROM dbo.audit_logs WHERE action = 'VIVA_DEMO') AS viva_rows_after_rollback;
GO

/* ===================================== PART 4 - CREATE VIEW ============================================== */
-- A view is a saved SELECT. This one summarises each patient across four modules.
CREATE OR ALTER VIEW dbo.vw_patient_summary AS
SELECT p.id AS patient_id, u.full_name, u.email, p.allergies,
       (SELECT COUNT(*) FROM dbo.appointments a WHERE a.patient_id = p.id) AS appointments,
       (SELECT TOP 1 c.bmi FROM dbo.checkups c WHERE c.patient_id = p.id ORDER BY c.checked_at DESC) AS latest_bmi,
       (SELECT TOP 1 d.title FROM dbo.diet_plans d WHERE d.patient_id = p.id AND d.status = 'ACTIVE') AS active_plan,
       (SELECT COALESCE(SUM(pay.amount), 0) FROM dbo.payments pay WHERE pay.patient_id = p.id AND pay.status = 'VERIFIED') AS total_paid
FROM dbo.patients AS p
INNER JOIN dbo.users AS u ON u.id = p.user_id;
GO
SELECT * FROM dbo.vw_patient_summary ORDER BY full_name;
GO

/* ===================================== PART 5 - STORED PROCEDURE AND TRIGGER ============================= */
-- 5a. Show their code
EXEC sp_helptext 'dbo.sp_VerifyPaymentSlip';
EXEC sp_helptext 'dbo.trg_AuditAppointmentStatusChange';
GO
-- 5b. Run the procedure on a SUBMITTED payment (rolled back afterwards)
DECLARE @payment BIGINT = (SELECT TOP 1 id FROM dbo.payments WHERE status = 'SUBMITTED' ORDER BY id DESC);
DECLARE @verifier BIGINT = (SELECT TOP 1 id FROM dbo.users WHERE role = 'FINANCE' AND active = 1 ORDER BY id);
IF @payment IS NULL
    SELECT 'No SUBMITTED payment - run PART 0 or submit one on the website first.' AS note;
ELSE
BEGIN
    BEGIN TRANSACTION;
        EXEC dbo.sp_VerifyPaymentSlip @PaymentID = @payment, @VerifierID = @verifier, @Remarks = N'Viva demonstration';
        SELECT pay.id, pay.status AS payment_status, a.id AS appointment_id, a.status AS appointment_status
        FROM dbo.payments AS pay LEFT JOIN dbo.appointments AS a ON a.id = pay.appointment_id
        WHERE pay.id = @payment;
        SELECT TOP 3 action, details FROM dbo.audit_logs ORDER BY id DESC;
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
END;
GO

/* ===================================== PART 6 - CONSTRAINTS REFUSE BAD DATA ============================== */
-- 6a. UNIQUE: a second account with the same email
BEGIN TRY
    INSERT INTO dbo.users (full_name, email, password_hash, role) VALUES (N'Copy', N'patient@nutricare.lk', N'x', N'PATIENT');
END TRY
BEGIN CATCH
    SELECT ERROR_NUMBER() AS error_number, ERROR_MESSAGE() AS refused_because;
END CATCH;
-- 6b. FOREIGN KEY: deleting a patient who still has appointments
BEGIN TRY
    DELETE FROM dbo.patients WHERE id = (SELECT TOP 1 patient_id FROM dbo.appointments);
END TRY
BEGIN CATCH
    SELECT ERROR_NUMBER() AS error_number, ERROR_MESSAGE() AS refused_because;
END CATCH;
-- 6c. CHECK: calories outside 0-900 in our food_items table (needs PART 1)
BEGIN TRY
    INSERT INTO dbo.food_items (name, category, calories_per_100g) VALUES (N'Impossible food', N'Test', 5000);
END TRY
BEGIN CATCH
    SELECT ERROR_NUMBER() AS error_number, ERROR_MESSAGE() AS refused_because;
END CATCH;
GO

/* ===================================== PART 7 - CLEAN-UP (after the viva) ================================ */
-- Removes the PART 0 sample data (and anything done on the website with those sample patients),
-- the food_items table and the view. The 7 demo accounts stay.
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @sample TABLE (user_id BIGINT, patient_id BIGINT);
    INSERT INTO @sample SELECT u.id, p.id FROM dbo.users u INNER JOIN dbo.patients p ON p.user_id = u.id WHERE u.email LIKE N'%@demo.lk';

    DELETE FROM dbo.payments     WHERE patient_id IN (SELECT patient_id FROM @sample);
    DELETE FROM dbo.checkups     WHERE patient_id IN (SELECT patient_id FROM @sample);
    DELETE FROM dbo.diet_plans   WHERE patient_id IN (SELECT patient_id FROM @sample);
    DELETE FROM dbo.appointments WHERE patient_id IN (SELECT patient_id FROM @sample);
    DELETE FROM dbo.patients     WHERE id IN (SELECT patient_id FROM @sample);
    DELETE FROM dbo.otp_codes    WHERE user_id IN (SELECT user_id FROM @sample);
    DELETE FROM dbo.audit_logs   WHERE user_id IN (SELECT user_id FROM @sample) OR action = N'SAMPLE_DATA_LOADED';
    DELETE FROM dbo.users        WHERE id IN (SELECT user_id FROM @sample);
    COMMIT TRANSACTION;
    PRINT 'Sample data removed.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    PRINT CONCAT('Clean-up stopped, nothing removed: ', ERROR_MESSAGE());
END CATCH;
DROP VIEW IF EXISTS dbo.vw_patient_summary;
DROP TABLE IF EXISTS dbo.food_items;
GO
