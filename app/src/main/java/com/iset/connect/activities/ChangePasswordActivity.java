package com.iset.connect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.iset.connect.R;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.ValidationUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Change password against auth/change_password.php; all tokens are revoked server-side. */
public class ChangePasswordActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private EditText etCurrentPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        session = new SessionManager(this);
        api = ApiClient.getService(session);

        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }

        setSupportActionBar((androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        progressBar = findViewById(R.id.progressBar);

        Button btnChangePassword = findViewById(R.id.btn_change_password);
        btnChangePassword.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String current = etCurrentPassword.getText().toString();
        String password = etNewPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        etCurrentPassword.setError(null);
        etNewPassword.setError(null);
        etConfirmPassword.setError(null);

        if (!ValidationUtils.notEmpty(current)) {
            etCurrentPassword.setError("Current password is required.");
            etCurrentPassword.requestFocus();
            return;
        }
        if (!ValidationUtils.validPassword(password)) {
            etNewPassword.setError("Password must be at least 8 characters.");
            etNewPassword.requestFocus();
            return;
        }
        if (!ValidationUtils.passwordsMatch(password, confirm)) {
            etConfirmPassword.setError("Passwords do not match.");
            etConfirmPassword.requestFocus();
            return;
        }

        showLoading();
        api.changePassword(current, password, confirm).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                hideLoading();
                if (response.code() == 401) {
                    session.logout();
                    goToLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> body = response.body();
                    if (body.isSuccess()) {
                        toast(!TextUtils.isEmpty(body.getMessage())
                                ? body.getMessage() : "Password changed. Please sign in again.");
                        session.logout();
                        goToLogin();
                    } else {
                        toast(!TextUtils.isEmpty(body.firstError()) ? body.firstError() : "Unable to change password.");
                    }
                    return;
                }
                toast(messageFrom(readError(response), "Unable to change password."));
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
