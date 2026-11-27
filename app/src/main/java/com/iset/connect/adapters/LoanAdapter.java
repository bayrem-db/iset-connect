package com.iset.connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iset.connect.R;
import com.iset.connect.models.Loan;
import com.iset.connect.utils.DateUtils;
import com.iset.connect.utils.UiUtils;

import java.util.List;

public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.ViewHolder> {
    public interface OnLoanClick { void onLoanClick(Loan loan); }

    private final List<Loan> loans;
    private final OnLoanClick listener;
    private final boolean adminMode;

    public LoanAdapter(List<Loan> loans, OnLoanClick listener) {
        this(loans, listener, false);
    }

    public LoanAdapter(List<Loan> loans, OnLoanClick listener, boolean adminMode) {
        this.loans = loans;
        this.listener = listener;
        this.adminMode = adminMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Loan loan = loans.get(position);
        holder.material.setText(UiUtils.safe(loan.getMaterialName()));
        holder.student.setText(adminMode
                ? "Student: " + UiUtils.safe(loan.getStudentName())
                : "Category: " + UiUtils.safe(loan.getCategory()));
        holder.quantity.setText("Quantity: " + loan.getQuantity()
                + "  •  Requested " + DateUtils.formatDisplay(loan.getRequestDate()));
        holder.dates.setText(DateUtils.formatDisplay(loan.getBorrowingDate()) + " → "
                + DateUtils.formatDisplay(loan.getExpectedReturnDate()));
        UiUtils.applyStatus(holder.status, loan.getStatus());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLoanClick(loan);
        });
    }

    @Override
    public int getItemCount() { return loans == null ? 0 : loans.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView material, student, quantity, dates, status;
        ViewHolder(View view) {
            super(view);
            material = view.findViewById(R.id.tv_loan_material);
            student = view.findViewById(R.id.tv_loan_student);
            quantity = view.findViewById(R.id.tv_loan_quantity);
            dates = view.findViewById(R.id.tv_loan_dates);
            status = view.findViewById(R.id.tv_loan_status);
        }
    }
}
