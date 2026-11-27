package com.iset.connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iset.connect.R;
import com.iset.connect.models.User;
import com.iset.connect.utils.UiUtils;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {
    public interface OnStudentClick { void onStudentClick(User student); }

    private final List<User> students;
    private final OnStudentClick listener;

    public StudentAdapter(List<User> students, OnStudentClick listener) {
        this.students = students;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User student = students.get(position);
        holder.name.setText(UiUtils.safe(student.getFullName()));
        holder.cin.setText("CIN: " + UiUtils.safe(student.getCin()));
        holder.email.setText(UiUtils.safe(student.getEmail()));
        holder.className.setText("Class: " + UiUtils.safe(student.getClassName()));
        UiUtils.applyStatus(holder.status, student.getStatus());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onStudentClick(student);
        });
    }

    @Override
    public int getItemCount() { return students == null ? 0 : students.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, cin, email, className, status;
        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tv_student_name);
            cin = view.findViewById(R.id.tv_student_cin);
            email = view.findViewById(R.id.tv_student_email);
            className = view.findViewById(R.id.tv_student_class);
            status = view.findViewById(R.id.tv_student_status);
        }
    }
}
