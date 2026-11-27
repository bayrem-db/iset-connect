package com.iset.connect.activities.student;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.iset.connect.R;
import com.iset.connect.activities.LoginActivity;
import com.iset.connect.activities.NotificationsActivity;
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

/** Published internships available to students. */
public class InternshipListActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;
    private final List<Internship> internships = new ArrayList<>();
    private InternshipAdapter adapter;
    private Call<ApiResponse<ApiData.InternshipListData>> currentCall;

    private EditText searchEditText;
    private Spinner domainSpinner;
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
        setContentView(R.layout.activity_internship_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        searchEditText = findViewById(R.id.searchEditText);
        domainSpinner = findViewById(R.id.domainSpinner);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        adapter = new InternshipAdapter(internships, internship -> {
            Intent intent = new Intent(this, InternshipDetailsActivity.class);
            intent.putExtra(Constants.EXTRA_INTERNSHIP_ID, internship.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        List<String> domains = new ArrayList<>();
        domains.add("ALL");
        domains.addAll(Arrays.asList(Constants.INTERNSHIP_DOMAINS));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, domains);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        domainSpinner.setAdapter(spinnerAdapter);
        domainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op.
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                loadData();
            }
        });

        setupBottomNav(R.id.nav_internships);
    }

    private void loadData() {
        String query = searchEditText.getText().toString().trim();
        String domain = (String) domainSpinner.getSelectedItem();
        if ("ALL".equals(domain)) {
            domain = null;
        }
        if (currentCall != null) {
            currentCall.cancel();
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        currentCall = api.listInternships(query.isEmpty() ? null : query, domain, null);
        currentCall.enqueue(new Callback<ApiResponse<ApiData.InternshipListData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.InternshipListData>> call,
                                   Response<ApiResponse<ApiData.InternshipListData>> response) {
                progressBar.setVisibility(View.GONE);
                if (handleUnauthorized(response.code())) {
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
                internships.clear();
                if (body.getData() != null && body.getData().getInternships() != null) {
                    internships.addAll(body.getData().getInternships());
                }
                adapter.notifyDataSetChanged();
                emptyState.setVisibility(internships.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.InternshipListData>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                if (call.isCanceled()) {
                    return;
                }
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
