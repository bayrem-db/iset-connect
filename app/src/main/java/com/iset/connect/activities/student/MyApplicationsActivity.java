package com.iset.connect.activities.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iset.connect.R;
import com.iset.connect.activities.LoginActivity;
import com.iset.connect.activities.NotificationsActivity;
import com.iset.connect.adapters.ApplicationAdapter;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.Application;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.DateUtils;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Student's own internship applications. */
public class MyApplicationsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;
    private final List<Application> applications = new ArrayList<>();
    private ApplicationAdapter adapter;
    private Call<ApiResponse<ApiData.ApplicationListData>> currentCall;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;

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
        setContentView(R.layout.activity_my_applications);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        adapter = new ApplicationAdapter(applications, this::showApplicationDialog, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupBottomNav(R.id.nav_internships);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        if (currentCall != null) {
            currentCall.cancel();
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        currentCall = api.listApplications(null, null, null);
        currentCall.enqueue(new Callback<ApiResponse<ApiData.ApplicationListData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.ApplicationListData>> call,
                                   Response<ApiResponse<ApiData.ApplicationListData>> response) {
                progressBar.setVisibility(View.GONE);
                if (handleUnauthorized(response.code())) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    toast("Server error");
                    return;
                }
                ApiResponse<ApiData.ApplicationListData> body = response.body();
                if (!body.isSuccess()) {
                    toast(body.getMessage());
                    return;
                }
                applications.clear();
                if (body.getData() != null && body.getData().getApplications() != null) {
                    applications.addAll(body.getData().getApplications());
                }
                adapter.notifyDataSetChanged();
                emptyState.setVisibility(applications.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.ApplicationListData>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                if (call.isCanceled()) {
                    return;
                }
                toast(getString(R.string.error_network));
            }
        });
    }

    private void showApplicationDialog(Application application) {
        String details = "Internship: " + UiUtils.safe(application.getInternshipTitle())
                + "\nCompany: " + UiUtils.safe(application.getCompanyName())
                + "\nStatus: " + UiUtils.safe(application.getStatus())
                + "\nApplied: " + DateUtils.formatDateTime(application.getApplicationDate())
                + "\n\nMotivation:\n" + UiUtils.safe(application.getMotivationMessage());
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Application details")
                .setMessage(details)
                .setNegativeButton(R.string.cancel, null);
        if (Constants.STATUS_PENDING.equals(application.getStatus())) {
            builder.setPositiveButton("Cancel application",
                    (dialog, which) -> cancelApplication(application.getId()));
        } else {
            builder.setPositiveButton(android.R.string.ok, null);
        }
        builder.show();
    }

    private void cancelApplication(long applicationId) {
        api.cancelApplication(applicationId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                if (handleUnauthorized(response.code())) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    toast("Server error");
                    return;
                }
                ApiResponse<Object> body = response.body();
                toast(body.isSuccess() ? "Application cancelled" : body.getMessage());
                if (body.isSuccess()) {
                    loadData();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                toast(getString(R.string.error_network));
            }
        });
    }

    private boolean handleUnauthorized(int code) {
        if (code == 401) {
            session.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
