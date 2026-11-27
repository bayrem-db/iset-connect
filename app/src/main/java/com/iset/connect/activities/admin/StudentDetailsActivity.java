package com.iset.connect.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iset.connect.R;
import com.iset.connect.adapters.ApplicationAdapter;
import com.iset.connect.adapters.LoanAdapter;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.Application;
import com.iset.connect.models.Loan;
import com.iset.connect.models.User;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Admin screen: student profile, account actions, and the student's loans/applications. */
public class StudentDetailsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;
    private long studentId = -1;

    private TextView tvName;
    private TextView tvCin;
    private TextView tvEmail;
    private TextView tvClass;
    private TextView tvStatus;
    private Button btnApprove;
    private Button btnReject;
    private Button btnSuspend;
    private Button btnReactivate;
    private ProgressBar progressBar;

    private final List<Loan> loans = new ArrayList<>();
    private final List<Application> applications = new ArrayList<>();
    private LoanAdapter loanAdapter;
    private ApplicationAdapter applicationAdapter;
    private TextView emptyLoans;
    private TextView emptyApplications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_details);

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

        studentId = getIntent().getLongExtra(Constants.EXTRA_STUDENT_ID, -1);
        if (studentId <= 0) {
            toast("Invalid student.");
            finish();
            return;
        }

        tvName = findViewById(R.id.tvStudentName);
        tvCin = findViewById(R.id.tvStudentCin);
        tvEmail = findViewById(R.id.tvStudentEmail);
        tvClass = findViewById(R.id.tvStudentClass);
        tvStatus = findViewById(R.id.tvStudentStatus);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnSuspend = findViewById(R.id.btnSuspend);
        btnReactivate = findViewById(R.id.btnReactivate);
        progressBar = findViewById(R.id.progressBar);
        emptyLoans = findViewById(R.id.emptyLoans);
        emptyApplications = findViewById(R.id.emptyApplications);

        RecyclerView loansRecycler = findViewById(R.id.recyclerViewLoans);
        RecyclerView applicationsRecycler = findViewById(R.id.recyclerViewApplications);
        loanAdapter = new LoanAdapter(loans, null, true);
        applicationAdapter = new ApplicationAdapter(applications, null, true);
        loansRecycler.setLayoutManager(new LinearLayoutManager(this));
        applicationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        loansRecycler.setNestedScrollingEnabled(false);
        applicationsRecycler.setNestedScrollingEnabled(false);
        loansRecycler.setAdapter(loanAdapter);
        applicationsRecycler.setAdapter(applicationAdapter);

        btnApprove.setOnClickListener(v -> performAction(api.approveStudent(studentId)));
        btnReject.setOnClickListener(v -> askReason("Reject student", reason ->
                performAction(api.rejectStudent(studentId, reason))));
        btnSuspend.setOnClickListener(v -> askReason("Suspend student", reason ->
                performAction(api.suspendStudent(studentId, reason))));
        btnReactivate.setOnClickListener(v -> performAction(api.reactivateStudent(studentId)));

        loadDetails();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadDetails() {
        showLoading();
        api.studentDetails(studentId).enqueue(new Callback<ApiResponse<ApiData.StudentDetailsData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.StudentDetailsData>> call,
                                   Response<ApiResponse<ApiData.StudentDetailsData>> response) {
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
                ApiResponse<ApiData.StudentDetailsData> body = response.body();
                if (!body.isSuccess()) {
                    toast(body.getMessage());
                    return;
                }
                ApiData.StudentDetailsData data = body.getData();
                if (data == null || data.student == null) {
                    toast("Student not found.");
                    return;
                }
                bindStudent(data.student);

                loans.clear();
                if (data.loans != null) loans.addAll(data.loans);
                loanAdapter.notifyDataSetChanged();
                emptyLoans.setVisibility(loans.isEmpty() ? View.VISIBLE : View.GONE);

                applications.clear();
                if (data.applications != null) applications.addAll(data.applications);
                applicationAdapter.notifyDataSetChanged();
                emptyApplications.setVisibility(applications.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.StudentDetailsData>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
            }
        });
    }

    private void bindStudent(User student) {
        tvName.setText(UiUtils.safe(student.getFullName()));
        tvCin.setText("CIN: " + UiUtils.safe(student.getCin()));
        tvEmail.setText(UiUtils.safe(student.getEmail()));
        tvClass.setText("Class: " + UiUtils.safe(student.getClassName()));
        UiUtils.applyStatus(tvStatus, student.getStatus());

        String status = student.getStatus();
        btnApprove.setEnabled(Constants.STATUS_PENDING.equals(status));
        btnReject.setEnabled(Constants.STATUS_PENDING.equals(status));
        btnSuspend.setEnabled(Constants.STATUS_APPROVED.equals(status));
        btnReactivate.setEnabled(Constants.STATUS_SUSPENDED.equals(status));
    }

    private void performAction(Call<ApiResponse<Object>> call) {
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
                if (body.isSuccess()) loadDetails();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
            }
        });
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
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
