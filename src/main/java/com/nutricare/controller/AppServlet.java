package com.nutricare.controller;

import com.nutricare.repository.AppointmentRepository;
import com.nutricare.repository.DashboardRepository;
import com.nutricare.repository.DietPlanRepository;
import com.nutricare.repository.HealthCheckupRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Shared entry points that do not belong to one business module. */
@WebServlet(urlPatterns = {"", "/dashboard", "/health", "/error"})
public class AppServlet extends BaseServlet {
    private final DashboardRepository dashboardRepository = new DashboardRepository();
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();
    private final HealthCheckupRepository checkupRepository = new HealthCheckupRepository();
    private final DietPlanRepository dietRepository = new DietPlanRepository();

    @Override
    protected void handleGet(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/" -> render(request, response, "Healthy Choices, Better Lives", "home");
            case "/dashboard" -> dashboard(request, response);
            case "/health" -> health(response);
            case "/error" -> error(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void dashboard(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireLogin(request);
        long userId = userId(request);
        String role = role(request);
        request.setAttribute("counts", dashboardRepository.dashboardCounts(userId, role));
        if (!"FINANCE".equals(role)) {
            request.setAttribute("upcoming", appointmentRepository.upcomingAppointments(userId, role));
        }
        if ("DOCTOR".equals(role) || "ADMIN".equals(role)) {
            request.setAttribute("abnormalCheckups", checkupRepository.recentAbnormalCheckups(userId, role));
        }
        if ("DOCTOR".equals(role)) {
            request.setAttribute("awaitingCheckup", appointmentRepository.appointmentsAwaitingCheckup(userId));
        }
        if ("NUTRITIONIST".equals(role) || "ADMIN".equals(role)) {
            request.setAttribute("reviews", dietRepository.dietPlansNeedingReview(userId, role));
        }
        render(request, response, "Dashboard", "dashboard");
    }

    private void health(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"UP\",\"application\":\"NutriCare\"}");
    }

    private void error(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Object code = request.getAttribute("jakarta.servlet.error.status_code");
        boolean notFound = code != null && code.toString().equals("404");
        request.setAttribute("pageTitle", notFound ? "Page not found" : "Something went wrong");
        request.setAttribute("errorMessage", notFound
                ? "The page you requested does not exist."
                : "The request could not be completed safely.");
        request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
    }
}
