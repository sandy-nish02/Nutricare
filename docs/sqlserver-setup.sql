-- NutriCare - one-time SQL Server Express setup. Run in SSMS (or: sqlcmd -S localhost\SQLEXPRESS -E -i docs\sqlserver-setup.sql).
-- Only the empty database is needed: NutriCare creates and upgrades its own tables, stored procedure,
-- and trigger every time it starts (see src/main/resources/schema-sqlserver.sql).

IF DB_ID(N'NutriCare') IS NULL
    CREATE DATABASE NutriCare;
GO

-- Optional: a SQL login for running NutriCare without Windows authentication
-- (then set DB_USER=nutricare_app and DB_PASSWORD=... before run.bat). Change the password first.
-- USE NutriCare;
-- CREATE LOGIN nutricare_app WITH PASSWORD = N'Change_This_Password1', CHECK_POLICY = ON;
-- CREATE USER nutricare_app FOR LOGIN nutricare_app;
-- ALTER ROLE db_owner ADD MEMBER nutricare_app;
-- GO
