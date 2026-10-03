-- NutriCare tables for Microsoft SQL Server (Express 2019+). The stored procedure and trigger live in
-- sqlserver-procedure-payments.sql and sqlserver-trigger-appointments.sql.
-- Run automatically by the application on start-up. Every batch is idempotent: missing tables
-- are created, columns added in later releases are added to existing databases, and nothing is
-- dropped. Batches are separated by GO lines, exactly as in SSMS.

-- Required for the filtered unique index below (and the SSMS/JDBC default anyway).
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
CREATE TABLE dbo.users (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_users PRIMARY KEY,
    full_name     NVARCHAR(120) NOT NULL,
    email         NVARCHAR(160) NOT NULL CONSTRAINT UQ_users_email UNIQUE,
    phone         NVARCHAR(30)  NULL,
    password_hash NVARCHAR(100) NOT NULL,
    role          NVARCHAR(30)  NOT NULL,
    active        BIT           NOT NULL CONSTRAINT DF_users_active DEFAULT 1,
    verified      BIT           NOT NULL CONSTRAINT DF_users_verified DEFAULT 1,
    created_at    DATETIME2(6)  NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSDATETIME()
);
GO
IF COL_LENGTH(N'dbo.users', N'verified') IS NULL
    ALTER TABLE dbo.users ADD verified BIT NOT NULL CONSTRAINT DF_users_verified DEFAULT 1;
GO

IF OBJECT_ID(N'dbo.patients', N'U') IS NULL
CREATE TABLE dbo.patients (
    id                BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_patients PRIMARY KEY,
    user_id           BIGINT        NOT NULL CONSTRAINT UQ_patients_user UNIQUE
                                    CONSTRAINT FK_patients_user REFERENCES dbo.users(id),
    date_of_birth     DATE          NULL,
    gender            NVARCHAR(20)  NULL,
    height_cm         DECIMAL(6,2)  NULL,
    weight_kg         DECIMAL(6,2)  NULL,
    allergies         NVARCHAR(500) NULL,
    conditions        NVARCHAR(800) NULL,
    medications       NVARCHAR(800) NULL,
    emergency_contact NVARCHAR(120) NULL
);
GO

