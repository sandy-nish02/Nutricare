package com.nutricare.controller;

import com.nutricare.repository.StaffAdminRepository;
import com.nutricare.util.Passwords;
import com.nutricare.util.Validation;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Set;

/** Staff accounts, role assignments, account status (staff and patients), and the audit trail. */
@WebServlet(urlPatterns = {"/staff", "/staff/create", "/staff/status", "/staff/access", "/staff/patient-status", "/audit"})
public class StaffAdminServlet extends BaseServlet {
    private static final Set<String> STAFF_ROLES =
            Set.of("ADMIN", "DOCTOR", "NUTRITIONIST", "RECEPTIONIST", "FINANCE");
    private final StaffAdminRepository staffRepository = new StaffAdminRepository();

    @Override
    protected void handleGet(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/staff" -> staff(request, response);
            case "/audit" -> audit(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void handlePost(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/staff/create" -> create(request, response);
            case "/staff/status" -> updateStatus(request, response);
            case "/staff/access" -> updateAccess(request, response);
            case "/staff/patient-status" -> updatePatientStatus(request, response);
            default -> throw new IllegalArgumentException("Unknown staff action.");
        }
    }

    private void staff(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireRole(request, "ADMIN");
        request.setAttribute("staff", staffRepository.staff());
        request.setAttribute("patientAccounts", staffRepository.patientAccounts());
        render(request, response, "Staff administration", "staff");
    }

    private void create(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireRole(request, "ADMIN");
        String selectedRole = Validation.required(request.getParameter("role"), "Role", 30).toUpperCase();
        requireChoice(selectedRole, STAFF_ROLES, "role");
        String password = Validation.password(request.getParameter("password"), "Temporary password");
        staffRepository.createStaff(
                Validation.required(request.getParameter("fullName"), "Full name", 120),
                Validation.email(request.getParameter("email")),
                Validation.phone(request.getParameter("phone"), false),
                Passwords.hash(password), selectedRole, userId(request));
        success(request, response, "Staff account created.", "/staff");
    }

    private void updateStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireRole(request, "ADMIN");
        staffRepository.setUserActive(Validation.id(request.getParameter("id"), "staff account"),
                "true".equalsIgnoreCase(request.getParameter("active")), userId(request));
        success(request, response, "Staff account status updated.", "/staff");
    }

    private void updatePatientStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireRole(request, "ADMIN");
        boolean active = "true".equalsIgnoreCase(request.getParameter("active"));
        staffRepository.setPatientActive(Validation.id(request.getParameter("id"), "patient account"), active, userId(request));
        success(request, response, active ? "Patient account restored." : "Patient account suspended.", "/staff");
    }

    private void updateAccess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireRole(request, "ADMIN");
        String selectedRole = Validation.required(request.getParameter("role"), "Role", 30).toUpperCase();
        requireChoice(selectedRole, STAFF_ROLES, "role");
        String password = request.getParameter("password");
        String hash = password == null || password.isBlank()
                ? null : Passwords.hash(Validation.password(password, "New password"));
        staffRepository.updateStaffAccess(Validation.id(request.getParameter("id"), "staff account"),
                selectedRole, hash, userId(request));
        success(request, response, "Staff role and credentials updated.", "/staff");
    }

    private void audit(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireRole(request, "ADMIN");
        request.setAttribute("logs", staffRepository.auditLogs());
        render(request, response, "Audit trail", "audit");
    }
}
