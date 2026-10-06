package com.nutricare.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/** Generates short-lived OTPs and stores only a one-way digest. */
public final class OtpService {
    /** Design document 6.1: OTPs expire after five minutes. */
    public static final int VALIDITY_MINUTES = 5;
    /** A code is burned after this many wrong guesses, so it cannot be brute-forced. */
    public static final int MAX_ATTEMPTS = 5;

    public static final String VERIFY = "VERIFY";
    public static final String RESET = "RESET";

    public enum Result { VERIFIED, WRONG_CODE, EXPIRED, TOO_MANY_ATTEMPTS }

    private static final SecureRandom RANDOM = new SecureRandom();
    private OtpService() { }

    public static String generate() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static LocalDateTime expiry() {
        return LocalDateTime.now().plusMinutes(VALIDITY_MINUTES);
    }

    public static String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static boolean sameHash(String expected, String supplied) {
        return expected != null && supplied != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    public static String message(Result result) {
        return switch (result) {
            case VERIFIED -> "Code accepted.";
            case WRONG_CODE -> "That code is incorrect. Check the six digits and try again.";
            case EXPIRED -> "That code has expired. Request a new code.";
            case TOO_MANY_ATTEMPTS -> "Too many incorrect attempts. Request a new code.";
        };
    }
}
