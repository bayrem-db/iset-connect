package com.iset.connect.utils;

import com.iset.connect.BuildConfig;

/** Application-wide constants. BASE_URL is configured per build type in app/build.gradle.kts. */
public final class Constants {
    private Constants() { }

    public static final String BASE_URL = BuildConfig.BASE_URL;
    public static final String PREF_NAME = "iset_connect_session";

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_STUDENT_ID = "student_id";
    public static final String EXTRA_MATERIAL_ID = "material_id";
    public static final String EXTRA_LOAN_ID = "loan_id";
    public static final String EXTRA_INTERNSHIP_ID = "internship_id";
    public static final String EXTRA_APPLICATION_ID = "application_id";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_TITLE = "title";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STUDENT = "STUDENT";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";

    public static final String[] MATERIAL_CATEGORIES = {
            "Computers", "Audiovisual", "Electronics", "Networking", "Accessories", "Laboratory", "Other"
    };
    public static final String[] INTERNSHIP_DOMAINS = {
            "Web Development", "Mobile Development", "Software Engineering", "Networking",
            "Cybersecurity", "Artificial Intelligence", "Database Administration"
    };
    public static final String[] ACCOUNT_STATUSES = {
            STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED, STATUS_SUSPENDED
    };
    public static final String[] LOAN_STATUSES = {
            STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED, STATUS_CANCELLED, STATUS_RETURNED
    };
    public static final String[] APPLICATION_STATUSES = {
            STATUS_PENDING, STATUS_ACCEPTED, STATUS_REJECTED, STATUS_CANCELLED
    };
}
