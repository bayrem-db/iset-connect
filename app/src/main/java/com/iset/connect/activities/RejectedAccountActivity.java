package com.iset.connect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iset.connect.R;
import com.iset.connect.activities.student.StudentDashboardActivity;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.User;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** REJECTED / SUSPENDED account screen with a live profile refresh. */
public class RejectedAccountActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private String status;
    private TextView tvStatusTitle;
    private TextView tvStatusMessage;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rejected_account);

        session = new SessionManager(this);
        api = ApiClient.getService(session);

        status = getIntent().getStringExtra(Constants.EXTRA_STATUS);
        if (TextUtils.isEmpty(status)) {
            status = session.getStatus();
        }
        if (!Constants.STATUS_SUSPENDED.equals(status)) {
            status = Constants.STATUS_REJECTED;
        }

        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvStatusMessage = findViewById(R.id.tv_status_message);
        progressBar = findViewById(R.id.progressBar);
        applyStatusText();

        Button btnRefresh = findViewById(R.id.btn_refresh_status);
        Button btnLogout = findViewById(R.id.btn_logout);
        btnRefresh.setOnClickListener(v -> refreshStatus());
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void applyStatusText() {
        if (Constants.STATUS_SUSPENDED.equals(status)) {
            tvStatusTitle.setText("Account suspended");
            tvStatusMessage.setText("Your access to ISET Connect is temporarily suspended. "
                    + "Please contact an administrator for more information.");
        } else {
            tvStatusTitle.setText("Account rejected");
            tvStatusMessage.setText("Your registration was rejected by an administrator. "
                    + "Please contact the administration if you think this is a mistake.");
        }
    }

    private void refreshStatus() {
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }
        showLoading();
        api.getProfile().enqueue(new Callback<ApiResponse<ApiData.UserData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.UserData>> call,
                                   Response<ApiResponse<ApiData.UserData>> response) {
                hideLoading();
                if (response.code() == 401) {
                    session.logout();
                    goToLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ApiData.UserData> body = response.body();
                    if (!body.isSuccess() || body.getData() == null || body.getData().getUser() == null) {
                        toast(!TextUtils.isEmpty(body.getMessage()) ? body.getMessage() : "Unable to load status.");
                        return;
                    }
                    User user = body.getData().getUser();
                    session.saveLogin(session.getToken(), user);
                    String newStatus = user.getStatus();

                    if (Constants.STATUS_APPROVED.equals(newStatus)) {
                        toast("Your account has been approved.");
                        startActivity(new Intent(RejectedAccountActivity.this, StudentDashboardActivity.class));
                        finish();
                    } else if (Constants.STATUS_PENDING.equals(newStatus)) {
                        startActivity(new Intent(RejectedAccountActivity.this, WaitingApprovalActivity.class));
                        finish();
                    } else {
                        status = newStatus;
                        applyStatusText();
                        toast("Status: " + newStatus);
                    }
                    return;
                }
                toast(messageFrom(readError(response), "Unable to refresh your status."));
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.UserData>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
            }
        });
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign out?")
                .setMessage("You can sign in again after your account status changes.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        if (!session.isLoggedIn()) {
            session.logout();
            goToLogin();
            return;
        }
        showLoading();
        api.logout().enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                hideLoading();
                session.logout();
                goToLogin();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                hideLoading();
                session.logout();
                toast(getString(R.string.error_network));
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
    }

    private void hideLoading() {
        progressBar.setVisibility(ProgressBar.GONE);
    }

    private void toast(String message) {
        Toast.makeText(this, TextUtils.isEmpty(message) ? "Request failed." : message, Toast.LENGTH_SHORT).show();
    }
}
