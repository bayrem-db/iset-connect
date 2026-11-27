package com.iset.connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iset.connect.R;
import com.iset.connect.models.Notification;
import com.iset.connect.utils.DateUtils;
import com.iset.connect.utils.UiUtils;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    public interface OnNotificationClick { void onNotificationClick(Notification notification); }

    private final List<Notification> notifications;
    private final OnNotificationClick listener;

    public NotificationAdapter(List<Notification> notifications, OnNotificationClick listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification item = notifications.get(position);
        holder.title.setText(UiUtils.safe(item.getTitle()));
        holder.message.setText(UiUtils.safe(item.getMessage()));
        holder.type.setText(UiUtils.safe(item.getType()).replace('_', ' '));
        holder.date.setText(DateUtils.formatDateTime(item.getCreatedAt()));
        holder.unread.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(item);
        });
    }

    @Override
    public int getItemCount() { return notifications == null ? 0 : notifications.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, message, type, date;
        final View unread;
        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tv_notification_title);
            message = view.findViewById(R.id.tv_notification_message);
            type = view.findViewById(R.id.tv_notification_type);
            date = view.findViewById(R.id.tv_notification_date);
            unread = view.findViewById(R.id.view_unread);
        }
    }
}
