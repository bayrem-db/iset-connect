package com.iset.connect.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.iset.connect.R;
import com.iset.connect.activities.LoginActivity;
import com.iset.connect.activities.NotificationsActivity;
import com.iset.connect.activities.student.StudentDashboardActivity;
import com.iset.connect.models.ApiData;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Administrator home: live statistics, quick actions and the navigation drawer. */
public class AdminDashboardActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private DrawerLayout drawerLayout;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        session = new SessionManager(this);
        api = ApiClient.getService(session);

        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }
        if (!session.isAdmin()) {
            if (session.isStudent()) {
                startActivity(new Intent(this, StudentDashboardActivity.class));
                finish();
            } else {
                goToLogin();
            }
            return;
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        progressBar = findViewById(R.id.progressBar);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationIcon(R.drawable.ic_dashboard);

        TextView tvAdminName = findViewById(R.id.tv_admin_name);
        tvAdminName.setText(session.getFullName());

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_logout) {
                drawerLayout.closeDrawers();
                confirmLogout();
                return true;
            }
            Class<?> target = null;
            if (id == R.id.nav_admin_students) {
                target = ManageStudentsActivity.class;
            } else if (id == R.id.nav_admin_materials) {
                target = ManageMaterialsActivity.class;
            } else if (id == R.id.nav_admin_loans) {
                target = BorrowRequestsActivity.class;
            } else if (id == R.id.nav_admin_internships) {
                target = ManageInternshipsActivity.class;
            } else if (id == R.id.nav_admin_applications) {
                target = InternshipApplicationsActivity.class;
            } else if (id == R.id.nav_admin_notifications) {
                target = NotificationsActivity.class;
            }
            drawerLayout.closeDrawers();
            if (target != null) {
                startActivity(new Intent(this, target));
            }
            return true;
        });

        findViewById(R.id.action_manage_students).setOnClickListener(
                v -> startActivity(new Intent(this, ManageStudentsActivity.class)));
        findViewById(R.id.action_manage_materials).setOnClickListener(
                v -> startActivity(new Intent(this, ManageMaterialsActivity.class)));
        findViewById(R.id.action_borrow_requests).setOnClickListener(
                v -> startActivity(new Intent(this, BorrowRequestsActivity.class)));
        findViewById(R.id.action_manage_internships).setOnClickListener(
                v -> startActivity(new Intent(this, ManageInternshipsActivity.class)));
        findViewById(R.id.action_internship_applications).setOnClickListener(
                v -> startActivity(new Intent(this, InternshipApplicationsActivity.class)));
        findViewById(R.id.action_notifications).setOnClickListener(
                v -> startActivity(new Intent(this, NotificationsActivity.class)));

        loadStats();
    }

    private void loadStats() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        api.adminStats().enqueue(new Callback<ApiResponse<ApiData.AdminStats>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.AdminStats>> call,
                                   Response<ApiResponse<ApiData.AdminStats>> response) {
                progressBar.setVisibility(ProgressBar.GONE);
                if (response.code() == 401) {
                    session.logout();
                    goToLogin();
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ApiData.AdminStats> body = response.body();
                    if (!body.isSuccess() || body.getData() == null) {
                        toast(!TextUtils.isEmpty(body.getMessage()) ? body.getMessage() : "Unable to load statistics.");
                        return;
                    }
                    ApiData.AdminStats data = body.getData();
                    setStat(R.id.stat_total_students, data.total_students);
                    setStat(R.id.stat_pending_students, data.pending_students);
                    setStat(R.id.stat_approved_students, data.approved_students);
                    setStat(R.id.stat_total_materials, data.total_materials);
                    setStat(R.id.stat_available_materials, data.available_materials);
                    setStat(R.id.stat_borrowed_materials, data.borrowed_materials);
                    setStat(R.id.stat_total_loans, data.total_loans);
                    setStat(R.id.stat_pending_loans, data.pending_loans);
                    setStat(R.id.stat_total_internships, data.total_internships);
                    setStat(R.id.stat_published_internships, data.published_internships);
                    setStat(R.id.stat_pending_applications, data.pending_applications);
                    setStat(R.id.stat_unread_notifications, data.unread_notifications);
                    return;
                }
                toast(messageFrom(readError(response), "Unable to load statistics."));
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.AdminStats>> call, Throwable t) {
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

    private void confirmLogout() {
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
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                session.logout();
                goToLogin();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                session.logout();
                toast(getString(R.string.error_network));
                goToLogin();
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
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
