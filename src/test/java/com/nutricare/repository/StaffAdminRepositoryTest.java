package com.nutricare.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Module 06 - User & Staff Management (Admin). */
class StaffAdminRepositoryTest extends IntegrationTestBase {

    @Test
    void administratorCanRestoreVerifiedPatientOnly() throws Exception {
        long admin = newStaff("Restore Admin", "ADMIN");
        long patientUser = newPatient("Restore Patient");
        patients.deactivatePatient(patientUser);
        staff.setPatientActive(patientUser, true, admin);
        assertEquals(true, patients.findUser(patientUser).orElseThrow().get("active"));

        long unverified = patients.createPatient("Unverified Patient", email("unverified"), "0770000005", PASSWORD, false);
        assertThrows(IllegalArgumentException.class, () -> staff.setPatientActive(unverified, true, admin));
    }

    @Test
    void adminCannotLockThemselvesOut() throws Exception {
        long admin = newStaff("Careful Admin", "ADMIN");
        assertThrows(IllegalArgumentException.class, () -> staff.setUserActive(admin, false, admin));
        assertThrows(IllegalArgumentException.class, () -> staff.updateStaffAccess(admin, "DOCTOR", null, admin));
    }

    @Test
    void roleChangesAndDeactivationAreAudited() throws Exception {
        long admin = newStaff("Audit Admin", "ADMIN");
        long reception = newStaff("Audit Reception", "RECEPTIONIST");
        staff.updateStaffAccess(reception, "FINANCE", null, admin);
        staff.setUserActive(reception, false, admin);
        assertEquals("FINANCE", patients.findUser(reception).orElseThrow().get("role"));
        assertEquals(false, patients.findUser(reception).orElseThrow().get("active"));
        assertTrue(staff.auditLogs().stream().anyMatch(log ->
                "STAFF_STATUS".equals(log.get("action")) && String.valueOf(log.get("details")).contains("#" + reception)));
    }

    @Test
    void patientsCannotBeChangedThroughStaffActions() throws Exception {
        long admin = newStaff("Boundary Admin", "ADMIN");
        long patientUser = newPatient("Boundary Patient");
        assertThrows(IllegalArgumentException.class, () -> staff.updateStaffAccess(patientUser, "ADMIN", null, admin));
        assertThrows(IllegalArgumentException.class, () -> staff.setUserActive(patientUser, false, admin));
    }
}
