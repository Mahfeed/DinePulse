package com.example.dinepulse.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.dinepulse.models.CartItem;
import com.example.dinepulse.models.MenuItem;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    // Volatile ensures changes to the instance are immediately visible to all threads
    private static volatile CartManager instance;
    private final List<CartItem> cartItems;
    private final MutableLiveData<List<CartItem>> cartLiveData;

    private CartManager() {
        cartItems = new ArrayList<>();
        cartLiveData = new MutableLiveData<>();
        cartLiveData.postValue(new ArrayList<>());
    }

    // Double-checked locking singleton for thread safety
    public static CartManager getInstance() {
        if (instance == null) {
            synchronized (CartManager.class) {
                if (instance == null) {
                    instance = new CartManager();
                }
            }
        }
        return instance;
    }

    public synchronized void addToCart(MenuItem item) {
        // Defensive barrier: Reject insertion if Firebase data hasn't fully hydrated
        if (item == null || (item.getId() == null && item.getName() == null)) {
            return;
        }

        for (CartItem cartItem : cartItems) {
            MenuItem existingItem = cartItem.getMenuItem();
            if (existingItem == null) continue;

            boolean isMatch = false;

            // Safely compare IDs, falling back to Name if ID is missing
            if (item.getId() != null && existingItem.getId() != null) {
                isMatch = item.getId().equals(existingItem.getId());
            } else if (item.getName() != null && existingItem.getName() != null) {
                isMatch = item.getName().equals(existingItem.getName());
            }

            if (isMatch) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                cartLiveData.postValue(new ArrayList<>(cartItems));
                return;
            }
        }

        cartItems.add(new CartItem(item, 1, item.getPrice(), ""));
        cartLiveData.postValue(new ArrayList<>(cartItems));
    }

    public synchronized void decrementItem(MenuItem item) {
        if (item == null || (item.getId() == null && item.getName() == null)) return;

        for (int i = 0; i < cartItems.size(); i++) {
            MenuItem existingItem = cartItems.get(i).getMenuItem();
            if (existingItem == null) continue;

            boolean isMatch = false;
            if (item.getId() != null && existingItem.getId() != null) {
                isMatch = item.getId().equals(existingItem.getId());
            } else if (item.getName() != null && existingItem.getName() != null) {
                isMatch = item.getName().equals(existingItem.getName());
            }

            if (isMatch) {
                int newQty = cartItems.get(i).getQuantity() - 1;
                if (newQty > 0) {
                    cartItems.get(i).setQuantity(newQty);
                } else {
                    cartItems.remove(i);
                }
                cartLiveData.postValue(new ArrayList<>(cartItems));
                return;
            }
        }
    }

    public synchronized void removeItemCompletely(MenuItem item) {
        if (item == null || (item.getId() == null && item.getName() == null)) return;

        cartItems.removeIf(cartItem -> {
            MenuItem existingItem = cartItem.getMenuItem();
            if (existingItem == null) return false;

            if (item.getId() != null && existingItem.getId() != null) {
                return item.getId().equals(existingItem.getId());
            }
            return item.getName() != null && existingItem.getName() != null &&
                    item.getName().equals(existingItem.getName());
        });
        cartLiveData.postValue(new ArrayList<>(cartItems));
    }

    public LiveData<List<CartItem>> getCartLiveData() {
        return cartLiveData;
    }

    // Returns a defensive copy to prevent external UI components from mutating the central state
    public synchronized List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    public synchronized double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public synchronized void clearCart() {
        cartItems.clear();
        cartLiveData.postValue(new ArrayList<>());
    }
}