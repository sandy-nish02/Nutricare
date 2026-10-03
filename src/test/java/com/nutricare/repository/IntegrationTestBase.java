package com.nutricare.repository;

import com.nutricare.config.Database;
import com.nutricare.config.DatabaseSchema;
import com.nutricare.util.Passwords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Shared set-up for the repository integration tests. Runs against an in-memory H2 database by default.
 * To run the same tests against SQL Server, create an empty database and pass its JDBC URL, for example:
 * {@code mvn test -Dit.sqlserver.url="jdbc:sqlserver://localhost:1433;databaseName=NutriCare_Test;encrypt=false;trustServerCertificate=true;integratedSecurity=true"}
 */
abstract class IntegrationTestBase {
    /** Makes emails and references unique, so the tests can be re-run against a persistent database. */
    static final String RUN = Long.toString(System.nanoTime(), 36);
    static final String PASSWORD = Passwords.hash("Password1");

    final UserPatientRepository patients = new UserPatientRepository();
    final StaffAdminRepository staff = new StaffAdminRepository();
    final AppointmentRepository appointments = new AppointmentRepository();
    final HealthCheckupRepository checkups = new HealthCheckupRepository();
    final DietPlanRepository diets = new DietPlanRepository();
    final PaymentRepository payments = new PaymentRepository();
    final DashboardRepository dashboard = new DashboardRepository();

    @BeforeEach
    void setUpDatabase() throws Exception {
        String sqlServerUrl = System.getProperty("it.sqlserver.url");
        if (sqlServerUrl != null && !sqlServerUrl.isBlank()) {
            System.setProperty("db.mode", "sqlserver");
            System.setProperty("db.url", sqlServerUrl);
            System.setProperty("db.password", "");
        } else {
            System.setProperty("db.mode", "h2");
            System.setProperty("db.url", "jdbc:h2:mem:nutricare_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
            System.setProperty("db.user", "sa");
            System.setProperty("db.password", "");
        }
        DatabaseSchema.initialize();
    }

    @AfterEach
    void clearDatabaseSettings() {
        System.clearProperty("db.mode");
        System.clearProperty("db.url");
        System.clearProperty("db.user");
        System.clearProperty("db.password");
    }

    static boolean onSqlServer() {
        return Database.isSqlServer();
    }

    static String email(String name) {
        return name + "." + RUN + "@test.lk";
    }

    long newPatient(String name) throws Exception {
        return patients.createPatient(name, email(name.toLowerCase().replace(' ', '.')), "0770000000", PASSWORD);
    }

    long newStaff(String name, String role) throws Exception {
        return staff.createStaff(name, email(name.toLowerCase().replace(' ', '.')), null, PASSWORD, role, 0);
    }

    long patientId(long patientUser) throws Exception {
        return ((Number) patients.patientByUser(patientUser).orElseThrow().get("id")).longValue();
    }

    /** Books the patient with the clinician the given number of hours from now and returns the appointment ID. */
    long book(long patientUser, long staffId, int hoursAhead) throws Exception {
        LocalDateTime time = LocalDateTime.now().plusHours(hoursAhead).withSecond(0).withNano(0);
        appointments.createAppointment(patientId(patientUser), staffId, time, "Visit", patientUser);
        return ((Number) appointments.appointments(patientUser, "PATIENT").get(0).get("id")).longValue();
    }

    String appointmentStatus(long patientUser) throws Exception {
        return String.valueOf(appointments.appointments(patientUser, "PATIENT").get(0).get("status"));
    }

    long paymentId(long patientUser) throws Exception {
        return ((Number) payments.payments(patientUser, "PATIENT").get(0).get("id")).longValue();
    }

    static long planId(List<Map<String, Object>> plans, String title) {
        return ((Number) plans.stream().filter(plan -> title.equals(plan.get("title")))
                .findFirst().orElseThrow().get("id")).longValue();
    }
}
