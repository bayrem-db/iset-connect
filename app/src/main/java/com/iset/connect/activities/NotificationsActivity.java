package com.iset.connect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iset.connect.R;
import com.iset.connect.adapters.NotificationAdapter;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.Notification;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Notification inbox backed by notifications/list.php and notifications/mark_read.php. */
public class NotificationsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private final List<Notification> notifications = new ArrayList<>();
    private NotificationAdapter adapter;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;
    private TextView tvUnreadCount;
    private int unreadCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        session = new SessionManager(this);
        api = ApiClient.getService(session);

        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.rv_notifications);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        tvUnreadCount = findViewById(R.id.tv_unread_count);

        adapter = new NotificationAdapter(notifications, this::onNotificationClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Button btnMarkAll = findViewById(R.id.btn_mark_all_read);
        btnMarkAll.setOnClickListener(v -> confirmMarkAllRead());

        loadNotifications();
    }

    private void loadNotifications() {
        showLoading();
        api.listNotifications(null).enqueue(new Callback<ApiResponse<ApiData.NotificationListData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.NotificationListData>> call,
                                   Response<ApiResponse<ApiData.NotificationListData>> response) {
                hideLoading();
                if (response.code() == 401) {
                    session.logout();
                    goToLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ApiData.NotificationListData> body = response.body();
                    if (!body.isSuccess() || body.getData() == null) {
                        toast(!TextUtils.isEmpty(body.getMessage()) ? body.getMessage() : "Unable to load notifications.");
                        return;
                    }
                    ApiData.NotificationListData data = body.getData();
                    notifications.clear();
                    if (data.getNotifications() != null) {
                        notifications.addAll(data.getNotifications());
                    }
                    adapter.notifyDataSetChanged();
                    unreadCount = data.getUnreadCount();
                    updateUnreadText();
                    emptyState.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    return;
                }
                toast(messageFrom(readError(response), "Unable to load notifications."));
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.NotificationListData>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
            }
        });
    }

    private void onNotificationClick(Notification notification) {
        if (notification.isRead()) {
            return;
        }
        api.markNotificationRead(notification.getId(), null).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.code() == 401) {
                    session.logout();
                    goToLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    int position = notifications.indexOf(notification);
                    notification.is_read = 1;
                    if (position >= 0) {
                        adapter.notifyItemChanged(position);
                    }
                    unreadCount = Math.max(0, unreadCount - 1);
                    updateUnreadText();
                } else {
                    toast(messageFrom(readError(response), "Unable to mark notification as read."));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                toast(getString(R.string.error_network));
            }
        });
    }

    private void confirmMarkAllRead() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Mark all as read?")
                .setMessage("Every notification in your inbox will be marked as read.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> markAllRead())
                .show();
    }

    private void markAllRead() {
        api.markNotificationRead(null, 1).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.code() == 401) {
                    session.logout();
                    goToLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    toast(!TextUtils.isEmpty(response.body().getMessage())
                            ? response.body().getMessage() : "All notifications marked as read.");
                    loadNotifications();
                } else {
                    toast(messageFrom(readError(response), "Unable to mark all notifications as read."));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                toast(getString(R.string.error_network));
            }
        });
    }

    private void updateUnreadText() {
        tvUnreadCount.setText(unreadCount + (unreadCount == 1 ? " unread notification" : " unread notifications"));
    }

    private String readError(Response<?> response) {
        if (response.errorBody() == null) {
            return "";
        }
        try {
            return response.errorBody().string();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String messageFrom(String raw, String fallback) {
        if (!TextUtils.isEmpty(raw)) {
            try {
                ApiResponse<?> body = new Gson().fromJson(raw, ApiResponse.class);
                if (body != null && !TextUtils.isEmpty(body.getMessage())) {
                    return body.getMessage();
                }
            } catch (Exception ignored) {
                // Fall through to the generic message.
            }
        }
        return fallback;
    }

    private void showLoading() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(ProgressBar.GONE);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, TextUtils.isEmpty(message) ? "Request failed." : message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
