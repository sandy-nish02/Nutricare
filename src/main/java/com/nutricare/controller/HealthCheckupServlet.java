package com.nutricare.controller;

import com.nutricare.repository.HealthCheckupRepository;
import com.nutricare.repository.HealthCheckupRepository.CheckupOutcome;
import com.nutricare.service.HealthRules;
import com.nutricare.util.Validation;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.Optional;

/** Clinical measurements, BMI calculation, abnormal-reading flags, and appointment-linked check-ups. */
@WebServlet(urlPatterns = {"/checkups", "/checkups/create"})
public class HealthCheckupServlet extends BaseServlet {
    private final HealthCheckupRepository checkupRepository = new HealthCheckupRepository();

    @Override
    protected void handleGet(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        if (!"/checkups".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        requireAnyRole(request, "PATIENT", "DOCTOR", "NUTRITIONIST", "ADMIN");
        request.setAttribute("checkups", checkupRepository.checkups(userId(request), role(request)));
        if ("DOCTOR".equals(role(request)) || "ADMIN".equals(role(request))) {
            request.setAttribute("patients", patientRepository.patients());
            String appointment = request.getParameter("appointmentId");
            if (appointment != null && !appointment.isBlank()) {
                // FR15: "Record check-up" from a completed appointment pre-selects that visit.
                Optional<Map<String, Object>> linked = checkupRepository.appointmentAwaitingCheckup(
                        Validation.id(appointment, "appointment"), userId(request), role(request));
                if (linked.isPresent()) {
                    request.setAttribute("linkedAppointment", linked.get());
                } else {
                    request.setAttribute("flashError", "That appointment is not a completed doctor visit awaiting a check-up.");
                }
            }
        }
        render(request, response, "Health check-ups", "checkups");
    }

    @Override
    protected void handlePost(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        if (!"/checkups/create".equals(path)) {
            throw new IllegalArgumentException("Unknown check-up action.");
        }
        requireAnyRole(request, "DOCTOR", "ADMIN");
        String appointment = request.getParameter("appointmentId");
        Long appointmentId = appointment == null || appointment.isBlank() ? null : Validation.id(appointment, "appointment");
        long patientId = appointmentId == null ? Validation.id(request.getParameter("patientId"), "patient") : 0;
        double height = Validation.decimal(request.getParameter("height"), "Height", 50, 260);
        double weight = Validation.decimal(request.getParameter("weight"), "Weight", 10, 500);
        Integer systolic = optionalInt(request.getParameter("systolic"), "Systolic pressure", 50, 260);
        Integer diastolic = optionalInt(request.getParameter("diastolic"), "Diastolic pressure", 30, 180);
        Double sugar = optionalDouble(request.getParameter("bloodSugar"), "Blood sugar", 20, 600);
        CheckupOutcome outcome = checkupRepository.createCheckup(patientId, appointmentId, userId(request), role(request),
                height, weight, systolic, diastolic, sugar, Validation.optional(request.getParameter("notes"), 1200));
        StringBuilder message = new StringBuilder("Check-up saved. BMI ").append(outcome.bmi())
                .append(" (").append(HealthRules.bmiLabel(outcome.bmi())).append(").");
        if (outcome.abnormal()) message.append(" Abnormal reading flagged for clinical review.");
        if (outcome.dietReviewFlagged()) message.append(" Weight changed by 5% or more, so the active diet plan was flagged for review.");
        success(request, response, message.toString(), "/checkups");
    }
}
