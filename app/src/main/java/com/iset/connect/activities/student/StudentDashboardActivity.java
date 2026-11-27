package com.iset.connect.activities.student;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.iset.connect.R;
import com.iset.connect.activities.LoginActivity;
import com.iset.connect.activities.NotificationsActivity;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.User;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.NotificationHelper;
import com.iset.connect.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Student home: personalised welcome, live statistics, quick actions and bottom navigation. */
public class StudentDashboardActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private TextView tvWelcome;
    private TextView tvClass;
    private ProgressBar progressBar;
    private int currentNavItem = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        session = new SessionManager(this);
        api = ApiClient.getService(session);
        NotificationHelper.createChannel(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        }

        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }
        if (!session.isStudent()) {
            if (session.isAdmin()) {
                startActivity(new Intent(this, com.iset.connect.activities.admin.AdminDashboardActivity.class));
                finish();
            } else {
                goToLogin();
            }
            return;
        }

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvWelcome = findViewById(R.id.tv_welcome);
        tvClass = findViewById(R.id.tv_class);
        progressBar = findViewById(R.id.progressBar);

        tvWelcome.setText("Welcome, " + session.getFullName() + "!");
        tvClass.setText(session.getClassName());

        findViewById(R.id.action_equipment).setOnClickListener(
                v -> startActivity(new Intent(this, EquipmentListActivity.class)));
        findViewById(R.id.action_loans).setOnClickListener(
                v -> startActivity(new Intent(this, MyLoansActivity.class)));
        findViewById(R.id.action_internships).setOnClickListener(
                v -> startActivity(new Intent(this, InternshipListActivity.class)));
        findViewById(R.id.action_applications).setOnClickListener(
                v -> startActivity(new Intent(this, MyApplicationsActivity.class)));
        findViewById(R.id.action_profile).setOnClickListener(
                v -> startActivity(new Intent(this, StudentProfileActivity.class)));
        findViewById(R.id.action_notifications).setOnClickListener(
                v -> startActivity(new Intent(this, NotificationsActivity.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                currentNavItem = id;
                return true;
            }
            Class<?> target = null;
            if (id == R.id.nav_equipment) {
                target = EquipmentListActivity.class;
            } else if (id == R.id.nav_internships) {
                target = InternshipListActivity.class;
            } else if (id == R.id.nav_notifications) {
                target = NotificationsActivity.class;
            } else if (id == R.id.nav_profile) {
                target = StudentProfileActivity.class;
            }
            if (target != null && id != currentNavItem) {
                currentNavItem = id;
                startActivity(new Intent(this, target));
            }
            return true;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);

        loadStats();
    }

    private void loadStats() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        api.studentStats().enqueue(new Callback<ApiResponse<ApiData.StudentStats>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.StudentStats>> call,
                                   Response<ApiResponse<ApiData.StudentStats>> response) {
                progressBar.setVisibility(ProgressBar.GONE);
                if (response.code() == 401) {
                    session.logout();
                    goToLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ApiData.StudentStats> body = response.body();
                    if (!body.isSuccess() || body.getData() == null) {
                        toast(!TextUtils.isEmpty(body.getMessage()) ? body.getMessage() : "Unable to load statistics.");
                        return;
                    }
                    ApiData.StudentStats data = body.getData();
                    User user = data.getUser();
                    if (user != null) {
                        session.saveLogin(session.getToken(), user);
                        tvWelcome.setText("Welcome, " + session.getFullName() + "!");
                        tvClass.setText(session.getClassName());
                    }
                    setStat(R.id.stat_available_materials, data.available_materials);
                    setStat(R.id.stat_my_loans, data.my_loans);
                    setStat(R.id.stat_my_borrowed, data.my_borrowed);
                    setStat(R.id.stat_available_internships, data.available_internships);
                    setStat(R.id.stat_my_applications, data.my_applications);
                    setStat(R.id.stat_unread_notifications, data.unread_notifications);
                    NotificationHelper.showUnreadSummary(StudentDashboardActivity.this, data.unread_notifications);
                    return;
                }
                toast(messageFrom(readError(response), "Unable to load statistics."));
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.StudentStats>> call, Throwable t) {
                progressBar.setVisibility(ProgressBar.GONE);
                toast(getString(R.string.error_network));
            }
        });
    }

    private void setStat(int textViewId, int value) {
        TextView view = findViewById(textViewId);
        if (view != null) {
            view.setText(String.valueOf(value));
        }
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
