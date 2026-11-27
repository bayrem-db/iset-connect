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
import com.iset.connect.adapters.MaterialAdapter;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.Material;
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

/** Student catalogue of borrowable equipment. */
public class EquipmentListActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;
    private final List<Material> materials = new ArrayList<>();
    private MaterialAdapter adapter;
    private Call<ApiResponse<ApiData.MaterialListData>> currentCall;

    private EditText searchEditText;
    private Spinner categorySpinner;
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
        setContentView(R.layout.activity_equipment_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        searchEditText = findViewById(R.id.searchEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        adapter = new MaterialAdapter(materials, material -> {
            Intent intent = new Intent(this, EquipmentDetailsActivity.class);
            intent.putExtra(Constants.EXTRA_MATERIAL_ID, material.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        List<String> categories = new ArrayList<>();
        categories.add("ALL");
        categories.addAll(Arrays.asList(Constants.MATERIAL_CATEGORIES));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

        setupBottomNav(R.id.nav_equipment);
    }

    private void loadData() {
        String query = searchEditText.getText().toString().trim();
        String category = (String) categorySpinner.getSelectedItem();
        if ("ALL".equals(category)) {
            category = null;
        }
        if (currentCall != null) {
            currentCall.cancel();
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        currentCall = api.listMaterials(query.isEmpty() ? null : query, category);
        currentCall.enqueue(new Callback<ApiResponse<ApiData.MaterialListData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.MaterialListData>> call,
                                   Response<ApiResponse<ApiData.MaterialListData>> response) {
                progressBar.setVisibility(View.GONE);
                if (handleUnauthorized(response.code())) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    toast("Server error");
                    return;
                }
                ApiResponse<ApiData.MaterialListData> body = response.body();
                if (!body.isSuccess()) {
                    toast(body.getMessage());
                    return;
                }
                materials.clear();
                if (body.getData() != null && body.getData().getMaterials() != null) {
                    materials.addAll(body.getData().getMaterials());
                }
                adapter.notifyDataSetChanged();
                emptyState.setVisibility(materials.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.MaterialListData>> call, Throwable t) {
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
