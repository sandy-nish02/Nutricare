# Module ownership and Git commit plan

Each member commits the files they own from their own GitHub account. The **shared foundation** is committed first by the team leader. The other six modules can then be pushed in any order on feature branches and merged by the leader. The project compiles once the foundation and all six modules are on `main`.

| # | Module | Owner | Branch |
|---|---|---|---|
| 0 | Shared foundation | Nawarathne S. N. (Team Leader) | `main` (first commit) |
| 1 | User & Patient Management | Hettiarachchi H. H. K. S. (IT25103745) | `feature/user-patient` |
| 2 | Diet Plan Management | Samarakoon P. T. A. N. (IT25103748) | `feature/diet-plans` |
| 3 | Health Check-up Management | Siriwardhana R. D. T. D. (IT25103749) | `feature/health-checkups` |
| 4 | Appointment Management | Driberg B. D. (IT25103744) | `feature/appointments` |
| 5 | Payment & Billing Management | Nawarathne S. N. (IT25103747) | `feature/payments` |
| 6 | User & Staff Management (Admin) | Perera K. V. R. (IT25103746) | `feature/staff-admin` |

## 0. Shared foundation (Team Leader)

```
.gitignore
.gitattributes
README.md
pom.xml
run.bat
run.sh
docs/MODULE_OWNERSHIP.md
docs/TESTING.md
docs/sqlserver-setup.sql
docs/sqlserver-demo-queries.sql
docs/sql-viva-demo.sql
scripts/CoreRuleSmoke.java
scripts/test.bat
scripts/test.sh
sqljdbc_13.4/enu/license.txt
sqljdbc_13.4/enu/redist.txt
sqljdbc_13.4/enu/auth/x64/mssql-jdbc_auth-13.4.0.x64.dll
src/main/java/com/nutricare/config/Database.java
src/main/java/com/nutricare/config/DatabaseBootstrap.java
src/main/java/com/nutricare/config/DatabaseSchema.java
src/main/java/com/nutricare/controller/AppServlet.java
src/main/java/com/nutricare/controller/BaseServlet.java
src/main/java/com/nutricare/repository/BaseRepository.java
src/main/java/com/nutricare/repository/DashboardRepository.java
src/main/java/com/nutricare/util/Validation.java
src/main/resources/schema-sqlserver.sql
src/main/resources/schema.sql
src/main/webapp/WEB-INF/web.xml
src/main/webapp/WEB-INF/views/includes/header.jsp
src/main/webapp/WEB-INF/views/includes/footer.jsp
src/main/webapp/WEB-INF/views/home.jsp
src/main/webapp/WEB-INF/views/dashboard.jsp
src/main/webapp/WEB-INF/views/error.jsp
src/main/webapp/assets/app.css
src/main/webapp/assets/app.js
src/test/java/com/nutricare/repository/IntegrationTestBase.java
src/test/java/com/nutricare/repository/DashboardRepositoryTest.java
src/test/java/com/nutricare/util/ValidationTest.java
```

## 1. User & Patient Management (Hettiarachchi H. H. K. S.)

```
src/main/java/com/nutricare/controller/UserPatientServlet.java
src/main/java/com/nutricare/repository/UserPatientRepository.java
src/main/java/com/nutricare/service/OtpService.java
src/main/java/com/nutricare/util/Passwords.java
src/main/webapp/WEB-INF/views/login.jsp
src/main/webapp/WEB-INF/views/register.jsp
src/main/webapp/WEB-INF/views/verify.jsp
src/main/webapp/WEB-INF/views/forgot-password.jsp
src/main/webapp/WEB-INF/views/reset-password.jsp
src/main/webapp/WEB-INF/views/profile.jsp
src/test/java/com/nutricare/repository/UserPatientRepositoryTest.java
src/test/java/com/nutricare/service/OtpServiceTest.java
```

## 2. Diet Plan Management (Samarakoon P. T. A. N.)

```
src/main/java/com/nutricare/controller/DietPlanServlet.java
src/main/java/com/nutricare/repository/DietPlanRepository.java
src/main/java/com/nutricare/service/DietSafety.java
src/main/webapp/WEB-INF/views/diets.jsp
src/test/java/com/nutricare/repository/DietPlanRepositoryTest.java
src/test/java/com/nutricare/service/DietSafetyTest.java
```

## 3. Health Check-up Management (Siriwardhana R. D. T. D.)

```
src/main/java/com/nutricare/controller/HealthCheckupServlet.java
src/main/java/com/nutricare/repository/HealthCheckupRepository.java
src/main/java/com/nutricare/service/HealthRules.java
src/main/webapp/WEB-INF/views/checkups.jsp
src/test/java/com/nutricare/repository/HealthCheckupRepositoryTest.java
src/test/java/com/nutricare/service/HealthRulesTest.java
```

## 4. Appointment Management (Driberg B. D.)

```
src/main/java/com/nutricare/controller/AppointmentServlet.java
src/main/java/com/nutricare/repository/AppointmentRepository.java
src/main/java/com/nutricare/service/AppointmentRules.java
src/main/resources/sqlserver-trigger-appointments.sql
src/main/webapp/WEB-INF/views/appointments.jsp
src/test/java/com/nutricare/repository/AppointmentRepositoryTest.java
src/test/java/com/nutricare/service/AppointmentRulesTest.java
```

## 5. Payment & Billing Management (Nawarathne S. N.)

```
src/main/java/com/nutricare/controller/PaymentServlet.java
src/main/java/com/nutricare/repository/PaymentRepository.java
src/main/java/com/nutricare/service/PaymentRules.java
src/main/resources/sqlserver-procedure-payments.sql
src/main/webapp/WEB-INF/views/payments.jsp
src/main/webapp/WEB-INF/views/receipt.jsp
src/test/java/com/nutricare/repository/PaymentRepositoryTest.java
src/test/java/com/nutricare/service/PaymentRulesTest.java
```

## 6. User & Staff Management — Admin (Perera K. V. R.)

```
src/main/java/com/nutricare/controller/StaffAdminServlet.java
src/main/java/com/nutricare/repository/StaffAdminRepository.java
src/main/java/com/nutricare/filter/SecurityFilter.java
src/main/webapp/WEB-INF/views/staff.jsp
src/main/webapp/WEB-INF/views/audit.jsp
src/test/java/com/nutricare/repository/StaffAdminRepositoryTest.java
```
