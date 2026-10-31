package com.example.dinepulse.repository;

import com.example.dinepulse.models.CartItem;
import com.example.dinepulse.models.MenuItem;
import java.util.ArrayList;
import java.util.List;

public class CartRepository {
    private static CartRepository instance;
    private List<CartItem> cartList;

    private CartRepository() {
        cartList = new ArrayList<>();
    }

    public static synchronized CartRepository getInstance() {
        if (instance == null) {
            instance = new CartRepository();
        }
        return instance;
    }

    public List<CartItem> getCartItems() {
        return cartList;
    }

    // 🛠️ The updated bulletproof method
    public void addItemToCart(MenuItem item, double modifiedPrice, String modifierNotes) {
        for (CartItem cartItem : cartList) {

            // 1. Match by Name (Safest for dummy data without IDs)
            boolean isSameItem = cartItem.getMenuItem().getName().equals(item.getName());

            // 2. Safely check notes even if they are null
            boolean isSameNotes = false;
            if (cartItem.getNotes() == null && modifierNotes == null) {
                isSameNotes = true;
            } else if (cartItem.getNotes() != null && cartItem.getNotes().equals(modifierNotes)) {
                isSameNotes = true;
            }

            // Stack them if both match perfectly
            if (isSameItem && isSameNotes) {
                cartItem.incrementQuantity();
                return;
            }
        }

        // Otherwise, add as a new row
        cartList.add(new CartItem(item, 1, modifiedPrice, modifierNotes));
    }

    public double calculateTotal() {
        double total = 0;
        for (CartItem item : cartList) {
            total += item.getSubtotal();
        }
        return total;
    }

    public void clearCart() {
        cartList.clear();
    }
}