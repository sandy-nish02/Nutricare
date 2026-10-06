package com.nutricare.controller;

import com.nutricare.service.OtpService;
import com.nutricare.util.Passwords;
import com.nutricare.util.Validation;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/** Registration, authentication, password recovery, and patient-profile workflows. */
@WebServlet(urlPatterns = {"/login", "/register", "/verify", "/verify/resend", "/logout", "/profile",
        "/profile/password", "/profile/deactivate", "/forgot-password", "/reset-password"})
public class UserPatientServlet extends BaseServlet {
    @Override
    protected void handleGet(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/login" -> showPublic(request, response, "Sign in", "login");
            case "/register" -> showPublic(request, response, "Create patient account", "register");
            case "/forgot-password" -> showPublic(request, response, "Forgot password", "forgot-password");
            case "/reset-password" -> showPublic(request, response, "Reset password", "reset-password");
            case "/verify" -> showVerification(request, response);
            case "/profile" -> profile(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void handlePost(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/login" -> login(request, response);
            case "/register" -> register(request, response);
            case "/verify" -> verify(request, response);
            case "/verify/resend" -> resendVerification(request, response);
            case "/forgot-password" -> forgotPassword(request, response);
            case "/reset-password" -> resetPassword(request, response);
            case "/logout" -> logout(request, response);
            case "/profile" -> updateProfile(request, response);
            case "/profile/password" -> updatePassword(request, response);
            case "/profile/deactivate" -> deactivatePatient(request, response);
            default -> throw new IllegalArgumentException("Unknown account action.");
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String email = Validation.email(request.getParameter("email"));
        String password = Validation.required(request.getParameter("password"), "Password", 100);
        Map<String, Object> user = patientRepository.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email or password is incorrect."));
        if (!Passwords.matches(password, String.valueOf(user.get("password_hash")))) {
            throw new IllegalArgumentException("Email or password is incorrect.");
        }
        long id = ((Number) user.get("id")).longValue();
        // The password is correct, so it is safe to explain why the account cannot sign in yet.
        if (!Boolean.TRUE.equals(user.get("verified"))) {
            startVerification(request, id);
            success(request, response, "Verify your phone to activate this account. A new code has been issued.", "/verify");
            return;
        }
        if (!Boolean.TRUE.equals(user.get("active"))) {
            throw new IllegalArgumentException("This account is deactivated. Please contact a NutriCare administrator.");
        }
        request.changeSessionId();
        HttpSession session = request.getSession();
        session.setAttribute("userId", id);
        session.setAttribute("role", String.valueOf(user.get("role")));
        session.setAttribute("fullName", String.valueOf(user.get("full_name")));
        patientRepository.log(id, "LOGIN", "Successful sign in");
        String name = String.valueOf(user.get("full_name"));
        success(request, response, "Welcome back, " + name + (name.endsWith(".") ? "" : "."), "/dashboard");
    }

    private void register(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String name = Validation.required(request.getParameter("fullName"), "Full name", 120);
        String email = Validation.email(request.getParameter("email"));
        String phone = Validation.phone(request.getParameter("phone"), true);
        String password = Validation.password(request.getParameter("password"), "Password");
        if (patientRepository.findUserByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists. Sign in instead; "
                    + "unverified accounts are sent a new code.");
        }
        long id = patientRepository.createPatient(name, email, phone, Passwords.hash(password), false);
        startVerification(request, id);
        success(request, response, "Account created. Verify the six-digit code to activate it.", "/verify");
    }

    /**
     * Issues a fresh verification code. The classroom build shows it on the verification page in place of an
     * SMS gateway; account activation does not depend on how the code is delivered.
     */
    private void startVerification(HttpServletRequest request, long userId) throws Exception {
        String otp = OtpService.generate();
        patientRepository.issueOtp(userId, OtpService.VERIFY, OtpService.hash(otp), OtpService.expiry());
        request.getSession().setAttribute("pendingVerification", userId);
        request.getSession().setAttribute("demoOtp", otp);
    }

    private void showVerification(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getSession().getAttribute("pendingVerification") == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }
        request.setAttribute("validityMinutes", OtpService.VALIDITY_MINUTES);
        render(request, response, "Verify patient account", "verify");
    }

