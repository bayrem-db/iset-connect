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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.iset.connect.R;
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

/** Admin screen: search materials, edit or delete them, and add new ones. */
public class ManageMaterialsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private EditText editSearch;
    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;

    private final List<Material> materials = new ArrayList<>();
    private MaterialAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_materials);

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
        spinnerCategory = findViewById(R.id.spinnerCategory);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnClear = findViewById(R.id.btnClear);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        List<String> options = new ArrayList<>();
        options.add("ALL");
        options.addAll(Arrays.asList(Constants.MATERIAL_CATEGORIES));
        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options));

        adapter = new MaterialAdapter(materials, this::openEdit, new MaterialAdapter.OnMaterialAction() {
            @Override
            public void onEdit(Material material) {
                openEdit(material);
            }

            @Override
            public void onDelete(Material material) {
                confirmDelete(material);
            }
        }, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> loadMaterials());
        btnClear.setOnClickListener(v -> {
            editSearch.setText("");
            spinnerCategory.setSelection(0);
            loadMaterials();
        });
        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddMaterialActivity.class)));

        loadMaterials();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openEdit(Material material) {
        Intent intent = new Intent(this, EditMaterialActivity.class);
        intent.putExtra(Constants.EXTRA_MATERIAL_ID, material.getId());
        startActivity(intent);
    }

    private void confirmDelete(Material material) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete material?")
                .setMessage("Delete \"" + material.getName() + "\"? This cannot be undone.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    showLoading();
                    api.deleteMaterial(material.getId()).enqueue(new Callback<ApiResponse<Object>>() {
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
                            if (body.isSuccess()) loadMaterials();
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            hideLoading();
                            toast(getString(R.string.error_network));
                        }
                    });
                })
                .show();
    }

    private void loadMaterials() {
        String query = editSearch.getText().toString().trim();
        int position = spinnerCategory.getSelectedItemPosition();
        String category = position <= 0 ? null : Constants.MATERIAL_CATEGORIES[position - 1];

        showLoading();
        api.listMaterials(query.isEmpty() ? null : query, category)
                .enqueue(new Callback<ApiResponse<ApiData.MaterialListData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ApiData.MaterialListData>> call,
                                           Response<ApiResponse<ApiData.MaterialListData>> response) {
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
                        ApiResponse<ApiData.MaterialListData> body = response.body();
                        if (!body.isSuccess()) {
                            toast(body.getMessage());
                            return;
                        }
                        ApiData.MaterialListData data = body.getData();
                        materials.clear();
                        if (data != null && data.getMaterials() != null) {
                            materials.addAll(data.getMaterials());
                        }
                        adapter.notifyDataSetChanged();
                        emptyState.setVisibility(materials.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ApiData.MaterialListData>> call, Throwable t) {
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
