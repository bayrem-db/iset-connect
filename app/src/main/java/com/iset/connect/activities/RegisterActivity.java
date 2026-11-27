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

/** Student registration form; accounts start as PENDING. */
public class RegisterActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private EditText etFirstName;
    private EditText etLastName;
    private EditText etCin;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private EditText etClassName;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        session = new SessionManager(this);
        api = ApiClient.getService(session);

        setSupportActionBar((androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etCin = findViewById(R.id.et_cin);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etClassName = findViewById(R.id.et_class_name);
        progressBar = findViewById(R.id.progressBar);

        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvBackLogin = findViewById(R.id.tv_back_login);
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvBackLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String cin = etCin.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String className = etClassName.getText().toString().trim();

        etFirstName.setError(null);
        etLastName.setError(null);
        etCin.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
        etClassName.setError(null);

        if (!ValidationUtils.notEmpty(firstName)) {
            etFirstName.setError("First name is required.");
            etFirstName.requestFocus();
            return;
        }
        if (!ValidationUtils.notEmpty(lastName)) {
            etLastName.setError("Last name is required.");
            etLastName.requestFocus();
            return;
        }
        if (!ValidationUtils.validCin(cin)) {
            etCin.setError("Enter a valid CIN (at least 4 characters).");
            etCin.requestFocus();
            return;
        }
        if (!ValidationUtils.validEmail(email)) {
            etEmail.setError("Enter a valid email address.");
            etEmail.requestFocus();
            return;
        }
        if (!ValidationUtils.validPassword(password)) {
            etPassword.setError("Password must be at least 8 characters.");
            etPassword.requestFocus();
            return;
        }
        if (!ValidationUtils.passwordsMatch(password, confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match.");
            etConfirmPassword.requestFocus();
            return;
        }
        if (!ValidationUtils.notEmpty(className)) {
            etClassName.setError("Class / group is required.");
            etClassName.requestFocus();
            return;
        }

        showLoading();
        api.register(firstName, lastName, cin, email, password, confirmPassword, className)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        hideLoading();
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Object> body = response.body();
                            if (body.isSuccess()) {
                                toast(TextUtils.isEmpty(body.getMessage())
                                        ? "Registration submitted." : body.getMessage());
                                startActivity(new Intent(RegisterActivity.this, WaitingApprovalActivity.class));
                                finish();
                            } else {
                                toast(!TextUtils.isEmpty(body.firstError())
                                        ? body.firstError() : "Registration failed.");
                            }
                            return;
                        }
                        toast(messageFrom(readError(response), "Registration failed."));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        hideLoading();
                        toast(getString(R.string.error_network));
                    }
                });
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
