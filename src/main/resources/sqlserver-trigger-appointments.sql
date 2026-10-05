-- Module 04 Appointment Management (owner: Driberg B. D.) - SQL Server trigger.
-- Applied automatically by NutriCare on start-up after schema-sqlserver.sql. Batches are separated by GO.

-- Appointment Management: database-level safety net for appointment status changes.
-- NutriCare writes its own audit entry (with the signed-in user and the reason) for every change it makes,
-- and tags its sessions with SESSION_CONTEXT 'nutricare_app'. This trigger therefore only records status
-- changes made OUTSIDE the application (for example directly in SSMS or by sp_VerifyPaymentSlip run by hand),
-- naming the SQL login responsible. Earlier versions logged every change a second time under the assigned
-- doctor's ID, which double-counted changes and blamed the wrong person.
CREATE OR ALTER TRIGGER dbo.trg_AuditAppointmentStatusChange
ON dbo.appointments
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(status) AND SESSION_CONTEXT(N'nutricare_app') IS NULL
    BEGIN
        INSERT INTO dbo.audit_logs (user_id, action, details, created_at)
        SELECT
            NULL AS user_id,
            N'APPOINTMENT_STATUS_CHANGED' AS action,
            CONCAT(
                N'Appointment ID ', i.id,
                N' (Patient ID: ', i.patient_id,
                N') transitioned from [', d.status,
                N'] to [', i.status,
                N'] outside NutriCare by SQL login ', ORIGINAL_LOGIN(),
                N'. Purpose: ', i.purpose,
                CASE WHEN i.change_reason IS NOT NULL
                     THEN CONCAT(N'. Reason: ', i.change_reason)
                     ELSE N''
                END
            ) AS details,
            SYSDATETIME() AS created_at
        FROM inserted i
        INNER JOIN deleted d ON i.id = d.id
        WHERE i.status <> d.status;
    END;
END;
GO
