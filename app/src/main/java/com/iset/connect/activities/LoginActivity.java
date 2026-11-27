package com.iset.connect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iset.connect.R;
import com.iset.connect.activities.admin.AdminDashboardActivity;
import com.iset.connect.activities.student.StudentDashboardActivity;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.User;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.ValidationUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Email/password sign in against auth/login.php. */
public class LoginActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private EditText etEmail;
    private EditText etPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);
        api = ApiClient.getService(session);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        progressBar = findViewById(R.id.progressBar);

        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvForgot = findViewById(R.id.tv_forgot_password);
        TextView tvRegister = findViewById(R.id.tv_register);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvForgot.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        TextView tvServer = findViewById(R.id.tv_server_settings);
        tvServer.setOnClickListener(v -> showServerSettings());
    }

    /** Allows one APK to work on an emulator or a physical device without rebuilding. */
    private void showServerSettings() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(session.getBaseUrl());
        input.setSelection(input.getText().length());
        new MaterialAlertDialogBuilder(this)
                .setTitle("Server address")
                .setMessage("Emulator: http://10.0.2.2/iset_connect/backend/api/\n"
                        + "Physical device: http://YOUR_PC_IP/iset_connect/backend/api/")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        toast("Address must start with http:// or https://");
                        return;
                    }
                    if (!url.endsWith("/")) {
                        url += "/";
                    }
                    session.setBaseUrl(url);
                    api = ApiClient.getService(session);
                    toast("Server address saved.");
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        etEmail.setError(null);
        etPassword.setError(null);

        if (!ValidationUtils.validEmail(email)) {
            etEmail.setError("Enter a valid email address.");
            etEmail.requestFocus();
            return;
        }
        if (!ValidationUtils.notEmpty(password)) {
            etPassword.setError("Password is required.");
            etPassword.requestFocus();
            return;
        }

        showLoading();
        api.login(email, password).enqueue(new Callback<ApiResponse<ApiData.LoginData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.LoginData>> call,
                                   Response<ApiResponse<ApiData.LoginData>> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ApiData.LoginData> body = response.body();
                    if (!body.isSuccess() || body.getData() == null || body.getData().getUser() == null
                            || TextUtils.isEmpty(body.getData().getToken())) {
                        toast(TextUtils.isEmpty(body.getMessage()) ? "Unable to sign in." : body.getMessage());
                        return;
                    }
                    ApiData.LoginData data = body.getData();
                    session.saveLogin(data.getToken(), data.getUser());
                    routeByUser(data.getUser());
                    return;
                }

                String raw = readError(response);
                if (response.code() == 403) {
                    String status = statusFrom(raw);
                    if (Constants.STATUS_PENDING.equals(status)
                            || Constants.STATUS_REJECTED.equals(status)
                            || Constants.STATUS_SUSPENDED.equals(status)) {
                        routeByStatus(status);
                        return;
                    }
                }
                toast(messageFrom(raw, "Unable to sign in."));
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.LoginData>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
            }
        });
    }

    private void routeByUser(User user) {
        String role = user.getRole();
        String status = user.getStatus();

        if (Constants.ROLE_ADMIN.equals(role)) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
            return;
        }
        if (Constants.ROLE_STUDENT.equals(role)) {
            if (Constants.STATUS_PENDING.equals(status)) {
                routeByStatus(status);
            } else if (Constants.STATUS_REJECTED.equals(status) || Constants.STATUS_SUSPENDED.equals(status)) {
                routeByStatus(status);
            } else {
                startActivity(new Intent(this, StudentDashboardActivity.class));
                finish();
            }
            return;
        }
        session.logout();
        toast("Unknown account role. Please contact an administrator.");
    }

    private void routeByStatus(String status) {
        Intent intent;
        if (Constants.STATUS_PENDING.equals(status)) {
            intent = new Intent(this, WaitingApprovalActivity.class);
        } else {
            intent = new Intent(this, RejectedAccountActivity.class);
        }
        intent.putExtra(Constants.EXTRA_STATUS, status);
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

    private String statusFrom(String raw) {
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonElement data = root.get("data");
            if (data != null && data.isJsonObject()) {
                JsonElement status = data.getAsJsonObject().get("status");
                if (status != null && status.isJsonPrimitive()) {
                    return status.getAsString();
                }
            }
        } catch (Exception ignored) {
            // Fall through to the generic error message.
        }
        return "";
    }

    private String messageFrom(String raw, String fallback) {
        if (!TextUtils.isEmpty(raw)) {
            try {
                ApiResponse<?> body = new Gson().fromJson(raw, ApiResponse.class);
                if (body != null && !TextUtils.isEmpty(body.getMessage())) {
                    return body.getMessage();
                }
            } catch (Exception ignored) {
                // Fall through to the generic error message.
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
