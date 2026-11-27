package com.iset.connect.activities.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iset.connect.R;
import com.iset.connect.activities.ChangePasswordActivity;
import com.iset.connect.activities.LoginActivity;
import com.iset.connect.activities.NotificationsActivity;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.User;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.UiUtils;
import com.iset.connect.utils.ValidationUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Student account information and settings. */
public class StudentProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private TextView tvName, tvCin, tvEmail, tvClass, tvStatus;
    private EditText etFirstName, etLastName, etClassName, etEmail;
    private MaterialButton btnSave, btnChangePassword, btnNotifications, btnSignOut;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!session.isStudent()) {
            finish();
            return;
        }
        api = ApiClient.getService(session);
        setContentView(R.layout.activity_student_profile);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tvProfileName);
        tvCin = findViewById(R.id.tvProfileCin);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvClass = findViewById(R.id.tvProfileClass);
        tvStatus = findViewById(R.id.tvProfileStatus);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etClassName = findViewById(R.id.etClassName);
        etEmail = findViewById(R.id.etEmail);
        btnSave = findViewById(R.id.btnSave);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnSignOut = findViewById(R.id.btnSignOut);
        progressBar = findViewById(R.id.progressBar);

        btnSave.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        btnSignOut.setOnClickListener(v -> confirmSignOut());

        setupBottomNav(R.id.nav_profile);
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        api.getProfile().enqueue(new Callback<ApiResponse<ApiData.UserData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.UserData>> call,
                                   Response<ApiResponse<ApiData.UserData>> response) {
                progressBar.setVisibility(View.GONE);
                if (handleUnauthorized(response.code())) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    toast("Server error");
                    return;
                }
                ApiResponse<ApiData.UserData> body = response.body();
                if (!body.isSuccess()) {
                    toast(body.getMessage());
                    return;
                }
                if (body.getData() == null || body.getData().getUser() == null) {
                    toast("Profile not found");
                    return;
                }
                bind(body.getData().getUser());
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.UserData>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                toast(getString(R.string.error_network));
            }
        });
    }

    private void bind(User user) {
        tvName.setText(UiUtils.safe(user.getFullName()));
        tvCin.setText("CIN: " + UiUtils.safe(user.getCin()));
        tvEmail.setText(UiUtils.safe(user.getEmail()));
        tvClass.setText(UiUtils.safe(user.getClassName()));
        UiUtils.applyStatus(tvStatus, user.getStatus());

        etFirstName.setText(user.getFirstName());
        etLastName.setText(user.getLastName());
        etClassName.setText(user.getClassName());
        etEmail.setText(user.getEmail());

        session.updateProfile(user);
    }

    private void saveProfile() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String className = etClassName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (!ValidationUtils.notEmpty(firstName) || !ValidationUtils.notEmpty(lastName)
                || !ValidationUtils.notEmpty(className)) {
            toast("Fill in first name, last name and class");
            return;
        }
        if (!ValidationUtils.validEmail(email)) {
            toast("Enter a valid email address");
            return;
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        api.updateProfile(firstName, lastName, className, email)
                .enqueue(new Callback<ApiResponse<ApiData.UserData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ApiData.UserData>> call,
                                           Response<ApiResponse<ApiData.UserData>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        if (handleUnauthorized(response.code())) {
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            toast("Server error");
                            return;
                        }
                        ApiResponse<ApiData.UserData> body = response.body();
                        if (!body.isSuccess()) {
                            toast(body.getMessage());
                            return;
                        }
                        User updated = null;
                        if (body.getData() != null) {
                            updated = body.getData().getUser();
                        }
                        if (updated == null) {
                            updated = new User();
                            updated.first_name = firstName;
                            updated.last_name = lastName;
                            updated.class_name = className;
                            updated.email = email;
                            updated.cin = session.getCin();
                            updated.role = session.getRole();
                            updated.status = session.getStatus();
                        }
                        session.updateProfile(updated);
                        bind(updated);
                        toast("Profile updated");
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApiData.UserData>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        toast(getString(R.string.error_network));
                    }
                });
    }

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign out?")
                .setMessage("You will need to sign in again to continue.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        api.logout().enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                finishSignOut();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                finishSignOut();
            }
        });
    }

    private void finishSignOut() {
        session.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean handleUnauthorized(int code) {
        if (code == 401) {
            finishSignOut();
            return true;
        }
        return false;
    }

    private void toast(String message) {
        Toast.makeText(this,
                message == null || message.isEmpty() ? getString(R.string.error_network) : message,
                Toast.LENGTH_SHORT).show();
    }

    private BottomNavigationView findBottomNav() {
        int navId = getResources().getIdentifier("bottomNavStudent", "id", getPackageName());
        if (navId == 0) {
            navId = getResources().getIdentifier("bottom_navigation", "id", getPackageName());
        }
        return navId == 0 ? null : findViewById(navId);
    }

    private void setupBottomNav(int currentItemId) {
        BottomNavigationView bottomNav = findBottomNav();
        if (bottomNav == null) {
            return;
        }
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentItemId) {
                return true;
            }
            Class<?> target = null;
            if (id == R.id.nav_home) {
                target = StudentDashboardActivity.class;
            } else if (id == R.id.nav_equipment) {
                target = EquipmentListActivity.class;
            } else if (id == R.id.nav_internships) {
                target = InternshipListActivity.class;
            } else if (id == R.id.nav_notifications) {
                target = NotificationsActivity.class;
            } else if (id == R.id.nav_profile) {
                target = StudentProfileActivity.class;
            }
            if (target != null) {
                startActivity(new Intent(this, target));
                finish();
            }
            return true;
        });
        bottomNav.setSelectedItemId(currentItemId);
    }
}
