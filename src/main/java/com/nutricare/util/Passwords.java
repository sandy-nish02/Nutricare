package com.nutricare.util;

import org.mindrot.jbcrypt.BCrypt;

/** Keeps password hashing details out of controllers and repositories. */
public final class Passwords {
    private Passwords() { }

    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt(12));
    }

    public static boolean matches(String plainText, String hash) {
        return plainText != null && hash != null && BCrypt.checkpw(plainText, hash);
    }
}
