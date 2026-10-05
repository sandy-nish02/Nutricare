package com.nutricare.controller;

import com.nutricare.repository.DietPlanRepository;
import com.nutricare.util.Validation;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.util.Set;

/** Diet-plan creation, allergy validation, approval, and archiving. */
@WebServlet(urlPatterns = {"/diets", "/diets/create", "/diets/status"})
public class DietPlanServlet extends BaseServlet {
    private final DietPlanRepository dietRepository = new DietPlanRepository();

    @Override
    protected void handleGet(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        if (!"/diets".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        requireAnyRole(request, "PATIENT", "NUTRITIONIST", "ADMIN");
        request.setAttribute("diets", dietRepository.dietPlans(userId(request), role(request)));
        if (!"PATIENT".equals(role(request))) request.setAttribute("patients", patientRepository.patients());
        render(request, response, "Diet plans", "diets");
    }

    @Override
    protected void handlePost(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/diets/create" -> create(request, response);
            case "/diets/status" -> updateStatus(request, response);
            default -> throw new IllegalArgumentException("Unknown diet-plan action.");
        }
    }

    private void create(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "NUTRITIONIST", "ADMIN");
        long selectedPatient = Validation.id(request.getParameter("patientId"), "patient");
        String meals = Validation.required(request.getParameter("meals"), "Meal plan", 3000);
        // The repository blocks meals that contain the patient's recorded allergies.
        LocalDate start = Validation.date(request.getParameter("startDate"), "start date");
        LocalDate end = Validation.date(request.getParameter("endDate"), "end date");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be on or after the start date.");
        }
        dietRepository.createDietPlan(selectedPatient, userId(request),
                Validation.required(request.getParameter("title"), "Title", 160),
                Validation.integer(request.getParameter("calories"), "Daily calories", 500, 10000),
                meals, start, end);
        success(request, response, "Draft diet plan created.", "/diets");
    }

    private void updateStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "NUTRITIONIST", "ADMIN");
        String status = Validation.required(request.getParameter("status"), "Status", 30).toUpperCase();
        requireChoice(status, Set.of("DRAFT", "ACTIVE", "ARCHIVED"), "diet status");
        dietRepository.updateDietStatus(Validation.id(request.getParameter("id"), "diet plan"),
                status, userId(request), role(request));
        success(request, response, "Diet plan status updated.", "/diets");
    }
}
