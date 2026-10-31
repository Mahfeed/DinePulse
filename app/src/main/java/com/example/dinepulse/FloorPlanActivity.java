package com.example.dinepulse;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FloorPlanActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerFloorPlan;
    private TableAdapter adapter;
    private List<DocumentSnapshot> tableList = new ArrayList<>();
    private final String VENUE_ID = "venue_alpha"; // Bound to current venue

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floor_plan);

        db = FirebaseFirestore.getInstance();

        recyclerFloorPlan = findViewById(R.id.recyclerFloorPlan);
        recyclerFloorPlan.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns
        adapter = new TableAdapter();
        recyclerFloorPlan.setAdapter(adapter);

        findViewById(R.id.fabAddTable).setOnClickListener(v -> showAddTableDialog());

        fetchLiveTables();
    }

    private void fetchLiveTables() {
        db.collection("venues").document(VENUE_ID).collection("tables")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    tableList.clear();
                    tableList.addAll(snapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddTableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Table");

        final EditText etTableName = new EditText(this);
        etTableName.setHint("e.g., Table 08");

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(etTableName);
        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String tableName = etTableName.getText().toString().trim();
            if (tableName.isEmpty()) return;

            String tableId = tableName.toLowerCase().replace(" ", "_");

            Map<String, Object> tableData = new HashMap<>();
            tableData.put("name", tableName);
            tableData.put("status", "Available");
            tableData.put("servicePing", false);

            db.collection("venues").document(VENUE_ID).collection("tables").document(tableId)
                    .set(tableData)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Table Created", Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showQRDialog(String tableId, String tableName) {
        try {
            // Generates the deep-link URL payload for this specific table
            String qrContent = "https://dinepulse.app/r/" + VENUE_ID + "/t/" + tableId;

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 600, 600);

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(20, 20, 20, 20);

            new AlertDialog.Builder(this)
                    .setTitle(tableName + " QR Code")
                    .setMessage("Scan to open menu:\n" + qrContent)
                    .setView(imageView)
                    .setPositiveButton("Done", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class TableAdapter extends RecyclerView.Adapter<TableAdapter.TableViewHolder> {

        @NonNull
        @Override
        public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table, parent, false);
            return new TableViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TableViewHolder holder, int position) {
            DocumentSnapshot doc = tableList.get(position);
            String tableId = doc.getId();
            String name = doc.getString("name");
            String status = doc.getString("status");
            Boolean servicePing = doc.getBoolean("servicePing");

            holder.tvTableName.setText(name != null ? name : tableId);

            // Light Theme Color Logic
            if (servicePing != null && servicePing) {
                holder.tvTableStatus.setText("SERVICE PING");
                holder.tvTableStatus.setTextColor(Color.parseColor("#DC2626")); // Dark Red Text
                holder.layoutTableBg.setBackgroundColor(Color.parseColor("#FEE2E2")); // Light Red Background
            } else if ("Occupied".equalsIgnoreCase(status)) {
                holder.tvTableStatus.setText("Occupied");
                holder.tvTableStatus.setTextColor(Color.parseColor("#6B7280")); // Gray Text
                holder.layoutTableBg.setBackgroundColor(Color.TRANSPARENT); // White Card
            } else {
                holder.tvTableStatus.setText("Available");
                holder.tvTableStatus.setTextColor(Color.parseColor("#10B981")); // Emerald Green Text
                holder.layoutTableBg.setBackgroundColor(Color.TRANSPARENT); // White Card
            }

            // Clear Service Ping
            holder.itemView.setOnClickListener(v -> {
                if (servicePing != null && servicePing) {
                    db.collection("venues").document(VENUE_ID).collection("tables").document(tableId)
                            .update("servicePing", false);
                    Toast.makeText(FloorPlanActivity.this, "Ping Cleared", Toast.LENGTH_SHORT).show();
                }
            });

            holder.btnGenerateQR.setOnClickListener(v -> showQRDialog(tableId, name));

            holder.btnDeleteTable.setOnClickListener(v -> {
                new AlertDialog.Builder(FloorPlanActivity.this)
                        .setTitle("Delete Table")
                        .setMessage("Remove " + name + " from the floor plan?")
                        .setPositiveButton("Delete", (dialog, which) ->
                                db.collection("venues").document(VENUE_ID).collection("tables").document(tableId).delete()
                        )
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return tableList.size();
        }

        class TableViewHolder extends RecyclerView.ViewHolder {
            TextView tvTableName, tvTableStatus;
            RelativeLayout layoutTableBg;
            View btnGenerateQR, btnDeleteTable;

            public TableViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTableName = itemView.findViewById(R.id.tvTableName);
                tvTableStatus = itemView.findViewById(R.id.tvTableStatus);
                layoutTableBg = itemView.findViewById(R.id.layoutTableBg);
                btnGenerateQR = itemView.findViewById(R.id.btnGenerateQR);
                btnDeleteTable = itemView.findViewById(R.id.btnDeleteTable);
            }
        }
    }
}