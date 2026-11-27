package com.iset.connect.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Date helpers for API dates (yyyy-MM-dd / yyyy-MM-dd HH:mm:ss). */
public final class DateUtils {
    private DateUtils() { }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String formatDisplay(String apiDate) {
        if (apiDate == null || apiDate.isEmpty()) {
            return "—";
        }
        try {
            Date date = apiDate.length() > 10
                    ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(apiDate)
                    : new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(apiDate);
            return date == null ? apiDate : new SimpleDateFormat("dd MMM yyyy", Locale.US).format(date);
        } catch (ParseException e) {
            return apiDate;
        }
    }

    public static String formatDateTime(String apiDate) {
        if (apiDate == null || apiDate.isEmpty()) {
            return "—";
        }
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(apiDate);
            return date == null ? apiDate : new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(date);
        } catch (ParseException e) {
            return apiDate;
        }
    }

    public static boolean isAfter(String date, String other) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return format.parse(date).after(format.parse(other));
        } catch (Exception e) {
            return false;
        }
    }
}
