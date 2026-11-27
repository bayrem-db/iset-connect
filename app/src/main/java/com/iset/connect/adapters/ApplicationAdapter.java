package com.iset.connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iset.connect.R;
import com.iset.connect.models.Application;
import com.iset.connect.utils.DateUtils;
import com.iset.connect.utils.UiUtils;

import java.util.List;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {
    public interface OnApplicationClick { void onApplicationClick(Application application); }

    private final List<Application> applications;
    private final OnApplicationClick listener;
    private final boolean adminMode;

    public ApplicationAdapter(List<Application> applications, OnApplicationClick listener) {
        this(applications, listener, false);
    }

    public ApplicationAdapter(List<Application> applications, OnApplicationClick listener, boolean adminMode) {
        this.applications = applications;
        this.listener = listener;
        this.adminMode = adminMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Application item = applications.get(position);
        holder.title.setText(UiUtils.safe(item.getInternshipTitle()));
        holder.company.setText(UiUtils.safe(item.getCompanyName()));
        holder.student.setText(adminMode
                ? "Applicant: " + UiUtils.safe(item.getStudentName()) + " • " + UiUtils.safe(item.getClassName())
                : "Applied " + DateUtils.formatDisplay(item.getApplicationDate()));
        holder.message.setText(UiUtils.safe(item.getMotivationMessage()));
        holder.date.setText(DateUtils.formatDateTime(item.getApplicationDate()));
        UiUtils.applyStatus(holder.status, item.getStatus());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onApplicationClick(item);
        });
    }

    @Override
    public int getItemCount() { return applications == null ? 0 : applications.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, company, student, message, date, status;
        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tv_application_title);
            company = view.findViewById(R.id.tv_application_company);
            student = view.findViewById(R.id.tv_application_student);
            message = view.findViewById(R.id.tv_application_message);
            date = view.findViewById(R.id.tv_application_date);
            status = view.findViewById(R.id.tv_application_status);
        }
    }
}
