package com.example.dinepulse;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dinepulse.adapters.ManagerOrderAdapter;
import com.example.dinepulse.models.Order;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ManagerDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private AdminMenuAdapter menuAdapter;
    private ManagerOrderAdapter orderAdapter;

    private List<DocumentSnapshot> adminMenuItems = new ArrayList<>();
    private List<Order> liveOrderList = new ArrayList<>();

    // Variables for Image Upload
    private Uri selectedImageUri;
    private ImageView imgPreview;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        db = FirebaseFirestore.getInstance();

        // Register the Image Picker Launcher
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && imgPreview != null) {
                selectedImageUri = uri;
                imgPreview.setImageURI(uri);
            }
        });

        RecyclerView recyclerManagerMenu = findViewById(R.id.recyclerManagerMenu);
        recyclerManagerMenu.setLayoutManager(new LinearLayoutManager(this));
        menuAdapter = new AdminMenuAdapter();
        recyclerManagerMenu.setAdapter(menuAdapter);

        RecyclerView recyclerManagerOrders = findViewById(R.id.recyclerManagerOrders);
        recyclerManagerOrders.setLayoutManager(new LinearLayoutManager(this));

        orderAdapter = new ManagerOrderAdapter(liveOrderList, new ManagerOrderAdapter.OnOrderInteractionListener() {
            @Override
            public void onStatusChange(Order order, String newStatus) {
                updateOrderStatus(order, newStatus);
            }

            @Override
            public void onOrderCardClick(Order order) {
                showOrderDetailsModal(order);
            }
        });
        recyclerManagerOrders.setAdapter(orderAdapter);

        fetchLiveAdminMenu();
        fetchLiveOrders();
        fetchLiveTablePings();

        findViewById(R.id.fabAddItem).setOnClickListener(v -> showAddItemBottomSheet());
        findViewById(R.id.btnSettings).setOnClickListener(v -> showNavBottomSheet());
    }

    private void showNavBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_admin_nav_menu, null);
        bottomSheet.setContentView(view);

        view.findViewById(R.id.navFloorPlan).setOnClickListener(v -> {
            startActivity(new Intent(this, FloorPlanActivity.class));
            bottomSheet.dismiss();
        });
        view.findViewById(R.id.navOrderHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, OrderHistoryActivity.class));
            bottomSheet.dismiss();
        });
        view.findViewById(R.id.navVenueSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, VenueSettingsActivity.class));
            bottomSheet.dismiss();
        });
        view.findViewById(R.id.navLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        bottomSheet.show();
    }

    private void showAddItemBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        bottomSheet.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        View view = getLayoutInflater().inflate(R.layout.dialog_add_item_bottom_sheet, null);
        bottomSheet.setContentView(view);

        selectedImageUri = null; // reset
        imgPreview = view.findViewById(R.id.imgPreview);
        View cardImageUpload = view.findViewById(R.id.cardImageUpload);

        TextInputEditText inputName = view.findViewById(R.id.inputName);
        TextInputEditText inputPrice = view.findViewById(R.id.inputPrice);
        TextInputEditText inputCategory = view.findViewById(R.id.inputCategory);
        MaterialButton btnSaveItem = view.findViewById(R.id.btnSaveItem);

        cardImageUpload.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSaveItem.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String priceStr = inputPrice.getText().toString().trim();
            String category = inputCategory.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Name, Price, and Category required", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSaveItem.setEnabled(false);
            btnSaveItem.setText("Uploading...");

            double price = Double.parseDouble(priceStr);

            if (selectedImageUri != null) {
                // Upload to Firebase Storage
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("menu_images/" + UUID.randomUUID().toString());
                storageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                saveItemToFirestore(name, price, category, uri.toString(), bottomSheet);
                            });
                        })
                        .addOnFailureListener(e -> {
                            btnSaveItem.setEnabled(true);
                            btnSaveItem.setText("Upload & Save");
                            Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Save without an image
                saveItemToFirestore(name, price, category, "", bottomSheet);
            }
        });

        bottomSheet.show();
    }

    private void saveItemToFirestore(String name, double price, String category, String imageUrl, BottomSheetDialog dialog) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("price", price);
        item.put("category", category);
        item.put("available", true);
        item.put("imageUrl", imageUrl);

        db.collection("menu").add(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item saved!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving item", Toast.LENGTH_SHORT).show());
    }

    private void showOrderDetailsModal(Order order) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_order_details, null);
        bottomSheet.setContentView(view);

        TextView tvTable = view.findViewById(R.id.tvModalTable);
        TextView tvOrderId = view.findViewById(R.id.tvModalOrderId);
        LinearLayout containerItems = view.findViewById(R.id.containerModalItems);
        TextView tvTotal = view.findViewById(R.id.tvModalTotal);

        view.findViewById(R.id.btnCloseModal).setOnClickListener(v -> bottomSheet.dismiss());

        tvTable.setText(order.getTable() != null ? order.getTable().toUpperCase() : "COUNTER");
        tvOrderId.setText("Order ID: " + order.getOrderId());
        tvTotal.setText(String.format(Locale.US, "Total: ৳%.2f", order.getTotalAmount()));

        if (order.getItems() != null) {
            for (Map<String, Object> itemMap : order.getItems()) {
                String name = (String) itemMap.get("name");
                Object qtyObj = itemMap.get("quantity");
                long qty = qtyObj instanceof Number ? ((Number) qtyObj).longValue() : 1;

                TextView tvItem = new TextView(this);
                tvItem.setText(qty + "x " + name);
                tvItem.setTextColor(Color.parseColor("#374151"));
                tvItem.setTextSize(16f);
                tvItem.setPadding(0, 8, 0, 8);
                containerItems.addView(tvItem);
            }
        }

        if (order.getCustomerNotes() != null && !order.getCustomerNotes().trim().isEmpty()) {
            TextView tvNotes = new TextView(this);
            tvNotes.setText("Special Instructions:\n" + order.getCustomerNotes());
            tvNotes.setTextColor(Color.parseColor("#D97706"));
            tvNotes.setTextSize(14f);
            tvNotes.setTypeface(null, Typeface.BOLD_ITALIC);
            tvNotes.setPadding(0, 16, 0, 8);
            containerItems.addView(tvNotes);
        }

        bottomSheet.show();
    }

    private void fetchLiveAdminMenu() {
        db.collection("menu").addSnapshotListener((snapshots, e) -> {
            if (e != null) return;
            if (snapshots != null) {
                adminMenuItems.clear();
                adminMenuItems.addAll(snapshots.getDocuments());
                menuAdapter.notifyDataSetChanged();
            }
        });
    }

    private void fetchLiveTablePings() {
        db.collection("venues").document("venue_alpha")
                .collection("tables")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Boolean servicePing = doc.getBoolean("servicePing");
                        String tableId = doc.getId();
                        if (servicePing != null && servicePing) {
                            Toast.makeText(this, "🚨 Alert: " + tableId.toUpperCase() + " is calling a server!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void fetchLiveOrders() {
        db.collection("orders")
                .whereEqualTo("venueId", "venue_alpha")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        liveOrderList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null && !"Completed".equalsIgnoreCase(order.getStatus())) {
                                order.setOrderId(doc.getId());
                                liveOrderList.add(order);
                            }
                        }
                        if (orderAdapter != null) {
                            orderAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getOrderId() == null) return;
        db.collection("orders").document(order.getOrderId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Order updated to " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show());
    }

    private class AdminMenuAdapter extends RecyclerView.Adapter<AdminMenuAdapter.AdminViewHolder> {

        private static final String FALLBACK_IMAGE_URL = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&q=80";

        @NonNull
        @Override
        public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_menu, parent, false);
            return new AdminViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
            DocumentSnapshot doc = adminMenuItems.get(position);
            String docId = doc.getId();

            String name = doc.getString("name");
            String cat = doc.getString("category");
            Double price = doc.getDouble("price");
            Boolean isAvailable = doc.getBoolean("available");
            String imageUrl = doc.getString("imageUrl");

            holder.tvName.setText(name != null ? name : "Unknown");
            holder.tvCategory.setText(cat != null ? cat : "No Category");
            holder.tvPrice.setText(price != null ? String.format(Locale.US, "৳%.2f", price) : "৳0.00");

            holder.switchAvailable.setOnCheckedChangeListener(null);
            holder.switchAvailable.setChecked(isAvailable != null && isAvailable);

            String urlToLoad = (imageUrl != null && !imageUrl.trim().isEmpty()) ? imageUrl.trim() : FALLBACK_IMAGE_URL;
            Glide.with(holder.itemView.getContext()).load(urlToLoad).centerCrop().into(holder.imgAdminItem);

            holder.switchAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                db.collection("menu").document(docId).update("available", isChecked)
                        .addOnFailureListener(e -> {
                            Toast.makeText(ManagerDashboardActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                            holder.switchAvailable.setChecked(!isChecked);
                        });
            });

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ManagerDashboardActivity.this)
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to permanently delete " + name + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.collection("menu").document(docId).delete();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return adminMenuItems.size(); }

        class AdminViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory, tvPrice, btnDelete;
            MaterialSwitch switchAvailable ;
            ImageView imgAdminItem;

            public AdminViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvAdminItemName);
                tvCategory = itemView.findViewById(R.id.tvAdminItemCategory);
                tvPrice = itemView.findViewById(R.id.tvAdminItemPrice);
                switchAvailable = itemView.findViewById(R.id.switchAvailable);
                btnDelete = itemView.findViewById(R.id.btnDeleteAdminItem);
                imgAdminItem = itemView.findViewById(R.id.imgAdminItem);
            }
        }
    }
}