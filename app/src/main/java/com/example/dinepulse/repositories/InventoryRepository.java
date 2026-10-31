package com.example.dinepulse.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.dinepulse.models.MenuItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class InventoryRepository {

    private static InventoryRepository instance;
    private final MutableLiveData<List<MenuItem>> menuLiveData = new MutableLiveData<>();
    private final FirebaseFirestore db;
    private boolean table4AlertActive = false;

    private InventoryRepository() {
        db = FirebaseFirestore.getInstance();
        fetchLiveFirebaseMenu();
    }

    public static synchronized InventoryRepository getInstance() {
        if (instance == null) {
            instance = new InventoryRepository();
        }
        return instance;
    }

    public LiveData<List<MenuItem>> getMenuLiveData() {
        return menuLiveData;
    }

    public List<MenuItem> getMenuItems() {
        return menuLiveData.getValue() != null ? menuLiveData.getValue() : new ArrayList<>();
    }

    private void fetchLiveFirebaseMenu() {
        db.collection("menu")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("InventoryRepo", "Listen failed.", error);
                        return;
                    }

                    List<MenuItem> liveItems = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                String id = doc.getId();
                                String name = doc.getString("name");
                                String description = doc.getString("description");
                                String category = doc.getString("category");
                                String imageUrl = doc.getString("imageUrl"); // GRAB THE IMAGE URL

                                double price = 0.0;
                                Object priceObj = doc.get("price");
                                if (priceObj instanceof Number) {
                                    price = ((Number) priceObj).doubleValue();
                                }

                                boolean available = true;
                                Boolean availObj = doc.getBoolean("available");
                                if (availObj != null) {
                                    available = availObj;
                                }

                                if (name != null) {
                                    MenuItem item = new MenuItem();
                                    item.setId(id);
                                    item.setName(name);
                                    item.setDescription(description);
                                    item.setCategory(category);
                                    item.setPrice(price);
                                    item.setAvailable(available);
                                    item.setImageUrl(imageUrl); // SET THE IMAGE URL

                                    liveItems.add(item);
                                }
                            } catch (Exception e) {
                                Log.e("InventoryRepo", "Error parsing item: " + doc.getId(), e);
                            }
                        }
                    }
                    menuLiveData.postValue(liveItems);
                });
    }

    public void updateStock(String itemId, boolean inStock) {
        if (itemId != null && !itemId.isEmpty()) {
            db.collection("menu").document(itemId)
                    .update("available", inStock)
                    .addOnSuccessListener(aVoid -> Log.d("InventoryRepo", "Stock updated successfully"))
                    .addOnFailureListener(e -> Log.e("InventoryRepo", "Failed to update stock", e));
        }
    }

    public boolean isTable4AlertActive() {
        return table4AlertActive;
    }

    public void setTable4AlertActive(boolean active) {
        this.table4AlertActive = active;
    }
}