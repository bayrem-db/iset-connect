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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.iset.connect.R;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.ValidationUtils;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Admin screen: create a material through the real API. */
public class AddMaterialActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private EditText etName;
    private EditText etDescription;
    private Spinner spinnerCategory;
    private EditText etQuantity;
    private EditText etImageUrl;
    private ProgressBar progressBar;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_material);

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

        etName = findViewById(R.id.etMaterialName);
        etDescription = findViewById(R.id.etMaterialDescription);
        spinnerCategory = findViewById(R.id.spinnerMaterialCategory);
        etQuantity = findViewById(R.id.etTotalQuantity);
        etImageUrl = findViewById(R.id.etImageUrl);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSaveMaterial);

        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Constants.MATERIAL_CATEGORIES));

        btnSave.setOnClickListener(v -> saveMaterial());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveMaterial() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = (String) spinnerCategory.getSelectedItem();
        String quantity = etQuantity.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();

        if (!ValidationUtils.notEmpty(name)) {
            etName.setError("Name is required.");
            return;
        }
        if (!ValidationUtils.notEmpty(description)) {
            etDescription.setError("Description is required.");
            return;
        }
        if (category == null || !Arrays.asList(Constants.MATERIAL_CATEGORIES).contains(category)) {
            toast("Choose a category.");
            return;
        }
        if (!ValidationUtils.positiveNumber(quantity)) {
            etQuantity.setError("Enter a quantity of at least 1.");
            return;
        }

        showLoading();
        api.createMaterial(name, description, category, quantity, imageUrl)
                .enqueue(new Callback<ApiResponse<Object>>() {
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
                        if (body.isSuccess()) finish();
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
