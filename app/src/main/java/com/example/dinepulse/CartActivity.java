package com.example.dinepulse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dinepulse.adapters.CartAdapter;
import com.example.dinepulse.models.CartItem;
import com.example.dinepulse.repository.CartManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CartActivity extends AppCompatActivity {

    private View rootLayout;
    private TextView tvTotalPrice, tvEmptyCartMessage;
    private EditText etCouponCode, etCartNotes; // Added etCartNotes
    private MaterialButton btnApplyCoupon, btnCheckout;
    private MaterialButtonToggleGroup toggleOrderType;
    private RecyclerView recyclerCart;
    private CartAdapter cartAdapter;

    private double subtotal = 0.0;
    private double finalTotal = 0.0;
    private String orderType = "Dine-in";
    private boolean couponApplied = false;

    // Idempotency & Concurrency guards
    private boolean isSubmitting = false;
    private String idempotencyOrderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        rootLayout = findViewById(android.R.id.content);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvEmptyCartMessage = findViewById(R.id.tvEmptyCartMessage);
        etCouponCode = findViewById(R.id.etCouponCode);
        etCartNotes = findViewById(R.id.etCartNotes); // Initialize notes field
        btnApplyCoupon = findViewById(R.id.btnApplyCoupon);
        btnCheckout = findViewById(R.id.btnCheckout);
        toggleOrderType = findViewById(R.id.toggleOrderType);
        recyclerCart = findViewById(R.id.recyclerCart);

        // Generate a single idempotency key for this checkout session
        idempotencyOrderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.US);

        setupCartList();

        // Single source of truth: Reactive LiveData observer
        CartManager.getInstance().getCartLiveData().observe(this, items -> {
            List<CartItem> safeItems = items != null ? items : new ArrayList<>();
            if (cartAdapter != null) {
                cartAdapter.updateList(safeItems);
            }
            updateEmptyState(safeItems.isEmpty());
            calculateTotal();
        });

        toggleOrderType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnTakeout) {
                    orderType = "Takeout";
                } else if (checkedId == R.id.btnDineIn) {
                    orderType = "Dine-in";
                }
            }
        });

        btnApplyCoupon.setOnClickListener(v -> applyCoupon());
        btnCheckout.setOnClickListener(v -> submitOrderIdempotently());
    }

    private void setupCartList() {
        recyclerCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(CartManager.getInstance().getCartItems());
        recyclerCart.setAdapter(cartAdapter);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (tvEmptyCartMessage != null) {
            tvEmptyCartMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        recyclerCart.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnCheckout.setEnabled(!isEmpty && !isSubmitting);
        btnCheckout.setAlpha(isEmpty ? 0.5f : 1.0f);
    }

    private void applyCoupon() {
        String code = etCouponCode.getText().toString().trim();
        if (couponApplied) {
            Snackbar.make(rootLayout, "VIP discount is already active.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (code.equalsIgnoreCase("LUXURY20")) {
            couponApplied = true;
            calculateTotal();
            etCouponCode.setEnabled(false);
            btnApplyCoupon.setEnabled(false);
            btnApplyCoupon.setText("Applied");
            Snackbar.make(rootLayout, "VIP 20% Discount Applied!", Snackbar.LENGTH_SHORT).show();
        } else {
            etCouponCode.setError("Invalid code");
            Snackbar.make(rootLayout, "Invalid VIP Coupon Code.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void calculateTotal() {
        subtotal = CartManager.getInstance().getTotalPrice();
        // Round to 2 decimal places using Math.round to prevent floating-point drift
        double rawTotal = couponApplied ? (subtotal * 0.80) : subtotal;
        finalTotal = Math.round(rawTotal * 100.0) / 100.0;
        tvTotalPrice.setText(String.format(Locale.US, "৳%.2f", finalTotal));
    }

    private void submitOrderIdempotently() {
        List<CartItem> currentItems = CartManager.getInstance().getCartItems();
        if (currentItems.isEmpty()) {
            Snackbar.make(rootLayout, "Your cart is empty.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Concurrency lock: prevent double submission
        if (isSubmitting) return;
        isSubmitting = true;
        btnCheckout.setEnabled(false);
        btnCheckout.setText("Submitting Order...");

        // Capture customer notes
        String customerNotes = etCartNotes != null ? etCartNotes.getText().toString().trim() : "";

        // Serialize cart items for Firestore document
        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (CartItem cartItem : currentItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("itemId", cartItem.getMenuItem().getId());
            itemMap.put("name", cartItem.getMenuItem().getName());
            itemMap.put("unitPrice", cartItem.getMenuItem().getPrice());
            itemMap.put("quantity", cartItem.getQuantity());
            itemMap.put("lineTotal", cartItem.getTotalPrice());
            serializedItems.add(itemMap);
        }

        Map<String, Object> orderPayload = new HashMap<>();
        orderPayload.put("orderId", idempotencyOrderId);
        orderPayload.put("venueId", "venue_alpha");
        orderPayload.put("tableId", "table_4");
        orderPayload.put("orderType", orderType);
        orderPayload.put("status", "Pending");
        orderPayload.put("subtotal", subtotal);
        orderPayload.put("discountApplied", couponApplied ? "LUXURY20" : "NONE");
        orderPayload.put("totalAmount", finalTotal);
        orderPayload.put("customerNotes", customerNotes); // Pushed to Firestore
        orderPayload.put("items", serializedItems);
        orderPayload.put("timestamp", System.currentTimeMillis());
        orderPayload.put("createdAt", FieldValue.serverTimestamp());

        // Write using the deterministic idempotencyOrderId as the document ID
        FirebaseFirestore.getInstance()
                .collection("orders")
                .document(idempotencyOrderId)
                .set(orderPayload)
                .addOnSuccessListener(aVoid -> completeCheckoutSuccess())
                .addOnFailureListener(e -> {
                    isSubmitting = false;
                    btnCheckout.setEnabled(true);
                    btnCheckout.setText("Proceed to Payment");
                    Snackbar.make(rootLayout, "Order failed: " + e.getMessage(), Snackbar.LENGTH_LONG)
                            .setAction("Retry", v -> submitOrderIdempotently())
                            .show();
                });
    }

    private void completeCheckoutSuccess() {
        // DO NOT clear the cart yet! We only clear it if payment succeeds.

        Intent intent = new Intent(this, PaymentSelectionActivity.class);
        intent.putExtra("ORDER_ID", idempotencyOrderId);
        intent.putExtra("TOTAL_AMOUNT", finalTotal);
        startActivity(intent);

        // We do NOT call finish() here, so if they back out of payment,
        // they return to their cart, and idempotency prevents duplicate orders.
    }
}