-- Module 05 Payment & Billing (owner: Nawarathne S. N.) - SQL Server stored procedure.
-- Applied automatically by NutriCare on start-up after schema-sqlserver.sql. Batches are separated by GO.

-- Payment & Billing: verifies a submitted slip and writes the audit entry in one transaction.
CREATE OR ALTER PROCEDURE dbo.sp_VerifyPaymentSlip
    @PaymentID BIGINT,
    @VerifierID BIGINT,
    @Remarks NVARCHAR(500) = N'Standard Bank Slip Verification Passed'
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        IF NOT EXISTS (
            SELECT 1 FROM dbo.users
            WHERE id = @VerifierID
              AND role IN (N'FINANCE', N'ADMIN')
              AND active = 1
        )
        BEGIN
            THROW 50001, N'Authorization Error: Verifier ID is invalid, inactive, or lacks financial authority.', 1;
        END;

        IF NOT EXISTS (
            SELECT 1 FROM dbo.payments
            WHERE id = @PaymentID
              AND status = N'SUBMITTED'
        )
        BEGIN
            THROW 50002, N'State Error: Payment record does not exist or is not in SUBMITTED status.', 1;
        END;

        UPDATE dbo.payments
        SET status = N'VERIFIED',
            verified_by = @VerifierID,
            verified_at = SYSDATETIME()
        WHERE id = @PaymentID;

        INSERT INTO dbo.audit_logs (user_id, action, details, created_at)
        VALUES (
            @VerifierID,
            N'PAYMENT_VERIFIED',
            CONCAT(N'Payment ID ', @PaymentID, N' verified by Staff ID ', @VerifierID, N'. Remarks: ', @Remarks),
            SYSDATETIME()
        );

        -- Same rule as the application (design document 5.5): a verified payment confirms its pending appointment.
        UPDATE a
        SET a.status = N'CONFIRMED'
        FROM dbo.appointments a
        INNER JOIN dbo.payments p ON p.appointment_id = a.id
        WHERE p.id = @PaymentID
          AND a.status = N'PENDING';

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();

        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH;
END;
GO
