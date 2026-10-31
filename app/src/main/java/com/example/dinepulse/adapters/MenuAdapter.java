package com.example.dinepulse.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.example.dinepulse.R;
import com.example.dinepulse.models.MenuItem;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

    public interface OnMenuItemClickListener {
        void onActionClicked(MenuItem item);
    }

    private List<MenuItem> menuItems;
    private FragmentManager fragmentManager;
    private OnMenuItemClickListener listener;

    // A high-quality default image for items created before we added the URL feature
    private static final String FALLBACK_IMAGE_URL = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&q=80";

    public MenuAdapter(List<MenuItem> menuItems, FragmentManager fragmentManager, OnMenuItemClickListener listener) {
        this.menuItems = menuItems;
        this.fragmentManager = fragmentManager;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem currentItem = menuItems.get(position);

        holder.tvItemName.setText(currentItem.getName() != null ? currentItem.getName() : "Unknown Item");

        if (currentItem.getDescription() != null && !currentItem.getDescription().isEmpty()) {
            holder.tvItemDescription.setText(currentItem.getDescription());
            holder.tvItemDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvItemDescription.setVisibility(View.GONE);
        }

        holder.tvItemPrice.setText(String.format("৳%.2f", currentItem.getPrice()));

        // Determine which URL to use
        String urlToLoad = FALLBACK_IMAGE_URL;
        if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().trim().isEmpty()) {
            urlToLoad = currentItem.getImageUrl().trim();
        }

        // Load image using Glide with CenterCrop
        Glide.with(holder.itemView.getContext())
                .load(urlToLoad)
                .transform(new CenterCrop())
                .into(holder.imgFoodItem);

        holder.btnAddCart.setOnClickListener(view -> {
            if (listener != null && currentItem != null &&
                    (currentItem.getId() != null || currentItem.getName() != null)) {
                listener.onActionClicked(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuItems == null ? 0 : menuItems.size();
    }

    public void updateList(List<MenuItem> newList) {
        this.menuItems = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoodItem;
        TextView tvItemName;
        TextView tvItemDescription;
        TextView tvItemPrice;
        MaterialButton btnAddCart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoodItem = itemView.findViewById(R.id.imgFoodItem);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemDescription = itemView.findViewById(R.id.tvItemDescription);
            tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
            btnAddCart = itemView.findViewById(R.id.btnAddCart);
        }
    }
}