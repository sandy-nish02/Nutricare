package com.nutricare.controller;

import com.nutricare.repository.AppointmentRepository;
import com.nutricare.service.AppointmentRules;
import com.nutricare.util.Validation;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.Set;

/** Appointment booking, conflict checks, status changes, and rescheduling. */
@WebServlet(urlPatterns = {"/appointments", "/appointments/create", "/appointments/status",
        "/appointments/reschedule"})
public class AppointmentServlet extends BaseServlet {
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();

    @Override
    protected void handleGet(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        if (!"/appointments".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        requireAnyRole(request, "PATIENT", "DOCTOR", "NUTRITIONIST", "RECEPTIONIST", "ADMIN");
        request.setAttribute("appointments", appointmentRepository.appointments(userId(request), role(request)));
        request.setAttribute("staffOptions", appointmentRepository.staffForAppointments());
        if (!"PATIENT".equals(role(request))) request.setAttribute("patients", patientRepository.patients());
        request.setAttribute("slotMinutes", AppointmentRules.SLOT_MINUTES);
        render(request, response, "Appointments", "appointments");
    }

    @Override
    protected void handlePost(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/appointments/create" -> create(request, response);
            case "/appointments/status" -> updateStatus(request, response);
            case "/appointments/reschedule" -> reschedule(request, response);
            default -> throw new IllegalArgumentException("Unknown appointment action.");
        }
    }

    private void create(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "PATIENT", "RECEPTIONIST", "ADMIN");
        long selectedPatient = "PATIENT".equals(role(request))
                ? patientId(request) : Validation.id(request.getParameter("patientId"), "patient");
        LocalDateTime time = Validation.dateTime(request.getParameter("appointmentTime"), "appointment time");
        requireFutureTime(time);
        appointmentRepository.createAppointment(selectedPatient,
                Validation.id(request.getParameter("staffId"), "staff member"), time,
                Validation.required(request.getParameter("purpose"), "Purpose", 500), userId(request));
        success(request, response, "Appointment requested.", "/appointments");
    }

    private void updateStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "PATIENT", "DOCTOR", "NUTRITIONIST", "RECEPTIONIST", "ADMIN");
        long id = Validation.id(request.getParameter("id"), "appointment");
        String status = Validation.required(request.getParameter("status"), "Status", 30).toUpperCase();
        String reason = Validation.optional(request.getParameter("reason"), 500);
        if (AppointmentRules.requiresReason(status) && reason == null) {
            throw new IllegalArgumentException("Enter a reason for this appointment status.");
        }
        if ("PATIENT".equals(role(request))) {
            if (!"CANCELLED".equals(status)) {
                throw new IllegalArgumentException("Patients may only cancel appointments.");
            }
            appointmentRepository.cancelOwnAppointment(id, userId(request), reason);
        } else {
            requireChoice(status, Set.of("PENDING", "CONFIRMED", "COMPLETED", "CANCELLED", "NO_SHOW"),
                    "appointment status");
            appointmentRepository.updateAppointmentStatus(id, status, userId(request), role(request), reason);
        }
        success(request, response, "Appointment status updated.", "/appointments");
    }

    private void reschedule(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "DOCTOR", "NUTRITIONIST", "RECEPTIONIST", "ADMIN");
        LocalDateTime time = Validation.dateTime(request.getParameter("appointmentTime"), "appointment time");
        requireFutureTime(time);
        appointmentRepository.rescheduleAppointment(Validation.id(request.getParameter("id"), "appointment"),
                time, Validation.required(request.getParameter("reason"), "Reschedule reason", 500),
                userId(request), role(request));
        success(request, response, "Appointment rescheduled and returned to pending.", "/appointments");
    }

    private static void requireFutureTime(LocalDateTime time) {
        if (time.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new IllegalArgumentException("Choose an appointment time at least 30 minutes from now.");
        }
    }
}