    private void verify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long id = pendingVerification(request);
        String code = sixDigitCode(request.getParameter("otp"));
        OtpService.Result result = patientRepository.verifyAccount(id, OtpService.hash(code));
        if (result != OtpService.Result.VERIFIED) {
            if (result != OtpService.Result.WRONG_CODE) request.getSession().removeAttribute("demoOtp");
            throw new IllegalArgumentException(OtpService.message(result));
        }
        request.getSession().removeAttribute("pendingVerification");
        request.getSession().removeAttribute("demoOtp");
        success(request, response, "Phone verified. You can sign in now.", "/login");
    }

    private void resendVerification(HttpServletRequest request, HttpServletResponse response) throws Exception {
        startVerification(request, pendingVerification(request));
        success(request, response, "A new verification code has been issued. Earlier codes no longer work.", "/verify");
    }

    private static long pendingVerification(HttpServletRequest request) {
        if (!(request.getSession().getAttribute("pendingVerification") instanceof Long id)) {
            throw new IllegalArgumentException("Start registration again to request a code.");
        }
        return id;
    }

    /**
     * FR01 secure password reset. The response is identical whether or not the email exists, so the form
     * cannot be used to discover accounts. The classroom build writes the "SMS" to the server console.
     */
    private void forgotPassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String email = Validation.email(request.getParameter("email"));
        request.getSession().removeAttribute("resetUserId");
        Optional<Map<String, Object>> user = patientRepository.findUserByEmail(email)
                .filter(row -> Boolean.TRUE.equals(row.get("active")) && Boolean.TRUE.equals(row.get("verified")));
        if (user.isPresent()) {
            long id = ((Number) user.get().get("id")).longValue();
            String otp = OtpService.generate();
            patientRepository.issueOtp(id, OtpService.RESET, OtpService.hash(otp), OtpService.expiry());
            request.getSession().setAttribute("resetUserId", id);
            getServletContext().log("[SMS simulation] To " + user.get().get("phone") + " (" + email
                    + "): your NutriCare password reset code is " + otp + ". It expires in "
                    + OtpService.VALIDITY_MINUTES + " minutes.");
        }
        success(request, response, "If that email belongs to an active account, a reset code has been sent to "
                + "its registered phone. It expires in " + OtpService.VALIDITY_MINUTES + " minutes.", "/reset-password");
    }

    private void resetPassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String code = sixDigitCode(request.getParameter("otp"));
        String password = Validation.password(request.getParameter("newPassword"), "New password");
        if (!password.equals(request.getParameter("confirmPassword"))) {
            throw new IllegalArgumentException("The two new passwords do not match.");
        }
        if (!(request.getSession().getAttribute("resetUserId") instanceof Long id)) {
            throw new IllegalArgumentException(OtpService.message(OtpService.Result.WRONG_CODE));
        }
        OtpService.Result result = patientRepository.resetPassword(id, OtpService.hash(code), Passwords.hash(password));
        if (result != OtpService.Result.VERIFIED) {
            if (result != OtpService.Result.WRONG_CODE) request.getSession().removeAttribute("resetUserId");
            throw new IllegalArgumentException(OtpService.message(result));
        }
        request.getSession().removeAttribute("resetUserId");
        success(request, response, "Password reset. Sign in with your new password.", "/login");
    }

    private static String sixDigitCode(String value) {
        String code = Validation.required(value, "Verification code", 6);
        if (!code.matches("\\d{6}")) {
            throw new IllegalArgumentException("Enter the six-digit code.");
        }
        return code;
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().invalidate();
        response.sendRedirect(request.getContextPath() + "/login");
    }

    /** Patients get their health profile; staff get their account details and password form. */
    private void profile(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireLogin(request);
        if ("PATIENT".equals(role(request))) {
            request.setAttribute("patient", patientRepository.patientByUser(userId(request)).orElseThrow());
            render(request, response, "My health profile", "profile");
        } else {
            request.setAttribute("account", patientRepository.findUser(userId(request)).orElseThrow());
            render(request, response, "My account", "profile");
        }
    }

    private void updateProfile(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireRole(request, "PATIENT");
        Double height = optionalDouble(request.getParameter("height"), "Height", 50, 260);
        Double weight = optionalDouble(request.getParameter("weight"), "Weight", 10, 500);
        int flagged = patientRepository.updateProfile(userId(request),
                Validation.required(request.getParameter("fullName"), "Full name", 120),
                Validation.phone(request.getParameter("phone"), true),
                Validation.dateOfBirth(request.getParameter("dob")),
                Validation.optional(request.getParameter("gender"), 20), height, weight,
                Validation.optional(request.getParameter("allergies"), 500),
                Validation.optional(request.getParameter("conditions"), 800),
                Validation.optional(request.getParameter("medications"), 800),
                Validation.optional(request.getParameter("emergency"), 120));
        success(request, response, flagged == 0 ? "Health profile updated."
                : "Health profile updated. " + flagged + " diet plan(s) contain a new allergy and were sent to your nutritionist for review.",
                "/profile");
    }

    private void updatePassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireLogin(request);
        String current = Validation.required(request.getParameter("currentPassword"), "Current password", 100);
        String next = Validation.password(request.getParameter("newPassword"), "New password");
        Map<String, Object> user = patientRepository.findUser(userId(request)).orElseThrow();
        if (!Passwords.matches(current, String.valueOf(user.get("password_hash")))) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        patientRepository.updatePassword(userId(request), Passwords.hash(next));
        success(request, response, "Password changed successfully.", "/profile");
    }

    private void deactivatePatient(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireRole(request, "PATIENT");
        long id = userId(request);
        patientRepository.deactivatePatient(id);
        request.getSession().invalidate();
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("flashSuccess", "Your account was deactivated. Contact an administrator to restore it.");
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private void showPublic(HttpServletRequest request, HttpServletResponse response, String title, String view)
            throws ServletException, IOException {
        if (request.getSession().getAttribute("userId") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        render(request, response, title, view);
    }
}
