# NutriCare

NutriCare is a web-based Diet Planning & Health Check-up System built with Java 17, JSP, Jakarta Servlets, JDBC/DAO, and Microsoft SQL Server Express. Six modules share one patient record: registration/profile, appointments, check-ups, diet plans, payments, and staff administration.

Implemented workflows include:

- **Accounts:** OTP-verified registration (5-minute codes, locked after 5 wrong attempts, resend), password reset by OTP, and immediate effect of deactivation and role changes.
- **Appointments:** 30-minute slot clash checks for both staff and patients, an appointment state machine (PENDING → CONFIRMED → COMPLETED / NO_SHOW, cancel with reason), rescheduling, and 24-hour reminders.
- **Check-ups:** automatic BMI and abnormal-reading flags, check-ups linked to completed doctor appointments (FR15), and weight-change diet reviews.
- **Diet plans:** allergy blocking on creation *and* activation; a newly recorded allergy flags existing plans for review.
- **Payments:** payment-slip upload and a payment state machine (SUBMITTED → VERIFIED / REJECTED, VERIFIED → REFUNDED). Verification confirms the linked appointment, in the app and in the `sp_VerifyPaymentSlip` stored procedure.
- **Database auditing:** the app audits every change with the signed-in user and reason. The `trg_AuditAppointmentStatusChange` trigger additionally records appointment status changes made outside the app, for example in SSMS.
- **Administration:** receipts, staff role changes, password resets, patient reactivation, role-specific dashboards, and a full audit history.

## 1. One-time SQL Server Express setup

1. Install **SQL Server Express** (2019 or newer) and **SSMS**.
2. **Enable TCP/IP.** The Java driver connects over TCP.
   1. Open *SQL Server Configuration Manager*.
   2. Go to *SQL Server Network Configuration → Protocols for SQLEXPRESS → TCP/IP* and set it to **Enabled**.
   3. On the *IP Addresses* tab, in the **IPAll** section, clear *TCP Dynamic Ports* and set *TCP Port* to **1433**.
   4. Restart the *SQL Server (SQLEXPRESS)* service.
3. **Create the empty database.** Run [`docs/sqlserver-setup.sql`](docs/sqlserver-setup.sql) in SSMS, or just run `CREATE DATABASE NutriCare;`.
   Tables, the `sp_VerifyPaymentSlip` procedure, and the `trg_AuditAppointmentStatusChange` trigger are created automatically when NutriCare starts. Older NutriCare databases are upgraded in place; no data is dropped.

## 2. Run

Requirements: a Java 17+ **JDK** and internet access on the first run. Maven is used if installed; otherwise the launcher downloads a private copy into `.tools`.

**Windows:** double-click `run.bat`, then open <http://localhost:8080/nutricare/>. Stop with `Ctrl+C`.

By default NutriCare signs in to SQL Server with **your Windows account** (Windows authentication), so no password is stored in the project. To use a SQL login instead, set these variables before starting:

```bat
set DB_USER=sa
set DB_PASSWORD=your_password
run.bat
```

`DB_URL` overrides the connection string. The default is `jdbc:sqlserver://localhost:1433;databaseName=NutriCare;encrypt=false;trustServerCertificate=true`.

**macOS/Linux:** Windows authentication is unavailable, so set `DB_USER`, `DB_PASSWORD`, and (for a remote server) `DB_URL`, then run `./run.sh`.

## Demo accounts

All seeded accounts use password `NutriCare123!`.

| Role | Email |
|---|---|
| Patient | `patient@nutricare.lk` |
| Admin | `admin@nutricare.lk` |
| Doctor | `doctor@nutricare.lk` |
| Nutritionist | `nutritionist@nutricare.lk` |
| Finance | `finance@nutricare.lk` |
| Receptionist | `reception@nutricare.lk` |

**Classroom stand-ins for SMS:**
- The registration OTP is shown on the verification page.
- Password-reset codes are printed in the server console window, so the web page never reveals them.

Change demo passwords before any real deployment. This project stores health-related data and is a classroom system, not a certified medical device.

## Build and test

Run `scripts\test.bat` (Windows) or `scripts/test.sh`. Unit and repository integration tests run against an in-memory H2 database, and the deployable WAR is generated at `target/nutricare.war` for any Jakarta Servlet 6 server such as Tomcat 10.1.

To run the same integration tests against SQL Server, see [`docs/TESTING.md`](docs/TESTING.md).

## Architecture

```text
Browser/JSP → SecurityFilter → module servlet → validation / business rules → NutriCareRepository → SQL Server
```

- `src/main/java/com/nutricare/controller`: routes and role authorization, one servlet per module.
- `src/main/java/com/nutricare/repository`: one repository per module (JDBC and transactions) on a shared `BaseRepository`.
- `src/main/java/com/nutricare/service`: BMI, allergy, OTP, appointment and payment rules.
- `src/main/java/com/nutricare/filter`: authentication, session re-validation, CSRF, flash messages, and headers.
- `src/main/webapp/WEB-INF/views`: protected JSP pages.
- `src/main/resources`: SQL Server tables (`schema-sqlserver.sql`), the payment stored procedure, and the appointment trigger, all applied automatically. `schema.sql` is the H2 copy used by tests.

Further documents:
- [`docs/MODULE_OWNERSHIP.md`](docs/MODULE_OWNERSHIP.md): which member owns and commits which files.
- [`docs/sqlserver-demo-queries.sql`](docs/sqlserver-demo-queries.sql): SSMS queries for checking every module's data, plus trigger, procedure, and constraint demos.
- [`docs/sql-viva-demo.sql`](docs/sql-viva-demo.sql): viva SQL demonstration. It loads sample data, then demonstrates CREATE TABLE, INSERT, SELECT, UPDATE … SET, ALTER, DELETE, INNER/LEFT JOIN, GROUP BY, views, transactions, the procedure and trigger, and constraints, and cleans up afterwards.
- [`docs/TESTING.md`](docs/TESTING.md): automated and manual testing.

## Member ownership

| Member | ID | Module |
|---|---|---|
| Driberg B. D. | IT25103744 | Appointment Management |
| Hettiarachchi H. H. K. S. | IT25103745 | User & Patient Management |
| Perera K. V. R. | IT25103746 | User & Staff Management — Admin |
| Nawarathne S. N. (Team Leader) | IT25103747 | Payment & Billing Management |
| Samarakoon P. T. A. N. | IT25103748 | Diet Plan Management |
| Siriwardhana R. D. T. D. | IT25103749 | Health Check-up Management |

The six module-specific controller and view areas follow the ownership table above. Team rehearsal material is distributed separately from the source submission.

## Troubleshooting

- **"Database initialization failed" / connection refused:**
  - Check that the *SQL Server (SQLEXPRESS)* service is running.
  - Check that TCP/IP is enabled on port 1433 (step 1.2) and the service was restarted afterwards.
- **"This driver is not configured for integrated authentication":** start with `run.bat`, which points Java at `sqljdbc_13.4\enu\auth\x64`. Alternatively, use a SQL login with `DB_USER` and `DB_PASSWORD`.
- **"Cannot open database NutriCare":** create it (step 1.3).
- **Login failed for your Windows user:** in SSMS, add your Windows login under *Security → Logins* and give it `db_owner` on NutriCare. Or use a SQL login.
- **Java not found:** install a Java 17+ JDK from Eclipse Temurin/Adoptium, restart the terminal, and run again.
- **Port 8080 busy:** stop the process using it, or run `mvn jetty:run -Djetty.http.port=8081` and use port 8081.
