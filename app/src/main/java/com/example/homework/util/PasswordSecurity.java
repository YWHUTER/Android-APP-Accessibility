package com.example.homework.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordSecurity {
    private static final String HASH_PREFIX = "sha256:";

    private PasswordSecurity() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(HASH_PREFIX);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash password", exception);
        }
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith(HASH_PREFIX)) {
            return storedPassword.equals(hash(rawPassword));
        }
        return storedPassword.equals(rawPassword);
    }

    public static boolean isLegacyPlaintext(String storedPassword) {
        return storedPassword != null && !storedPassword.startsWith(HASH_PREFIX);
    }
}
