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
import com.iset.connect.utils.ValidationUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Application form for a published internship. */
public class ApplyInternshipActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;
    private long internshipId;

    private TextView tvInternshipTitle, tvDeadline, tvDeadlineStatus;
    private EditText etMotivation;
    private MaterialButton btnSubmit;
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
        setContentView(R.layout.activity_apply_internship);

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

        tvInternshipTitle = findViewById(R.id.tvInternshipTitle);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvDeadlineStatus = findViewById(R.id.tvDeadlineStatus);
        etMotivation = findViewById(R.id.etMotivation);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        btnSubmit.setOnClickListener(v -> submitApplication());

        setupBottomNav(R.id.nav_internships);
        loadInternship();
    }

    private void loadInternship() {
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
                Internship item = body.getData().getInternship();
                tvInternshipTitle.setText(UiUtils.safe(item.getTitle())
                        + " — " + UiUtils.safe(item.getCompanyName()));
                tvDeadline.setText("Application deadline: "
                        + DateUtils.formatDisplay(item.getApplicationDeadline()));

                if (DateUtils.isAfter(DateUtils.today(), item.getApplicationDeadline())) {
                    tvDeadlineStatus.setText("The application deadline has passed.");
                    tvDeadlineStatus.setVisibility(View.VISIBLE);
                    btnSubmit.setEnabled(false);
                } else if (item.hasApplied()) {
                    tvDeadlineStatus.setText("You have already applied for this internship.");
                    tvDeadlineStatus.setVisibility(View.VISIBLE);
                    btnSubmit.setEnabled(false);
                } else {
                    tvDeadlineStatus.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.InternshipData>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                toast(getString(R.string.error_network));
            }
        });
    }

    private void submitApplication() {
        String message = etMotivation.getText().toString().trim();
        if (!ValidationUtils.notEmpty(message)) {
            toast("Write a motivation message");
            return;
        }
        if (!btnSubmit.isEnabled()) {
            return;
        }
        btnSubmit.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        api.createApplication(internshipId, message).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (handleUnauthorized(response.code())) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    btnSubmit.setEnabled(true);
                    toast("Server error");
                    return;
                }
                ApiResponse<Object> body = response.body();
                toast(body.isSuccess() ? "Application submitted" : body.getMessage());
                if (body.isSuccess()) {
                    finish();
                } else {
                    btnSubmit.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
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
