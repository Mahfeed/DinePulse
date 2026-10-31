package com.example.dinepulse;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dinepulse.models.Order;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerOrderHistory;
    private HistoryAdapter adapter;
    private List<Order> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        recyclerOrderHistory = findViewById(R.id.recyclerOrderHistory);
        recyclerOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerOrderHistory.setAdapter(adapter);

        findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> finish());

        fetchOrderHistory();
    }

    private void fetchOrderHistory() {
        FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("venueId", "venue_alpha")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        historyList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                order.setOrderId(doc.getId());
                                historyList.add(order);
                            }
                        }

                        // Local Sort: Newest orders at the top
                        historyList.sort((o1, o2) -> {
                            long t1 = o1.getTimestamp() instanceof Number ? ((Number) o1.getTimestamp()).longValue() : 0L;
                            long t2 = o2.getTimestamp() instanceof Number ? ((Number) o2.getTimestamp()).longValue() : 0L;
                            return Long.compare(t2, t1);
                        });

                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            Order order = historyList.get(position);

            holder.tvOrderId.setText("Order: " + order.getOrderId());
            holder.tvTotal.setText(String.format(java.util.Locale.US, "৳%.2f", order.getTotalAmount()));

            String status = order.getStatus() != null ? order.getStatus() : "Unknown";
            holder.tvStatus.setText(status.toUpperCase());

            if (status.equalsIgnoreCase("Completed")) {
                holder.tvStatus.setTextColor(Color.parseColor("#16A34A"));
            } else if (status.equalsIgnoreCase("Pending")) {
                holder.tvStatus.setTextColor(Color.parseColor("#DC2626"));
            } else {
                holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
            }

            holder.btnViewInvoice.setOnClickListener(v -> {
                // FIX: Point directly to the new AdminReceiptActivity
                Intent intent = new Intent(OrderHistoryActivity.this, AdminReceiptActivity.class);
                intent.putExtra("ORDER_ID", order.getOrderId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvStatus, tvTotal;
            MaterialButton btnViewInvoice;

            public HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvHistoryOrderId);
                tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
                tvTotal = itemView.findViewById(R.id.tvHistoryTotal);
                btnViewInvoice = itemView.findViewById(R.id.btnViewInvoice);
            }
        }
    }
}