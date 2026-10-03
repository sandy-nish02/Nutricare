package com.nutricare.controller;

import com.nutricare.repository.BaseRepository;
import com.nutricare.repository.UserPatientRepository;
import com.nutricare.util.Validation;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;

/** Shared request handling used by the module-specific controllers. */
public abstract class BaseServlet extends HttpServlet {
    protected final UserPatientRepository patientRepository = new UserPatientRepository();

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestPath = path(request);
        try {
            handleGet(request, response, requestPath);
        } catch (SecurityException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
        } catch (Exception exception) {
            getServletContext().log("Request failed: " + requestPath, exception);
            if (!response.isCommitted()) {
                request.setAttribute("errorMessage", "We could not load this page. Please try again.");
                request.setAttribute("pageTitle", "Something went wrong");
                request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
            }
        }
    }

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String requestPath = path(request);
        try {
            handlePost(request, response, requestPath);
        } catch (SecurityException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            fail(request, response, exception.getMessage(), fallback(requestPath));
        } catch (SQLException exception) {
            getServletContext().log("Database action failed: " + requestPath, exception);
            String message = BaseRepository.isDuplicateKey(exception)
                    ? "That email or reference is already in use."
                    : "The database could not save that change.";
            fail(request, response, message, fallback(requestPath));
        } catch (Exception exception) {
            getServletContext().log("Action failed: " + requestPath, exception);
            fail(request, response, "Something went wrong. No changes were saved.", fallback(requestPath));
        }
    }

    protected abstract void handleGet(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception;

    protected void handlePost(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        throw new IllegalArgumentException("Unknown action.");
    }

    protected final void render(HttpServletRequest request, HttpServletResponse response, String title, String view)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", title);
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    protected final long patientId(HttpServletRequest request) throws SQLException {
        return patientRepository.patientByUser(userId(request))
                .map(row -> ((Number) row.get("id")).longValue())
                .orElseThrow(() -> new IllegalArgumentException("Patient profile was not found."));
    }

    protected static String path(HttpServletRequest request) {
        String requestPath = request.getServletPath();
        return requestPath == null || requestPath.isBlank() ? "/" : requestPath;
    }

    protected static long userId(HttpServletRequest request) {
        return (Long) request.getSession().getAttribute("userId");
    }

    protected static String role(HttpServletRequest request) {
        return (String) request.getSession().getAttribute("role");
    }

    protected static void requireLogin(HttpServletRequest request) {
        if (request.getSession().getAttribute("userId") == null) {
            throw new SecurityException("Sign in required.");
        }
    }

    protected static void requireRole(HttpServletRequest request, String required) {
        requireLogin(request);
        if (!required.equals(role(request))) {
            throw new SecurityException("You do not have permission to use this page.");
        }
    }

    protected static void requireAnyRole(HttpServletRequest request, String... roles) {
        requireLogin(request);
        if (Arrays.stream(roles).noneMatch(role(request)::equals)) {
            throw new SecurityException("You do not have permission to use this page.");
        }
    }

    protected static void requireChoice(String value, Set<String> allowed, String field) {
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException("Select a valid " + field + ".");
        }
    }

    protected static Integer optionalInt(String value, String field, int min, int max) {
        return value == null || value.isBlank() ? null : Validation.integer(value, field, min, max);
    }

    protected static Double optionalDouble(String value, String field, double min, double max) {
        return value == null || value.isBlank() ? null : Validation.decimal(value, field, min, max);
    }

    protected static String safeFileName(String input) {
        if (input == null || input.isBlank()) return "payment-slip";
        String normalized = input.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    protected static void success(HttpServletRequest request, HttpServletResponse response,
                                  String message, String target) throws IOException {
        request.getSession().setAttribute("flashSuccess", message);
        response.sendRedirect(request.getContextPath() + target);
    }

    private static void fail(HttpServletRequest request, HttpServletResponse response,
                             String message, String target) throws IOException {
        request.getSession().setAttribute("flashError", message);
        response.sendRedirect(request.getContextPath() + target);
    }

    private static String fallback(String requestPath) {
        for (String module : new String[]{"/profile", "/appointments", "/checkups", "/diets", "/payments", "/staff"}) {
            if (requestPath.startsWith(module)) return module;
        }
        if (requestPath.startsWith("/verify")) return "/verify";
        return requestPath;
    }
}
