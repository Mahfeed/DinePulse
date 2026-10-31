package com.example.dinepulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dinepulse.adapters.MenuAdapter;
import com.example.dinepulse.models.MenuItem;
import com.example.dinepulse.repository.CartManager;
import com.example.dinepulse.repository.InventoryRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuestMenuActivity extends AppCompatActivity implements MenuAdapter.OnMenuItemClickListener {

    private RecyclerView recyclerView;
    private MenuAdapter adapter;
    private ChipGroup chipGroupCategories;
    private ExtendedFloatingActionButton btnCallServer, fabViewCart;
    private ImageView btnLogout;
    private TextView tvVenueName, tvActiveOrderText;
    private MaterialCardView cardActiveOrder;

    private List<MenuItem> fullMenuList = new ArrayList<>();

    private String currentVenueId;
    private String currentTableId;
    private String uniqueSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_menu);

        recyclerView = findViewById(R.id.recyclerViewGuestMenu);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        btnCallServer = findViewById(R.id.btnCallServer);
        fabViewCart = findViewById(R.id.fabViewCart);
        btnLogout = findViewById(R.id.btnLogout);
        tvVenueName = findViewById(R.id.tvVenueName);

        // Active Order UI
        cardActiveOrder = findViewById(R.id.cardActiveOrder);
        tvActiveOrderText = findViewById(R.id.tvActiveOrderText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        initializeGuestSession();

        InventoryRepository.getInstance().getMenuLiveData().observe(this, items -> {
            if (items != null) {
                fullMenuList.clear();
                for (MenuItem item : items) {
                    if (item.isAvailable()) {
                        fullMenuList.add(item);
                    }
                }
                setupCategories();
                if (adapter == null) {
                    adapter = new MenuAdapter(fullMenuList, getSupportFragmentManager(), this);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.updateList(fullMenuList);
                }
            }
        });

        fabViewCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        btnCallServer.setOnClickListener(v -> {
            if (currentVenueId != null && currentTableId != null) {
                java.util.Map<String, Object> pingData = new java.util.HashMap<>();
                pingData.put("servicePing", true);
                pingData.put("pingSessionId", uniqueSessionId);
                pingData.put("status", "Occupied");

                FirebaseFirestore.getInstance()
                        .collection("venues").document(currentVenueId)
                        .collection("tables").document(currentTableId)
                        .set(pingData, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "A server is on the way!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("DinePulsePrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                FirebaseAuth.getInstance().signOut();
            }

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkActiveOrderStatus();
    }

    private void checkActiveOrderStatus() {
        SharedPreferences prefs = getSharedPreferences("DinePulsePrefs", MODE_PRIVATE);
        String lastOrderId = prefs.getString("LAST_ORDER_ID", "");

        if (lastOrderId.isEmpty()) {
            cardActiveOrder.setVisibility(View.GONE);
            return;
        }

        // Query Firestore to see if the order is still active
        FirebaseFirestore.getInstance().collection("orders").document(lastOrderId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = doc.getString("status");

                        if (status != null && (status.equalsIgnoreCase("Pending") ||
                                status.equalsIgnoreCase("Preparing") ||
                                status.equalsIgnoreCase("Ready"))) {

                            // Show banner and update text
                            tvActiveOrderText.setText("🍽️ View Active Order (" + status + ") ➔");
                            cardActiveOrder.setVisibility(View.VISIBLE);

                            // Route to tracker on click
                            cardActiveOrder.setOnClickListener(v -> {
                                Intent intent = new Intent(GuestMenuActivity.this, OrderStatusActivity.class);
                                intent.putExtra("ORDER_ID", lastOrderId);
                                startActivity(intent);
                            });

                        } else {
                            // Order is completed or paid, hide banner and clear memory
                            cardActiveOrder.setVisibility(View.GONE);
                            prefs.edit().remove("LAST_ORDER_ID").apply();
                        }
                    }
                });
    }

    private void initializeGuestSession() {
        SharedPreferences prefs = getSharedPreferences("DinePulsePrefs", MODE_PRIVATE);
        currentVenueId = prefs.getString("CURRENT_VENUE", "venue_alpha");
        currentTableId = prefs.getString("CURRENT_TABLE", "table_08");
        uniqueSessionId = prefs.getString("SESSION_ID", UUID.randomUUID().toString());

        if (tvVenueName != null) {
            tvVenueName.setText("Seated at: " + currentTableId.toUpperCase().replace("_", " "));
        }
        registerGuestAtTable();
    }

    private void registerGuestAtTable() {
        if (currentVenueId == null || currentTableId == null) return;

        FirebaseFirestore.getInstance()
                .collection("venues").document(currentVenueId)
                .collection("tables").document(currentTableId)
                .update("status", "Occupied")
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to connect to table", Toast.LENGTH_SHORT).show());

        FirebaseFirestore.getInstance()
                .collection("venues").document(currentVenueId)
                .collection("tables").document(currentTableId)
                .collection("sessions").document(uniqueSessionId)
                .set(new java.util.HashMap<String, Object>() {{
                    put("startTime", System.currentTimeMillis());
                    put("status", "Active");
                }});
    }

    @Override
    public void onBackPressed() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Leave Menu?")
                .setMessage("Your cart will be saved, but you will return to the home screen.")
                .setPositiveButton("Exit", (dialog, which) -> {
                    super.onBackPressed();
                    Intent intent = new Intent(GuestMenuActivity.this, GuestHomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Stay", null)
                .show();
    }

    private void setupCategories() {
        chipGroupCategories.removeAllViews();
        List<String> categories = new ArrayList<>();
        categories.add("All");

        for (MenuItem item : fullMenuList) {
            if (item.getCategory() != null && !item.getCategory().trim().isEmpty()) {
                String cat = item.getCategory().trim();
                String normalizedCat = cat.substring(0, 1).toUpperCase() + cat.substring(1).toLowerCase();

                if (normalizedCat.equals("Main")) normalizedCat = "Mains";
                if (normalizedCat.equals("Drink")) normalizedCat = "Drinks";

                item.setCategory(normalizedCat);

                if (!categories.contains(normalizedCat)) {
                    categories.add(normalizedCat);
                }
            }
        }

        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setTextColor(getResources().getColor(android.R.color.black, getTheme()));

            if (category.equals("All")) chip.setChecked(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) filterMenu(category);
            });
            chipGroupCategories.addView(chip);
        }
    }

    private void filterMenu(String category) {
        if (category.equals("All")) {
            adapter.updateList(fullMenuList);
            return;
        }
        List<MenuItem> filtered = new ArrayList<>();
        for (MenuItem item : fullMenuList) {
            if (category.equals(item.getCategory())) {
                filtered.add(item);
            }
        }
        adapter.updateList(filtered);
    }

    @Override
    public void onActionClicked(MenuItem item) {
        try {
            if (CartManager.getInstance() != null) {
                CartManager.getInstance().addToCart(item);
                Toast.makeText(this, item.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Crash Prevented! Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}