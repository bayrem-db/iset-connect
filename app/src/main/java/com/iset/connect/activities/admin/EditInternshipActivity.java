package com.iset.connect.activities.admin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iset.connect.R;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.Internship;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.DateUtils;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.ValidationUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Admin screen: load, update, publish, close and delete an internship. */
public class EditInternshipActivity extends AppCompatActivity {

    private static final String[] STATUSES = {
            Constants.STATUS_DRAFT, Constants.STATUS_PUBLISHED, Constants.STATUS_CLOSED
    };

    private SessionManager session;
    private ApiService api;
    private long internshipId = -1;

    private EditText etTitle;
    private EditText etCompanyName;
    private EditText etCompanyAddress;
    private Spinner spinnerDomain;
    private EditText etDescription;
    private EditText etSkills;
    private EditText etStartDate;
    private EditText etEndDate;
    private EditText etDeadline;
    private EditText etTotalPositions;
    private Spinner spinnerStatus;
    private ProgressBar progressBar;
    private Button btnSave;
    private Button btnPublish;
    private Button btnClose;
    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_internship);

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

        internshipId = getIntent().getLongExtra(Constants.EXTRA_INTERNSHIP_ID, -1);
        if (internshipId <= 0) {
            toast("Invalid internship.");
            finish();
            return;
        }

        etTitle = findViewById(R.id.etInternshipTitle);
        etCompanyName = findViewById(R.id.etCompanyName);
        etCompanyAddress = findViewById(R.id.etCompanyAddress);
        spinnerDomain = findViewById(R.id.spinnerInternshipDomain);
        etDescription = findViewById(R.id.etDescription);
        etSkills = findViewById(R.id.etRequiredSkills);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etDeadline = findViewById(R.id.etDeadline);
        etTotalPositions = findViewById(R.id.etTotalPositions);
        spinnerStatus = findViewById(R.id.spinnerInternshipStatus);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSaveInternship);
        btnPublish = findViewById(R.id.btnPublish);
        btnClose = findViewById(R.id.btnClose);
        btnDelete = findViewById(R.id.btnDelete);

        setupDateField(etStartDate);
        setupDateField(etEndDate);
        setupDateField(etDeadline);

        btnSave.setOnClickListener(v -> saveInternship());
        btnPublish.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Publish internship?")
                .setMessage("Approved students will be notified.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Publish", (dialog, which) ->
                        performWrite(api.publishInternship(internshipId), false))
                .show());
        btnClose.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Close internship?")
                .setMessage("The offer will no longer accept applications.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Close", (dialog, which) ->
                        performWrite(api.closeInternship(internshipId), false))
                .show());
        btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Delete internship?")
                .setMessage("This cannot be undone. Offers with applications cannot be deleted.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Delete", (dialog, which) ->
                        performWrite(api.deleteInternship(internshipId), true))
                .show());

        loadInternship();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadInternship() {
        showLoading();
        api.internshipDetails(internshipId).enqueue(new Callback<ApiResponse<ApiData.InternshipData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.InternshipData>> call,
                                   Response<ApiResponse<ApiData.InternshipData>> response) {
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
                ApiResponse<ApiData.InternshipData> body = response.body();
                if (!body.isSuccess()) {
                    toast(body.getMessage());
                    return;
                }
                ApiData.InternshipData data = body.getData();
                if (data == null || data.getInternship() == null) {
                    toast("Internship not found.");
                    return;
                }
                bindInternship(data.getInternship());
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.InternshipData>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
            }
        });
    }

    private void bindInternship(Internship internship) {
        etTitle.setText(internship.getTitle());
        etCompanyName.setText(internship.getCompanyName());
        etCompanyAddress.setText(internship.getCompanyAddress());
        etDescription.setText(internship.getDescription());
        etSkills.setText(internship.getRequiredSkills());
        etStartDate.setText(internship.getStartDate());
        etEndDate.setText(internship.getEndDate());
        etDeadline.setText(internship.getApplicationDeadline());
        etTotalPositions.setText(String.valueOf(internship.getTotalPositions()));

        List<String> domains = new ArrayList<>(Arrays.asList(Constants.INTERNSHIP_DOMAINS));
        if (!domains.contains(internship.getDomain())) {
            domains.add(0, internship.getDomain());
        }
        spinnerDomain.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, domains));
        int domainIndex = domains.indexOf(internship.getDomain());
        if (domainIndex >= 0) spinnerDomain.setSelection(domainIndex);

        List<String> statuses = new ArrayList<>(Arrays.asList(STATUSES));
        if (!statuses.contains(internship.getStatus())) {
            statuses.add(0, internship.getStatus());
        }
        spinnerStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses));
        int statusIndex = statuses.indexOf(internship.getStatus());
        if (statusIndex >= 0) spinnerStatus.setSelection(statusIndex);
    }

    private void setupDateField(EditText field) {
        field.setOnClickListener(v -> showDatePicker(field));
    }

    private void showDatePicker(EditText field) {
        Calendar calendar = Calendar.getInstance();
        String value = field.getText().toString().trim();
        if (!value.isEmpty()) {
            try {
                java.util.Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value);
                if (parsed != null) calendar.setTime(parsed);
            } catch (ParseException ignored) {
                // Keep today's date as the picker's starting point.
            }
        }
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                field.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveInternship() {
        String title = etTitle.getText().toString().trim();
        String company = etCompanyName.getText().toString().trim();
        String address = etCompanyAddress.getText().toString().trim();
        String domain = (String) spinnerDomain.getSelectedItem();
        String description = etDescription.getText().toString().trim();
        String skills = etSkills.getText().toString().trim();
        String start = etStartDate.getText().toString().trim();
        String end = etEndDate.getText().toString().trim();
        String deadline = etDeadline.getText().toString().trim();
        String positions = etTotalPositions.getText().toString().trim();
        String status = (String) spinnerStatus.getSelectedItem();

        if (!ValidationUtils.notEmpty(title)) {
            etTitle.setError("Title is required.");
            return;
        }
        if (!ValidationUtils.notEmpty(company)) {
            etCompanyName.setError("Company name is required.");
            return;
        }
        if (!ValidationUtils.notEmpty(address)) {
            etCompanyAddress.setError("Company address is required.");
            return;
        }
        if (!ValidationUtils.notEmpty(domain)) {
            toast("Choose a domain.");
            return;
        }
        if (!ValidationUtils.notEmpty(description)) {
            etDescription.setError("Description is required.");
            return;
        }
        if (!ValidationUtils.notEmpty(skills)) {
            etSkills.setError("Required skills are needed.");
            return;
        }
        if (!ValidationUtils.notEmpty(start) || !ValidationUtils.notEmpty(end)
                || !ValidationUtils.notEmpty(deadline)) {
            toast("Choose all three dates.");
            return;
        }
        if (DateUtils.isAfter(end, start)) {
            etEndDate.setError("End date cannot be before the start date.");
            return;
        }
        if (DateUtils.isAfter(deadline, end)) {
            etDeadline.setError("Deadline cannot be after the end date.");
            return;
        }
        if (!ValidationUtils.positiveNumber(positions)) {
            etTotalPositions.setError("Enter at least 1 position.");
            return;
        }
        if (!ValidationUtils.notEmpty(status)) {
            toast("Choose a status.");
            return;
        }

        performWrite(api.updateInternship(internshipId, title, company, address, domain,
                description, skills, start, end, deadline, positions, status), false);
    }

    private void performWrite(Call<ApiResponse<Object>> call, boolean finishOnSuccess) {
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
                if (!body.isSuccess()) return;
                if (finishOnSuccess) {
                    finish();
                } else {
                    loadInternship();
                }
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
        btnSave.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(true);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
