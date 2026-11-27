package com.iset.connect.utils;

import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.iset.connect.R;

/** Shared UI helpers for status chips and consistent feedback. */
public final class UiUtils {
    private UiUtils() { }

    public static void applyStatus(TextView view, String status) {
        if (view == null || status == null) {
            return;
        }
        view.setText(status);
        int background;
        int color;
        switch (status) {
            case Constants.STATUS_APPROVED:
            case Constants.STATUS_ACCEPTED:
            case Constants.STATUS_RETURNED:
                background = R.drawable.bg_chip_approved;
                color = R.color.success;
                break;
            case Constants.STATUS_PENDING:
            case Constants.STATUS_DRAFT:
                background = R.drawable.bg_chip_pending;
                color = R.color.warning;
                break;
            case Constants.STATUS_REJECTED:
            case Constants.STATUS_SUSPENDED:
            case Constants.STATUS_CANCELLED:
            case Constants.STATUS_EXPIRED:
            case Constants.STATUS_CLOSED:
                background = R.drawable.bg_chip_rejected;
                color = R.color.danger;
                break;
            default:
                background = R.drawable.bg_chip_info;
                color = R.color.primary;
                break;
        }
        view.setBackgroundResource(background);
        view.setTextColor(ContextCompat.getColor(view.getContext(), color));
    }

    public static String safe(String value) {
        return value == null || value.isEmpty() ? "—" : value;
    }
}
