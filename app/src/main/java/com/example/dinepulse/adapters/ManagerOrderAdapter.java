package com.example.dinepulse.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dinepulse.R;
import com.example.dinepulse.models.Order;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Map;

public class ManagerOrderAdapter extends RecyclerView.Adapter<ManagerOrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnOrderInteractionListener listener;

    public interface OnOrderInteractionListener {
        void onStatusChange(Order order, String newStatus);
        void onOrderCardClick(Order order);
    }

    public ManagerOrderAdapter(List<Order> orderList, OnOrderInteractionListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manager_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvOrderTable.setText(order.getTable() != null ? order.getTable() : "Counter");
        holder.tvOrderTotal.setText(String.format(java.util.Locale.US, "Total: ৳%.2f", order.getTotalAmount()));

        String currentStatus = order.getStatus() != null ? order.getStatus() : "Pending";
        holder.tvOrderStatus.setText(currentStatus.toUpperCase());

        // Apply Pill Badge Styling
        GradientDrawable badgeDrawable = (GradientDrawable) holder.tvOrderStatus.getBackground();
        if (currentStatus.equalsIgnoreCase("Pending")) {
            holder.tvOrderStatus.setTextColor(Color.parseColor("#D97706")); // Dark Amber
            badgeDrawable.setColor(Color.parseColor("#FFFBEB")); // Light Amber fill

            holder.btnUpdateStatus.setText("Accept Order");
            holder.btnUpdateStatus.setBackgroundColor(Color.parseColor("#006AFF")); // Blue
            holder.btnUpdateStatus.setVisibility(View.VISIBLE);
            holder.btnUpdateStatus.setOnClickListener(v -> listener.onStatusChange(order, "Preparing"));

        } else if (currentStatus.equalsIgnoreCase("Preparing")) {
            holder.tvOrderStatus.setTextColor(Color.parseColor("#2563EB")); // Dark Blue
            badgeDrawable.setColor(Color.parseColor("#EFF6FF")); // Light Blue fill

            holder.btnUpdateStatus.setText("Mark as Ready");
            holder.btnUpdateStatus.setBackgroundColor(Color.parseColor("#16A34A")); // Green
            holder.btnUpdateStatus.setVisibility(View.VISIBLE);
            holder.btnUpdateStatus.setOnClickListener(v -> listener.onStatusChange(order, "Ready"));

        } else {
            holder.tvOrderStatus.setTextColor(Color.parseColor("#059669")); // Dark Emerald
            badgeDrawable.setColor(Color.parseColor("#ECFDF5")); // Light Emerald fill

            holder.btnUpdateStatus.setText("Clear Ticket");
            holder.btnUpdateStatus.setBackgroundColor(Color.parseColor("#9CA3AF")); // Gray
            holder.btnUpdateStatus.setVisibility(View.VISIBLE);
            holder.btnUpdateStatus.setOnClickListener(v -> listener.onStatusChange(order, "Completed"));
        }

        // Populate Dynamic Items List
        holder.containerOrderItems.removeAllViews();
        if (order.getItems() != null) {
            for (Map<String, Object> itemMap : order.getItems()) {
                String name = (String) itemMap.get("name");
                Object qtyObj = itemMap.get("quantity");
                long qty = qtyObj instanceof Number ? ((Number) qtyObj).longValue() : 1;

                // Main Item Line
                TextView tvItem = new TextView(context);
                tvItem.setText(qty + "x " + name);
                tvItem.setTextColor(Color.parseColor("#374151"));
                tvItem.setTextSize(16f);
                tvItem.setPadding(0, 0, 0, 4);
                holder.containerOrderItems.addView(tvItem);

                // Placeholder for Modifiers (Assuming modifiers are stored as a string or list in your real data model later)
                // For demonstration, we'll check for a dummy key "modifiers"
                if (itemMap.containsKey("modifiers")) {
                    String modifiersText = (String) itemMap.get("modifiers");
                    TextView tvMods = new TextView(context);
                    tvMods.setText(modifiersText);
                    tvMods.setTextColor(Color.parseColor("#6B7280"));
                    tvMods.setTextSize(14f);
                    tvMods.setPadding(16, 0, 0, 8); // Indent modifiers
                    holder.containerOrderItems.addView(tvMods);
                }
            }
        }

        // Display Overall Customer Notes
        if (order.getCustomerNotes() != null && !order.getCustomerNotes().trim().isEmpty()) {
            holder.tvOrderNotes.setText("Note: " + order.getCustomerNotes());
            holder.tvOrderNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvOrderNotes.setVisibility(View.GONE);
        }

        // Handle Card Click for Detailed Modal
        holder.cardOrderRoot.setOnClickListener(v -> listener.onOrderCardClick(order));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardOrderRoot;
        TextView tvOrderTable, tvOrderStatus, tvOrderTotal, tvOrderNotes;
        LinearLayout containerOrderItems;
        MaterialButton btnUpdateStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardOrderRoot = itemView.findViewById(R.id.cardOrderRoot);
            tvOrderTable = itemView.findViewById(R.id.tvOrderTable);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderNotes = itemView.findViewById(R.id.tvOrderNotes);
            containerOrderItems = itemView.findViewById(R.id.containerOrderItems);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }
    }
}