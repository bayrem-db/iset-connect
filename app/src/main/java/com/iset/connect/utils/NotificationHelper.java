package com.iset.connect.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.iset.connect.R;

/** Android local notifications for updates already stored in the centralized MySQL database. */
public final class NotificationHelper {
    private static final String CHANNEL_ID = "iset_connect_updates";
    private static final int NOTIFICATION_ID = 1001;

    private NotificationHelper() { }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "ISET Connect updates", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Account, equipment and internship updates");
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
                && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Show one summary notification for unread server notifications. */
    public static void showUnreadSummary(Context context, int unreadCount) {
        if (unreadCount <= 0 || !canPost(context)) {
            return;
        }
        createChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("ISET Connect")
                .setContentText(unreadCount == 1
                        ? "You have 1 new notification"
                        : "You have " + unreadCount + " new notifications")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
}
