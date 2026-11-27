package com.iset.connect.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iset.connect.R;
import com.iset.connect.adapters.LoanAdapter;
import com.iset.connect.models.ApiData;
import com.iset.connect.models.Loan;
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

/** Admin screen: review borrow requests, approve/reject them and register returns. */
public class BorrowRequestsActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService api;

    private Spinner spinnerStatus;
    private ToggleButton toggleHistory;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;

    private final List<Loan> loans = new ArrayList<>();
    private LoanAdapter adapter;
    private boolean history;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_borrow_requests);

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

        spinnerStatus = findViewById(R.id.spinnerLoanStatus);
        toggleHistory = findViewById(R.id.toggleHistory);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);

        List<String> statuses = new ArrayList<>();
        statuses.add("ALL");
        statuses.addAll(Arrays.asList(Constants.LOAN_STATUSES));
        spinnerStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses));

        adapter = new LoanAdapter(loans, this::showLoanDialog, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        toggleHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
            history = isChecked;
            spinnerStatus.setEnabled(!isChecked);
            loadLoans();
        });

        loadLoans();

        spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                if (!history) loadLoans();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No action needed.
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadLoans() {
        int position = spinnerStatus.getSelectedItemPosition();
        String status = (history || position <= 0) ? null : Constants.LOAN_STATUSES[position - 1];

        showLoading();
        Call<ApiResponse<ApiData.LoanListData>> call = history
                ? api.listLoans(null, null, "history")
                : api.listLoans(null, status, null);

        call.enqueue(new Callback<ApiResponse<ApiData.LoanListData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ApiData.LoanListData>> call,
                                   Response<ApiResponse<ApiData.LoanListData>> response) {
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
                ApiResponse<ApiData.LoanListData> body = response.body();
                if (!body.isSuccess()) {
                    toast(body.getMessage());
                    return;
                }
                ApiData.LoanListData data = body.getData();
                loans.clear();
                if (data != null && data.getLoans() != null) {
                    loans.addAll(data.getLoans());
                }
                adapter.notifyDataSetChanged();
                emptyState.setVisibility(loans.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<ApiResponse<ApiData.LoanListData>> call, Throwable t) {
                hideLoading();
                toast(getString(R.string.error_network));
            }
        });
    }

    private void showLoanDialog(Loan loan) {
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                getResources().getDisplayMetrics());

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, padding / 2, padding, padding / 2);

        TextView details = new TextView(this);
        details.setText("Student: " + UiUtils.safe(loan.getStudentName())
                + "\nMaterial: " + UiUtils.safe(loan.getMaterialName())
                + "\nQuantity: " + loan.getQuantity()
                + "\nRequested: " + DateUtils.formatDisplay(loan.getRequestDate())
                + "\nBorrowing date: " + DateUtils.formatDisplay(loan.getBorrowingDate())
                + "\nExpected return: " + DateUtils.formatDisplay(loan.getExpectedReturnDate())
                + "\nActual return: " + DateUtils.formatDisplay(loan.getActualReturnDate())
                + "\nStatus: " + UiUtils.safe(loan.getStatus()));
        details.setTextColor(getResources().getColor(R.color.text_main));
        container.addView(details, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, padding / 2, 0, 0);

        Button btnApprove = createActionButton(R.string.approve);
        Button btnReject = createActionButton(R.string.reject);
        Button btnReturn = createActionButton(R.string.return_item);

        boolean pending = Constants.STATUS_PENDING.equals(loan.getStatus());
        boolean approved = Constants.STATUS_APPROVED.equals(loan.getStatus());
        btnApprove.setVisibility(pending ? View.VISIBLE : View.GONE);
        btnReject.setVisibility(pending ? View.VISIBLE : View.GONE);
        btnReturn.setVisibility(approved ? View.VISIBLE : View.GONE);

        final androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Borrow request")
                .setView(container)
                .setNegativeButton("Close", null)
                .create();

        btnApprove.setOnClickListener(v -> {
            dialog.dismiss();
            performWrite(api.approveLoan(loan.getId()));
        });
        btnReject.setOnClickListener(v -> {
            dialog.dismiss();
            askReason("Reject borrow request", reason ->
                    performWrite(api.rejectLoan(loan.getId(), reason)));
        });
        btnReturn.setOnClickListener(v -> {
            dialog.dismiss();
            performWrite(api.returnLoan(loan.getId()));
        });

        actions.addView(btnApprove, 0, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(btnReject, 1, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(btnReturn, 2, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        container.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        dialog.show();
    }

    private Button createActionButton(int labelRes) {
        Button button = new Button(this);
        button.setText(labelRes);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        return button;
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
                if (body.isSuccess()) loadLoans();
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
