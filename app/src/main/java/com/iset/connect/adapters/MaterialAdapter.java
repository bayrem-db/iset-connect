package com.iset.connect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iset.connect.R;
import com.iset.connect.models.Material;
import com.iset.connect.utils.Constants;
import com.iset.connect.utils.UiUtils;

import java.util.List;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.ViewHolder> {
    public interface OnMaterialClick { void onMaterialClick(Material material); }
    public interface OnMaterialAction {
        void onEdit(Material material);
        void onDelete(Material material);
    }

    private final List<Material> materials;
    private final OnMaterialClick clickListener;
    private final OnMaterialAction actionListener;
    private final boolean adminMode;

    public MaterialAdapter(List<Material> materials, OnMaterialClick clickListener) {
        this(materials, clickListener, null, false);
    }

    public MaterialAdapter(List<Material> materials, OnMaterialClick clickListener,
                           OnMaterialAction actionListener, boolean adminMode) {
        this.materials = materials;
        this.clickListener = clickListener;
        this.actionListener = actionListener;
        this.adminMode = adminMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Material material = materials.get(position);
        holder.name.setText(UiUtils.safe(material.getName()));
        holder.category.setText(UiUtils.safe(material.getCategory()));
        holder.description.setText(UiUtils.safe(material.getDescription()));
        holder.quantity.setText("Available " + material.getAvailableQuantity()
                + " / " + material.getTotalQuantity() + " total");
        UiUtils.applyStatus(holder.status,
                material.getAvailableQuantity() > 0 ? Constants.STATUS_APPROVED : Constants.STATUS_CLOSED);
        if (adminMode) {
            holder.edit.setVisibility(View.VISIBLE);
            holder.delete.setVisibility(View.VISIBLE);
            holder.status.setText("Stock: " + material.getAvailableQuantity());
        } else {
            holder.edit.setVisibility(View.GONE);
            holder.delete.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onMaterialClick(material);
        });
        holder.edit.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onEdit(material);
        });
        holder.delete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(material);
        });
    }

    @Override
    public int getItemCount() { return materials == null ? 0 : materials.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, category, description, quantity, status;
        final ImageButton edit, delete;
        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tv_material_name);
            category = view.findViewById(R.id.tv_material_category);
            description = view.findViewById(R.id.tv_material_description);
            quantity = view.findViewById(R.id.tv_material_quantity);
            status = view.findViewById(R.id.tv_material_status);
            edit = view.findViewById(R.id.btn_edit_material);
            delete = view.findViewById(R.id.btn_delete_material);
        }
    }
}
