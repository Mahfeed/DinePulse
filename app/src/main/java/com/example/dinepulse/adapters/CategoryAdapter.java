package com.example.dinepulse.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dinepulse.R;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<String> categories;
    private int selectedPosition = 0; // "All" is selected by default

    public CategoryAdapter(List<String> categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_pill, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        holder.tvCategoryName.setText(category);

        if (position == selectedPosition) {
            holder.categoryCard.setCardBackgroundColor(Color.parseColor("#111827")); // Dark Gray/Black
            holder.tvCategoryName.setTextColor(Color.parseColor("#FFFFFF")); // White text
            holder.categoryCard.setStrokeWidth(0);
        } else {
            holder.categoryCard.setCardBackgroundColor(Color.parseColor("#FFFFFF")); // White
            holder.tvCategoryName.setTextColor(Color.parseColor("#6B7280")); // Gray text
            holder.categoryCard.setStrokeWidth(2); // 1dp stroke in pixel equivalent
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            // In a full build, this is where you would filter your InventoryRepository list
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView categoryCard;
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryCard = itemView.findViewById(R.id.categoryCard);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}