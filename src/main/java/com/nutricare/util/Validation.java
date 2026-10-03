package com.nutricare.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class Validation {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private Validation() { }

    public static String required(String value, String field, int max) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " is required.");
        String clean = value.trim();
        if (clean.length() > max) throw new IllegalArgumentException(field + " is too long.");
        return clean;
    }

    public static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (clean.length() > max) throw new IllegalArgumentException("A value is too long.");
        return clean;
    }

    public static String email(String value) {
        String clean = required(value, "Email", 160).toLowerCase();
        if (!EMAIL.matcher(clean).matches()) throw new IllegalArgumentException("Enter a valid email address.");
        return clean;
    }

    public static long id(String value, String field) {
        try { long result = Long.parseLong(value); if (result < 1) throw new NumberFormatException(); return result; }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Select a valid " + field + "."); }
    }

    public static int integer(String value, String field, int min, int max) {
        try { int result = Integer.parseInt(value); if (result < min || result > max) throw new NumberFormatException(); return result; }
        catch (NumberFormatException e) { throw new IllegalArgumentException(field + " must be between " + min + " and " + max + "."); }
    }

    public static double decimal(String value, String field, double min, double max) {
        try { double result = Double.parseDouble(value); if (!Double.isFinite(result) || result < min || result > max) throw new NumberFormatException(); return result; }
        catch (NumberFormatException e) { throw new IllegalArgumentException(field + " must be between " + min + " and " + max + "."); }
    }

    public static BigDecimal money(String value) {
        try { BigDecimal result = new BigDecimal(value).setScale(2); if (result.signum() <= 0 || result.compareTo(new BigDecimal("1000000")) > 0) throw new NumberFormatException(); return result; }
        catch (Exception e) { throw new IllegalArgumentException("Enter a valid positive amount."); }
    }

    public static LocalDate date(String value, String field) {
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException | NullPointerException e) { throw new IllegalArgumentException("Enter a valid " + field + "."); }
    }

    public static LocalDateTime dateTime(String value, String field) {
        try { return LocalDateTime.parse(value); }
        catch (DateTimeParseException | NullPointerException e) { throw new IllegalArgumentException("Enter a valid " + field + "."); }
    }

    /** Proposal NFR: at least 8 characters; design AD01 also requires a letter and a number. */
    public static String password(String value, String field) {
        String password = value == null ? "" : value;
        if (password.length() < 8 || password.length() > 100
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(field + " must be 8 to 100 characters with a letter and a number.");
        }
        return password;
    }

    /** Accepts local (0771234567) or international (+94 77 123 4567) numbers. */
    public static String phone(String value, boolean required) {
        String clean = required ? required(value, "Phone", 30) : optional(value, 30);
        if (clean == null) return null;
        String digits = clean.replaceAll("[\\s-]", "");
        if (!digits.matches("\\+?\\d{9,15}")) throw new IllegalArgumentException("Enter a valid phone number.");
        return clean;
    }

    public static LocalDate dateOfBirth(String value) {
        if (value == null || value.isBlank()) return null;
        LocalDate dob = date(value, "date of birth");
        if (dob.isAfter(LocalDate.now()) || dob.isBefore(LocalDate.now().minusYears(130))) {
            throw new IllegalArgumentException("Enter a realistic date of birth.");
        }
        return dob;
    }
}
