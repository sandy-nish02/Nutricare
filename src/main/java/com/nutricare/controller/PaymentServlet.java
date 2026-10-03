package com.nutricare.controller;

import com.nutricare.repository.PaymentRepository;
import com.nutricare.util.Validation;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.util.Map;
import java.util.Set;

/** Payment submission, slip handling, finance verification, and receipts. */
@WebServlet(urlPatterns = {"/payments", "/payments/create", "/payments/status",
        "/payments/slip", "/payments/receipt"})
// The container limit is deliberately looser than the 2 MB business rule: a request over the container
// limit is cut off mid-upload (the browser just sees a reset), whereas the rule below returns a clear message.
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 11 * 1024 * 1024)
public class PaymentServlet extends BaseServlet {
    private static final long MAX_SLIP_BYTES = 2 * 1024 * 1024;
    private final PaymentRepository paymentRepository = new PaymentRepository();

    @Override
    protected void handleGet(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/payments" -> payments(request, response);
            case "/payments/slip" -> slip(request, response);
            case "/payments/receipt" -> receipt(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void handlePost(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        switch (path) {
            case "/payments/create" -> create(request, response);
            case "/payments/status" -> updateStatus(request, response);
            default -> throw new IllegalArgumentException("Unknown payment action.");
        }
    }

    private void payments(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "PATIENT", "FINANCE", "ADMIN");
        request.setAttribute("payments", paymentRepository.payments(userId(request), role(request)));
        request.setAttribute("appointmentOptions", paymentRepository.payableAppointments(userId(request), role(request)));
        if (!"PATIENT".equals(role(request))) request.setAttribute("patients", patientRepository.patients());
        render(request, response, "Payments and billing", "payments");
    }

    private void create(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "PATIENT", "FINANCE", "ADMIN");
        long selectedPatient = "PATIENT".equals(role(request))
                ? patientId(request) : Validation.id(request.getParameter("patientId"), "patient");
        String appointment = request.getParameter("appointmentId");
        Long appointmentId = appointment == null || appointment.isBlank()
                ? null : Validation.id(appointment, "appointment");
        String method = Validation.required(request.getParameter("method"), "Payment method", 40);
        requireChoice(method, Set.of("BANK_TRANSFER", "CARD", "CASH"), "payment method");
        Part slip = request.getPart("slip");
        byte[] slipBytes = null;
        String slipName = null;
        String slipType = null;
        if (slip != null && slip.getSize() > 0) {
            if (slip.getSize() > MAX_SLIP_BYTES) {
                throw new IllegalArgumentException("Payment slips must be 2 MB or smaller.");
            }
            slipType = slip.getContentType();
            if (!Set.of("application/pdf", "image/jpeg", "image/png").contains(slipType)) {
                throw new IllegalArgumentException("Payment slip must be a PDF, JPG, or PNG file.");
            }
            slipBytes = slip.getInputStream().readAllBytes();
            slipName = safeFileName(slip.getSubmittedFileName());
        }
        if ("BANK_TRANSFER".equals(method) && slipBytes == null) {
            throw new IllegalArgumentException("Upload a payment slip for a bank transfer.");
        }
        paymentRepository.createPayment(selectedPatient, appointmentId,
                Validation.money(request.getParameter("amount")),
                Validation.required(request.getParameter("reference"), "Transaction reference", 100),
                method, slipName, slipType, slipBytes, userId(request));
        success(request, response, "Payment submitted for verification.", "/payments");
    }

    private void slip(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "PATIENT", "FINANCE", "ADMIN");
        Map<String, Object> payment = paymentRepository.paymentDocument(
                        Validation.id(request.getParameter("id"), "payment"), userId(request), role(request))
                .orElseThrow(() -> new IllegalArgumentException("Payment was not found."));
        byte[] data = (byte[]) payment.get("payment_slip_data");
        if (data == null) throw new IllegalArgumentException("This payment has no uploaded slip.");
        response.setContentType(String.valueOf(payment.get("payment_slip_type")));
        response.setHeader("Content-Disposition", "inline; filename=\""
                + safeFileName(String.valueOf(payment.get("payment_slip_name"))) + "\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    private void receipt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "PATIENT", "FINANCE", "ADMIN");
        request.setAttribute("payment", paymentRepository.paymentDocument(
                        Validation.id(request.getParameter("id"), "payment"), userId(request), role(request))
                .orElseThrow(() -> new IllegalArgumentException("Payment was not found.")));
        render(request, response, "Payment receipt", "receipt");
    }

    private void updateStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAnyRole(request, "FINANCE", "ADMIN");
        String status = Validation.required(request.getParameter("status"), "Status", 30).toUpperCase();
        requireChoice(status, Set.of("VERIFIED", "REJECTED", "REFUNDED"), "payment status");
        boolean appointmentConfirmed = paymentRepository.updatePaymentStatus(
                Validation.id(request.getParameter("id"), "payment"), status, userId(request));
        success(request, response, appointmentConfirmed
                ? "Payment verified and the linked appointment is now CONFIRMED."
                : "Payment marked " + status + ".", "/payments");
    }
}
