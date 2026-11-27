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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.iset.connect.R;
import com.iset.connect.adapters.InternshipAdapter;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.Internship;
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

/** Admin screen: search/filter internships and open the edit screen. */
public class ManageInternshipsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private EditText editSearch;
    private Spinner spinnerDomain;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;

    private final List<Internship> internships = new ArrayList<>();
    private InternshipAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_internships);

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
        spinnerDomain = findViewById(R.id.spinnerDomain);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnClear = findViewById(R.id.btnClear);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        List<String> domains = new ArrayList<>();
        domains.add("ALL");
        domains.addAll(Arrays.asList(Constants.INTERNSHIP_DOMAINS));
        spinnerDomain.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, domains));

        adapter = new InternshipAdapter(internships, internship -> {
            Intent intent = new Intent(this, EditInternshipActivity.class);
            intent.putExtra(Constants.EXTRA_INTERNSHIP_ID, internship.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> loadInternships());
        btnClear.setOnClickListener(v -> {
            editSearch.setText("");
            spinnerDomain.setSelection(0);
            loadInternships();
        });
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddInternshipActivity.class)));

        loadInternships();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadInternships() {
        String query = editSearch.getText().toString().trim();
        int position = spinnerDomain.getSelectedItemPosition();
        String domain = position <= 0 ? null : Constants.INTERNSHIP_DOMAINS[position - 1];

        showLoading();
        api.listInternships(query.isEmpty() ? null : query, domain, null)
                .enqueue(new Callback<ApiResponse<ApiData.InternshipListData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ApiData.InternshipListData>> call,
                                           Response<ApiResponse<ApiData.InternshipListData>> response) {
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
                        ApiResponse<ApiData.InternshipListData> body = response.body();
                        if (!body.isSuccess()) {
                            toast(body.getMessage());
                            return;
                        }
                        ApiData.InternshipListData data = body.getData();
                        internships.clear();
                        if (data != null && data.getInternships() != null) {
                            internships.addAll(data.getInternships());
                        }
                        adapter.notifyDataSetChanged();
                        emptyState.setVisibility(internships.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApiData.InternshipListData>> call, Throwable t) {
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
