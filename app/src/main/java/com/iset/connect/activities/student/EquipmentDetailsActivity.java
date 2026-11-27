package com.iset.connect.activities.student;

import android.app.DatePickerDialog;
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
import com.iset.connect.models.Material;
import com.iset.connect.network.ApiClient;
import com.iset.connect.network.ApiResponse;
import com.iset.connect.network.ApiService;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.DateUtils;
import com.iset.connect.utils.SessionManager;
import com.iset.connect.utils.UiUtils;
import com.iset.connect.utils.ValidationUtils;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Material detail screen with a borrow request form. */
public class EquipmentDetailsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;
    private long materialId;
    private Material material;
    private String borrowingDate = "";
    private String expectedReturnDate = "";

    private TextView tvName, tvCategory, tvDescription, tvQuantity;
    private EditText etQuantity;
    private MaterialButton btnBorrowingDate, btnExpectedReturnDate, btnRequestBorrow;
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
        setContentView(R.layout.activity_equipment_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        materialId = getIntent().getLongExtra(Constants.EXTRA_MATERIAL_ID, -1L);
        if (materialId <= 0) {
            toast("Missing material");
            finish();
            return;
        }

        tvName = findViewById(R.id.tvDetailName);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvQuantity = findViewById(R.id.tvDetailQuantity);
        etQuantity = findViewById(R.id.etQuantity);
        btnBorrowingDate = findViewById(R.id.btnBorrowingDate);
        btnExpectedReturnDate = findViewById(R.id.btnExpectedReturnDate);
        btnRequestBorrow = findViewById(R.id.btnRequestBorrow);
        progressBar = findViewById(R.id.progressBar);

        btnBorrowingDate.setOnClickListener(v -> showDatePicker(true));
        btnExpectedReturnDate.setOnClickListener(v -> showDatePicker(false));
        btnRequestBorrow.setOnClickListener(v -> requestBorrow());

        setupBottomNav(R.id.nav_equipment);
        loadDetails();
    }

    private void loadDetails() {
        progressBar.setVisibility(View.VISIBLE);
        api.materialDetails(materialId).enqueue(new Callback<ApiResponse<ApiData.MaterialData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.MaterialData>> call,
                                   Response<ApiResponse<ApiData.MaterialData>> response) {
                progressBar.setVisibility(View.GONE);
                if (handleUnauthorized(response.code())) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    toast("Server error");
                    return;
                }
                ApiResponse<ApiData.MaterialData> body = response.body();
                if (!body.isSuccess()) {
                    toast(body.getMessage());
                    return;
                }
                if (body.getData() == null || body.getData().getMaterial() == null) {
                    toast("Material not found");
                    return;
                }
                material = body.getData().getMaterial();
                tvName.setText(UiUtils.safe(material.getName()));
                tvCategory.setText(UiUtils.safe(material.getCategory()));
                tvDescription.setText(UiUtils.safe(material.getDescription()));
                tvQuantity.setText("Available " + material.getAvailableQuantity()
                        + " / " + material.getTotalQuantity() + " total");
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.MaterialData>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                toast(getString(R.string.error_network));
            }
        });
    }

    private void showDatePicker(boolean borrowing) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String value = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            if (borrowing) {
                borrowingDate = value;
                btnBorrowingDate.setText(DateUtils.formatDisplay(value));
            } else {
                expectedReturnDate = value;
                btnExpectedReturnDate.setText(DateUtils.formatDisplay(value));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void requestBorrow() {
        String quantityText = etQuantity.getText().toString().trim();
        if (!ValidationUtils.positiveNumber(quantityText)) {
            toast("Enter a positive quantity");
            return;
        }
        if (material == null) {
            toast("Material details are still loading");
            return;
        }
        int quantity = Integer.parseInt(quantityText);
        if (quantity > material.getAvailableQuantity()) {
            toast("Only " + material.getAvailableQuantity() + " units are available");
            return;
        }
        if (borrowingDate.isEmpty() || expectedReturnDate.isEmpty()) {
            toast("Select both borrowing and expected return dates");
            return;
        }
        if (!DateUtils.isAfter(expectedReturnDate, borrowingDate)) {
            toast("Expected return date must be after the borrowing date");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRequestBorrow.setEnabled(false);
        api.createLoan(materialId, String.valueOf(quantity), borrowingDate, expectedReturnDate)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                                           Response<ApiResponse<Object>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnRequestBorrow.setEnabled(true);
                        if (handleUnauthorized(response.code())) {
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            toast("Server error");
                            return;
                        }
                        ApiResponse<Object> body = response.body();
                        toast(body.isSuccess() ? "Borrow request sent" : body.getMessage());
                        if (body.isSuccess()) {
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnRequestBorrow.setEnabled(true);
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
