package com.iset.connect.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.iset.connect.models.User;

/** Local session storage. Only a bearer token and public profile fields are kept. */
public class SessionManager {
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_CIN = "cin";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_CLASS = "class_name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_STATUS = "status";
    private static final String KEY_BASE_URL = "base_url";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String token, User user) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, user.getId())
                .putString(KEY_FIRST_NAME, user.getFirstName())
                .putString(KEY_LAST_NAME, user.getLastName())
                .putString(KEY_CIN, user.getCin())
                .putString(KEY_EMAIL, user.getEmail())
                .putString(KEY_CLASS, user.getClassName())
                .putString(KEY_ROLE, user.getRole())
                .putString(KEY_STATUS, user.getStatus())
                .apply();
    }

    public void updateProfile(User user) {
        prefs.edit()
                .putString(KEY_FIRST_NAME, user.getFirstName())
                .putString(KEY_LAST_NAME, user.getLastName())
                .putString(KEY_CIN, user.getCin())
                .putString(KEY_EMAIL, user.getEmail())
                .putString(KEY_CLASS, user.getClassName())
                .apply();
    }

    public void logout() {
        // Keep the configured server address across sign-outs.
        String baseUrl = getBaseUrl();
        prefs.edit().clear().putString(KEY_BASE_URL, baseUrl).apply();
    }

    /** Server address configured at runtime (falls back to the BuildConfig value). */
    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, com.iset.connect.BuildConfig.BASE_URL);
    }

    public void setBaseUrl(String baseUrl) {
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, 0);
    }

    public String getFirstName() {
        return prefs.getString(KEY_FIRST_NAME, "");
    }

    public String getLastName() {
        return prefs.getString(KEY_LAST_NAME, "");
    }

    public String getFullName() {
        return (getFirstName() + " " + getLastName()).trim();
    }

    public String getCin() {
        return prefs.getString(KEY_CIN, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getClassName() {
        return prefs.getString(KEY_CLASS, "");
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "");
    }

    public String getStatus() {
        return prefs.getString(KEY_STATUS, "");
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(getRole());
    }

    public boolean isStudent() {
        return Constants.ROLE_STUDENT.equals(getRole());
    }
}
