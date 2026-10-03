package com.nutricare.config;

import com.nutricare.repository.StaffAdminRepository;
import com.nutricare.repository.UserPatientRepository;
import com.nutricare.util.Passwords;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.SQLException;

@WebListener
public class DatabaseBootstrap implements ServletContextListener {
    private final UserPatientRepository patientRepository = new UserPatientRepository();
    private final StaffAdminRepository staffRepository = new StaffAdminRepository();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            // Creates any missing tables (and upgrades older NutriCare databases) on every start.
            DatabaseSchema.initialize();
            seed("Driberg B. D.", "appointments@nutricare.lk", "DOCTOR");
            seed("Perera K. V. R.", "admin@nutricare.lk", "ADMIN");
            seed("Nawarathne S. N.", "finance@nutricare.lk", "FINANCE");
            seed("Samarakoon P. T. A. N.", "nutritionist@nutricare.lk", "NUTRITIONIST");
            seed("Siriwardhana R. D. T. D.", "doctor@nutricare.lk", "DOCTOR");
            seed("Reception Desk", "reception@nutricare.lk", "RECEPTIONIST");
            if (patientRepository.findUserByEmail("patient@nutricare.lk").isEmpty()) {
                patientRepository.createPatient("Demo Patient", "patient@nutricare.lk", "0771234567", Passwords.hash("NutriCare123!"));
            }
            event.getServletContext().log("NutriCare database ready in " + Database.mode() + " mode");
        } catch (Exception exception) {
            event.getServletContext().log("NutriCare could not initialize the database", exception);
            throw new IllegalStateException("Database initialization failed. Check that SQL Server Express is running, "
                    + "TCP/IP is enabled on port 1433, and the NutriCare database exists (see README).", exception);
        }
    }

    private void seed(String name, String email, String role) throws SQLException {
        if (patientRepository.findUserByEmail(email).isEmpty()) {
            staffRepository.createStaff(name, email, "", Passwords.hash("NutriCare123!"), role, 0);
        }
    }
}
