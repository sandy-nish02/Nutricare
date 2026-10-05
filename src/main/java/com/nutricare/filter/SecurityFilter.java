package com.nutricare.filter;

import com.nutricare.repository.UserPatientRepository;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Authentication, account re-validation, CSRF protection, security headers, and one-time messages. */
@WebFilter("/*")
public class SecurityFilter implements Filter {
    private static final Set<String> PUBLIC_PATHS = Set.of("", "/", "/login", "/register", "/verify", "/verify/resend",
            "/forgot-password", "/reset-password", "/health", "/error");

    private final SecureRandom random = new SecureRandom();
    private final UserPatientRepository repository = new UserPatientRepository();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Cache-Control", "no-store");

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(true);
        if (session.getAttribute("csrf") == null) {
            byte[] token = new byte[24];
            random.nextBytes(token);
            session.setAttribute("csrf", Base64.getUrlEncoder().withoutPadding().encodeToString(token));
        }

        boolean publicPath = PUBLIC_PATHS.contains(path);
        if (!publicPath) {
            if (session.getAttribute("userId") == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            session = revalidate(request, session);
            if (session.getAttribute("userId") == null) {
                session.setAttribute("flashError", "Your account is no longer active. Contact a NutriCare administrator.");
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
        }
        moveFlash(session, request, "flashSuccess");
        moveFlash(session, request, "flashError");

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String supplied;
            try {
                supplied = request.getParameter("_csrf");
            } catch (RuntimeException uploadTooLarge) {
                // Reading parameters parses multipart bodies; an oversized payment slip fails here.
                session.setAttribute("flashError", "The upload was too large. Payment slips must be 2 MB or smaller.");
                response.sendRedirect(request.getContextPath() + (path.startsWith("/payments") ? "/payments" : "/dashboard"));
                return;
            }
            String expected = (String) session.getAttribute("csrf");
            if (expected == null || supplied == null || !constantTimeEquals(expected, supplied)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "The form expired. Refresh the page and try again.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Re-reads the signed-in account on every request so that deactivation and role changes made by an
     * administrator take effect immediately rather than when the session expires.
     */
    private HttpSession revalidate(HttpServletRequest request, HttpSession session) {
        long userId = (Long) session.getAttribute("userId");
        Optional<Map<String, Object>> user;
        try {
            user = repository.findUser(userId);
        } catch (SQLException exception) {
            request.getServletContext().log("Could not re-validate session for user #" + userId, exception);
            return session;
        }
        if (user.isEmpty() || !Boolean.TRUE.equals(user.get().get("active"))) {
            session.invalidate();
            return request.getSession(true);
        }
        session.setAttribute("role", String.valueOf(user.get().get("role")));
        session.setAttribute("fullName", String.valueOf(user.get().get("full_name")));
        return session;
    }

    private static void moveFlash(HttpSession session, HttpServletRequest request, String name) {
        Object value = session.getAttribute(name);
        if (value != null) {
            request.setAttribute(name, value);
            session.removeAttribute(name);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) return false;
        int difference = 0;
        for (int i = 0; i < left.length(); i++) difference |= left.charAt(i) ^ right.charAt(i);
        return difference == 0;
    }
}
