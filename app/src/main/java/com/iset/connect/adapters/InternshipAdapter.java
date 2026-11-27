package com.iset.connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iset.connect.R;
import com.iset.connect.models.Internship;
import com.iset.connect.utils.DateUtils;
import com.iset.connect.utils.UiUtils;

import java.util.List;

public class InternshipAdapter extends RecyclerView.Adapter<InternshipAdapter.ViewHolder> {
    public interface OnInternshipClick { void onInternshipClick(Internship internship); }

    private final List<Internship> internships;
    private final OnInternshipClick listener;

    public InternshipAdapter(List<Internship> internships, OnInternshipClick listener) {
        this.internships = internships;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_internship, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Internship item = internships.get(position);
        holder.title.setText(UiUtils.safe(item.getTitle()));
        holder.company.setText(UiUtils.safe(item.getCompanyName()));
        holder.domain.setText(UiUtils.safe(item.getDomain()));
        holder.deadline.setText("Apply by " + DateUtils.formatDisplay(item.getApplicationDeadline()));
        holder.positions.setText(item.getAvailablePositions() + " / " + item.getTotalPositions() + " positions");
        UiUtils.applyStatus(holder.status, item.getStatus());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onInternshipClick(item);
        });
    }

    @Override
    public int getItemCount() { return internships == null ? 0 : internships.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, company, domain, deadline, positions, status;
        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tv_internship_title);
            company = view.findViewById(R.id.tv_internship_company);
            domain = view.findViewById(R.id.tv_internship_domain);
            deadline = view.findViewById(R.id.tv_internship_deadline);
            positions = view.findViewById(R.id.tv_internship_positions);
            status = view.findViewById(R.id.tv_internship_status);
        }
    }
}
