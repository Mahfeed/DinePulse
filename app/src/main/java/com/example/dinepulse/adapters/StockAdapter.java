package com.example.dinepulse.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dinepulse.R;
import com.example.dinepulse.models.MenuItem;
import com.example.dinepulse.repository.InventoryRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {

    private List<MenuItem> menuList;

    public StockAdapter(List<MenuItem> menuList) {
        this.menuList = menuList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_toggle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = menuList.get(position);

        holder.tvManagerItemName.setText(item.getName());
        holder.tvManagerCategory.setText(item.getCategory() + " • " + String.format("৳%.2f", item.getPrice()));

        // 1. Clear listener to prevent recursive trigger loops during list scrolling
        holder.switchStock.setOnCheckedChangeListener(null);

        // 2. Set current state using the correct getter
        holder.switchStock.setChecked(item.isAvailable());

        // 3. Re-attach listener and use the correct setter
        holder.switchStock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setAvailable(isChecked);
            InventoryRepository.getInstance().updateStock(item.getId(), isChecked);
        });

    }

    @Override
    public int getItemCount() {
        return menuList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvManagerItemName, tvManagerCategory;
        SwitchCompat switchStock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvManagerItemName = itemView.findViewById(R.id.tvManagerItemName);
            tvManagerCategory = itemView.findViewById(R.id.tvManagerCategory);
            switchStock = itemView.findViewById(R.id.switchStock);
        }
    }
}