IF OBJECT_ID(N'dbo.appointments', N'U') IS NULL
CREATE TABLE dbo.appointments (
    id               BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_appointments PRIMARY KEY,
    patient_id       BIGINT        NOT NULL CONSTRAINT FK_appointments_patient REFERENCES dbo.patients(id),
    staff_id         BIGINT        NOT NULL CONSTRAINT FK_appointments_staff REFERENCES dbo.users(id),
    appointment_time DATETIME2(6)  NOT NULL,
    purpose          NVARCHAR(500) NOT NULL,
    status           NVARCHAR(30)  NOT NULL CONSTRAINT DF_appointments_status DEFAULT N'PENDING',
    change_reason    NVARCHAR(500) NULL,
    created_at       DATETIME2(6)  NOT NULL CONSTRAINT DF_appointments_created_at DEFAULT SYSDATETIME()
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_appointments_staff_time')
    CREATE INDEX IX_appointments_staff_time ON dbo.appointments (staff_id, appointment_time);
GO

IF OBJECT_ID(N'dbo.checkups', N'U') IS NULL
CREATE TABLE dbo.checkups (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_checkups PRIMARY KEY,
    patient_id     BIGINT         NOT NULL CONSTRAINT FK_checkups_patient REFERENCES dbo.patients(id),
    doctor_id      BIGINT         NOT NULL CONSTRAINT FK_checkups_doctor REFERENCES dbo.users(id),
    appointment_id BIGINT         NULL CONSTRAINT FK_checkups_appointment REFERENCES dbo.appointments(id),
    checked_at     DATETIME2(6)   NOT NULL CONSTRAINT DF_checkups_checked_at DEFAULT SYSDATETIME(),
    height_cm      DECIMAL(6,2)   NOT NULL,
    weight_kg      DECIMAL(6,2)   NOT NULL,
    bmi            DECIMAL(5,2)   NOT NULL,
    systolic       INT            NULL,
    diastolic      INT            NULL,
    blood_sugar    DECIMAL(6,2)   NULL,
    notes          NVARCHAR(1200) NULL,
    abnormal       BIT            NOT NULL CONSTRAINT DF_checkups_abnormal DEFAULT 0
);
GO
IF COL_LENGTH(N'dbo.checkups', N'appointment_id') IS NULL
    ALTER TABLE dbo.checkups ADD appointment_id BIGINT NULL
        CONSTRAINT FK_checkups_appointment REFERENCES dbo.appointments(id);
GO
-- FR15: one check-up record per completed appointment.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_checkups_appointment')
    CREATE UNIQUE INDEX UX_checkups_appointment ON dbo.checkups (appointment_id) WHERE appointment_id IS NOT NULL;
GO

IF OBJECT_ID(N'dbo.diet_plans', N'U') IS NULL
CREATE TABLE dbo.diet_plans (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_diet_plans PRIMARY KEY,
    patient_id      BIGINT         NOT NULL CONSTRAINT FK_diet_plans_patient REFERENCES dbo.patients(id),
    nutritionist_id BIGINT         NOT NULL CONSTRAINT FK_diet_plans_nutritionist REFERENCES dbo.users(id),
    title           NVARCHAR(160)  NOT NULL,
    daily_calories  INT            NOT NULL,
    meals           NVARCHAR(3000) NOT NULL,
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL,
    status          NVARCHAR(30)   NOT NULL CONSTRAINT DF_diet_plans_status DEFAULT N'DRAFT',
    review_needed   BIT            NOT NULL CONSTRAINT DF_diet_plans_review DEFAULT 0,
    created_at      DATETIME2(6)   NOT NULL CONSTRAINT DF_diet_plans_created_at DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID(N'dbo.payments', N'U') IS NULL
CREATE TABLE dbo.payments (
    id                BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_payments PRIMARY KEY,
    patient_id        BIGINT         NOT NULL CONSTRAINT FK_payments_patient REFERENCES dbo.patients(id),
    appointment_id    BIGINT         NULL CONSTRAINT FK_payments_appointment REFERENCES dbo.appointments(id),
    amount            DECIMAL(10,2)  NOT NULL,
    transaction_ref   NVARCHAR(100)  NOT NULL CONSTRAINT UQ_payments_reference UNIQUE,
    method            NVARCHAR(40)   NOT NULL,
    payment_slip_name NVARCHAR(255)  NULL,
    payment_slip_type NVARCHAR(100)  NULL,
    payment_slip_data VARBINARY(MAX) NULL,
    status            NVARCHAR(30)   NOT NULL CONSTRAINT DF_payments_status DEFAULT N'SUBMITTED',
    submitted_at      DATETIME2(6)   NOT NULL CONSTRAINT DF_payments_submitted_at DEFAULT SYSDATETIME(),
    verified_by       BIGINT         NULL CONSTRAINT FK_payments_verified_by REFERENCES dbo.users(id),
    verified_at       DATETIME2(6)   NULL
);
GO

IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NULL
CREATE TABLE dbo.audit_logs (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_audit_logs PRIMARY KEY,
    user_id    BIGINT         NULL CONSTRAINT FK_audit_logs_user REFERENCES dbo.users(id),
    action     NVARCHAR(120)  NOT NULL,
    details    NVARCHAR(1000) NULL,
    created_at DATETIME2(6)   NOT NULL CONSTRAINT DF_audit_logs_created_at DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID(N'dbo.otp_codes', N'U') IS NULL
CREATE TABLE dbo.otp_codes (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_otp_codes PRIMARY KEY,
    user_id    BIGINT        NOT NULL CONSTRAINT FK_otp_codes_user REFERENCES dbo.users(id),
    purpose    NVARCHAR(20)  NOT NULL CONSTRAINT DF_otp_codes_purpose DEFAULT N'VERIFY',
    code_hash  NVARCHAR(64)  NOT NULL,
    expires_at DATETIME2(6)  NOT NULL,
    attempts   INT           NOT NULL CONSTRAINT DF_otp_codes_attempts DEFAULT 0,
    used       BIT           NOT NULL CONSTRAINT DF_otp_codes_used DEFAULT 0,
    created_at DATETIME2(6)  NOT NULL CONSTRAINT DF_otp_codes_created_at DEFAULT SYSDATETIME()
);
GO
IF COL_LENGTH(N'dbo.otp_codes', N'purpose') IS NULL
    ALTER TABLE dbo.otp_codes ADD purpose NVARCHAR(20) NOT NULL CONSTRAINT DF_otp_codes_purpose DEFAULT N'VERIFY';
GO
IF COL_LENGTH(N'dbo.otp_codes', N'attempts') IS NULL
    ALTER TABLE dbo.otp_codes ADD attempts INT NOT NULL CONSTRAINT DF_otp_codes_attempts DEFAULT 0;
GO
