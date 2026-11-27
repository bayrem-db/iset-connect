package com.iset.connect.models;

public class Notification {
    public long id;
    public String title;
    public String message;
    public String notification_type;
    public int is_read;
    public String created_at;

    public long getId() { return id; }
    public String getTitle() { return title == null ? "" : title; }
    public String getMessage() { return message == null ? "" : message; }
    public String getType() { return notification_type == null ? "" : notification_type; }
    public boolean isRead() { return is_read == 1; }
    public String getCreatedAt() { return created_at == null ? "" : created_at; }
}
