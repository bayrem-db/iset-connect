package com.iset.connect.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iset.connect.R;
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
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Admin screen: review internship applications and accept or reject pending ones. */
public class InternshipApplicationsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private EditText editSearch;
    private Spinner spinnerStatus;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;

    private final List<Application> applications = new ArrayList<>();
    private ApplicationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internship_applications);

        session = new SessionManager(this);
        if (!session.isLoggedIn() || !session.isAdmin()) {
            finish();
            return;
        }
        api = ApiClient.getService(session);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        editSearch = findViewById(R.id.editSearch);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnClear = findViewById(R.id.btnClear);

        List<String> statuses = new ArrayList<>();
        statuses.add("ALL");
        statuses.addAll(Arrays.asList(Constants.APPLICATION_STATUSES));
        spinnerStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses));

        adapter = new ApplicationAdapter(applications, this::showApplicationDialog, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> loadApplications());
        btnClear.setOnClickListener(v -> {
            editSearch.setText("");
            spinnerStatus.setSelection(0);
            loadApplications();
        });

        loadApplications();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadApplications() {
        String query = editSearch.getText().toString().trim();
        int position = spinnerStatus.getSelectedItemPosition();
        String status = position <= 0 ? null : Constants.APPLICATION_STATUSES[position - 1];

        showLoading();
        api.listApplications(query.isEmpty() ? null : query, status, null)
                .enqueue(new Callback<ApiResponse<ApiData.ApplicationListData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ApiData.ApplicationListData>> call,
                                           Response<ApiResponse<ApiData.ApplicationListData>> response) {
                        hideLoading();
                        if (handleUnauthorized(response)) return;
                        if (response.code() == 403) {
                            toast("Access denied.");
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
                        ApiData.ApplicationListData data = body.getData();
                        applications.clear();
                        if (data != null && data.getApplications() != null) {
                            applications.addAll(data.getApplications());
                        }
                        adapter.notifyDataSetChanged();
                        emptyState.setVisibility(applications.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApiData.ApplicationListData>> call, Throwable t) {
                        hideLoading();
                        toast(getString(R.string.error_network));
                    }
                });
    }

    private void showApplicationDialog(Application application) {
        String details = "Applicant: " + UiUtils.safe(application.getStudentName())
                + "\nCIN: " + UiUtils.safe(application.getCin())
                + "\nClass: " + UiUtils.safe(application.getClassName())
                + "\nInternship: " + UiUtils.safe(application.getInternshipTitle())
                + "\nCompany: " + UiUtils.safe(application.getCompanyName())
                + "\nApplied: " + DateUtils.formatDateTime(application.getApplicationDate())
                + "\nMotivation:\n" + UiUtils.safe(application.getMotivationMessage());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Application details")
                .setMessage(details);

        if (Constants.STATUS_PENDING.equals(application.getStatus())) {
            builder.setPositiveButton(R.string.accept, (dialog, which) ->
                            performWrite(api.acceptApplication(application.getId())))
                    .setNegativeButton(R.string.reject, (dialog, which) ->
                            askReason("Reject application", reason ->
                                    performWrite(api.rejectApplication(application.getId(), reason))))
                    .setNeutralButton("Close", null);
        } else {
            builder.setPositiveButton(R.string.cancel, null);
        }
        builder.show();
    }

    private interface ReasonAction {
        void run(String reason);
    }

    private void askReason(String title, ReasonAction action) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Reason (optional)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(2);

        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                getResources().getDisplayMetrics());
        FrameLayout frame = new FrameLayout(this);
        frame.setPadding(padding, padding / 2, padding, padding / 2);
        frame.addView(input, new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(frame)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) ->
                        action.run(input.getText().toString().trim()))
                .show();
    }

    private void performWrite(Call<ApiResponse<Object>> call) {
        showLoading();
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                hideLoading();
                if (handleUnauthorized(response)) return;
                if (response.code() == 403) {
                    toast("Access denied.");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    toast("Server error");
                    return;
                }
                ApiResponse<Object> body = response.body();
                toast(body.getMessage());
                if (body.isSuccess()) loadApplications();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
            }
        });
    }

    private boolean handleUnauthorized(Response<?> response) {
        if (response.code() != 401) return false;
        session.logout();
        Intent intent = new Intent();
        intent.setClassName(this, "com.iset.connect.activities.LoginActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        return true;
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
