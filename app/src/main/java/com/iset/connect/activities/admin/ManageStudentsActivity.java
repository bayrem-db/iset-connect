package com.iset.connect.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.iset.connect.R;
import com.iset.connect.adapters.StudentAdapter;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.User;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Admin screen: search and filter student accounts, then open a student's details. */
public class ManageStudentsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private EditText editSearch;
    private Spinner spinnerStatus;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;

    private final List<User> students = new ArrayList<>();
    private StudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

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

        List<String> options = new ArrayList<>();
        options.add("ALL");
        options.addAll(Arrays.asList(Constants.ACCOUNT_STATUSES));
        spinnerStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options));

        adapter = new StudentAdapter(students, student -> {
            Intent intent = new Intent(this, StudentDetailsActivity.class);
            intent.putExtra(Constants.EXTRA_STUDENT_ID, student.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> loadStudents());
        btnClear.setOnClickListener(v -> {
            editSearch.setText("");
            spinnerStatus.setSelection(0);
            loadStudents();
        });

        loadStudents();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadStudents() {
        String query = editSearch.getText().toString().trim();
        int position = spinnerStatus.getSelectedItemPosition();
        String status = position <= 0 ? null : Constants.ACCOUNT_STATUSES[position - 1];

        showLoading();
        api.listStudents(query.isEmpty() ? null : query, status)
                .enqueue(new Callback<ApiResponse<ApiData.StudentListData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ApiData.StudentListData>> call,
                                           Response<ApiResponse<ApiData.StudentListData>> response) {
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
                        ApiResponse<ApiData.StudentListData> body = response.body();
                        if (!body.isSuccess()) {
                            toast(body.getMessage());
                            return;
                        }
                        ApiData.StudentListData data = body.getData();
                        students.clear();
                        if (data != null && data.getStudents() != null) {
                            students.addAll(data.getStudents());
                        }
                        adapter.notifyDataSetChanged();
                        emptyState.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApiData.StudentListData>> call, Throwable t) {
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
