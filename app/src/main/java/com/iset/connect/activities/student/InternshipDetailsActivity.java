package com.iset.connect.activities.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.iset.connect.R;
import com.iset.connect.activities.LoginActivity;
import com.iset.connect.activities.NotificationsActivity;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.Internship;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.DateUtils;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.UiUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Full internship information for a student. */
public class InternshipDetailsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;
    private long internshipId;

    private TextView tvTitle, tvCompany, tvDomain, tvAddress, tvDescription, tvSkills;
    private TextView tvDates, tvDeadline, tvPositions, tvAppliedHint, statusChip;
    private MaterialButton btnApply;
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
        setContentView(R.layout.activity_internship_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        internshipId = getIntent().getLongExtra(Constants.EXTRA_INTERNSHIP_ID, -1L);
        if (internshipId <= 0) {
            toast("Missing internship");
            finish();
            return;
        }

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvCompany = findViewById(R.id.tvDetailCompany);
        tvDomain = findViewById(R.id.tvDetailDomain);
        tvAddress = findViewById(R.id.tvDetailAddress);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvSkills = findViewById(R.id.tvDetailSkills);
        tvDates = findViewById(R.id.tvDetailDates);
        tvDeadline = findViewById(R.id.tvDetailDeadline);
        tvPositions = findViewById(R.id.tvDetailPositions);
        tvAppliedHint = findViewById(R.id.tvAppliedHint);
        statusChip = findViewById(R.id.statusChip);
        btnApply = findViewById(R.id.btnApply);
        progressBar = findViewById(R.id.progressBar);

        btnApply.setOnClickListener(v -> {
            Intent intent = new Intent(this, ApplyInternshipActivity.class);
            intent.putExtra(Constants.EXTRA_INTERNSHIP_ID, internshipId);
            startActivity(intent);
        });

        setupBottomNav(R.id.nav_internships);
        loadDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDetails();
    }

    private void loadDetails() {
        progressBar.setVisibility(View.VISIBLE);
        api.internshipDetails(internshipId).enqueue(new Callback<ApiResponse<ApiData.InternshipData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.InternshipData>> call,
                                   Response<ApiResponse<ApiData.InternshipData>> response) {
                progressBar.setVisibility(View.GONE);
                if (handleUnauthorized(response.code())) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    toast("Server error");
                    return;
                }
                ApiResponse<ApiData.InternshipData> body = response.body();
                if (!body.isSuccess()) {
                    toast(body.getMessage());
                    return;
                }
                if (body.getData() == null || body.getData().getInternship() == null) {
                    toast("Internship not found");
                    return;
                }
                bind(body.getData().getInternship());
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.InternshipData>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                toast(getString(R.string.error_network));
            }
        });
    }

    private void bind(Internship item) {
        tvTitle.setText(UiUtils.safe(item.getTitle()));
        tvCompany.setText(UiUtils.safe(item.getCompanyName()));
        tvDomain.setText(UiUtils.safe(item.getDomain()));
        tvAddress.setText(UiUtils.safe(item.getCompanyAddress()));
        tvDescription.setText(UiUtils.safe(item.getDescription()));
        tvSkills.setText(UiUtils.safe(item.getRequiredSkills()));
        tvDates.setText(DateUtils.formatDisplay(item.getStartDate())
                + " → " + DateUtils.formatDisplay(item.getEndDate()));
        tvDeadline.setText("Apply by " + DateUtils.formatDisplay(item.getApplicationDeadline()));
        tvPositions.setText(item.getAvailablePositions() + " / " + item.getTotalPositions()
                + " positions available");
        UiUtils.applyStatus(statusChip, item.getStatus());

        boolean applied = item.hasApplied();
        btnApply.setVisibility(applied ? View.GONE : View.VISIBLE);
        tvAppliedHint.setVisibility(applied ? View.VISIBLE : View.GONE);
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
