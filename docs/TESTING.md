# Testing checklist

## Automated

Run `scripts\test.bat` on Windows or `scripts/test.sh` on macOS/Linux. This runs 44 tests against an in-memory H2 database, then builds `target/nutricare.war`. Two tests that need SQL Server-only objects are skipped on H2.

| Module | Unit tests (pure rules) | Integration tests (repository + database) |
|---|---|---|
| User & Patient | `OtpServiceTest` | `UserPatientRepositoryTest` |
| Diet Plans | `DietSafetyTest` | `DietPlanRepositoryTest` |
| Health Check-ups | `HealthRulesTest` | `HealthCheckupRepositoryTest` |
| Appointments | `AppointmentRulesTest` | `AppointmentRepositoryTest` (includes the trigger test) |
| Payments | `PaymentRulesTest` | `PaymentRepositoryTest` (includes the stored-procedure test) |
| Staff Admin | — | `StaffAdminRepositoryTest` |
| Shared | `ValidationTest` | `DashboardRepositoryTest` |

To run one module's tests only:

```bat
.tools\apache-maven-3.9.9\bin\mvn.cmd test -Dtest=AppointmentRulesTest,AppointmentRepositoryTest
```

### Running the integration tests on SQL Server

The same repository tests can run against a real SQL Server database. This checks every query in SQL Server's dialect, together with the trigger and the stored procedure.

1. Create an empty test database. Never point the tests at the live `NutriCare` database.

   ```bat
   sqlcmd -S localhost\SQLEXPRESS -E -Q "CREATE DATABASE NutriCare_Test"
   ```

2. Run the tests with Windows authentication:

   ```bat
   .tools\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=com.nutricare.repository.*Test" "-Dit.sqlserver.url=jdbc:sqlserver://localhost:1433;databaseName=NutriCare_Test;encrypt=false;trustServerCertificate=true;integratedSecurity=true" "-DargLine=-Djava.library.path=sqljdbc_13.4\enu\auth\x64"
   ```

Each run uses unique emails and references, so the suite can be repeated against the same database. Expected result: 28 tests, 0 failures, 0 skipped.

## Checking the database

Open [`sqlserver-demo-queries.sql`](sqlserver-demo-queries.sql) in SSMS. Connect to `localhost\SQLEXPRESS` with Windows authentication, select a section, and press F5. Sections 2–7 show each module's data. Sections 8–10 demonstrate the trigger, the stored procedure, and the constraints inside rolled-back transactions.

For the viva SQL demonstration, open [`sql-viva-demo.sql`](sql-viva-demo.sql) the same way. Run PART 0 once to load sample patients, appointments, check-ups, diet plans, and payments. Then run PARTS 1–6 one at a time, and PART 7 afterwards to remove the sample data again.

When using `sqlcmd` instead of SSMS, add `-I`. Without it `sqlcmd` turns `QUOTED_IDENTIFIER` off, and writes to the `checkups` table fail because of its filtered unique index.

## Manual role walkthrough

1. **Patient:**
   - Register, verify the on-screen code, then sign in.
   - Update allergies and measurements.
   - Book a future appointment.
   - Submit a payment linked to that appointment.
2. **Finance:** verify the payment, then confirm the appointment changes to CONFIRMED. Submit the same reference again and confirm it is rejected as a duplicate.
3. **Doctor:**
   - Mark the appointment COMPLETED.
   - Use **Record check-up** on that row and confirm the BMI appears.
   - Confirm the button is replaced by "Check-up recorded".
4. **Nutritionist:**
   - Create a safe diet plan.
   - Try an allergy word to confirm it is blocked.
   - Activate the safe plan.
5. **Patient:** add a new allergy that appears in the active plan. Confirm the plan shows REVIEW NEEDED and cannot be re-activated.
6. **Doctor:** record a 5% weight change and confirm the active diet plan shows REVIEW NEEDED.
7. **Patient:** cancel a confirmed appointment with a reason. **Finance** can then refund its verified payment.
8. **Admin:**
   - Create and deactivate a staff account; confirm that person is signed out on their next click.
   - Restore a self-deactivated patient.
   - Inspect the audit page.
9. **Forgot password:** request a code for `patient@nutricare.lk`, read it from the server console, and reset the password.

## Deployment smoke check

- `GET /nutricare/health` must return `{"status":"UP","application":"NutriCare"}`.
- `GET /nutricare/` shows the public homepage; `/nutricare/dashboard` redirects to sign-in when signed out.
- Log in with `patient@nutricare.lk` / `NutriCare123!`.
- Confirm a patient cannot open an admin URL (403), and staff cannot change another module through a POST request.
