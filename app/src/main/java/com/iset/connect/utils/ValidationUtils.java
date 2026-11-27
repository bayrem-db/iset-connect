package com.iset.connect.utils;

import android.util.Patterns;

/** Client-side validation. The backend always repeats these checks. */
public final class ValidationUtils {
    private ValidationUtils() { }

    public static boolean notEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean validEmail(String value) {
        return notEmpty(value) && Patterns.EMAIL_ADDRESS.matcher(value).matches();
    }

    public static boolean validPassword(String value) {
        return value != null && value.length() >= 8;
    }

    public static boolean passwordsMatch(String password, String confirmation) {
        return notEmpty(password) && password.equals(confirmation);
    }

    public static boolean validCin(String value) {
        return notEmpty(value) && value.trim().length() >= 4;
    }

    public static boolean positiveNumber(String value) {
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
