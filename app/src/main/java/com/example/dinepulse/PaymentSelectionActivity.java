package com.example.dinepulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dinepulse.repository.CartManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentSelectionActivity extends AppCompatActivity {

    private String orderId;
    private double totalAmount;
    private FrameLayout overlayLoading;
    private TextView tvLoadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_selection);

        orderId = getIntent().getStringExtra("ORDER_ID");
        totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 0.0);

        TextView tvPaymentTotal = findViewById(R.id.tvPaymentTotal);
        TextView tvPaymentOrderId = findViewById(R.id.tvPaymentOrderId);
        overlayLoading = findViewById(R.id.overlayLoading);
        tvLoadingText = findViewById(R.id.tvLoadingText);

        tvPaymentTotal.setText(String.format(Locale.US, "৳%.2f", totalAmount));
        tvPaymentOrderId.setText("Order ID: " + orderId);

        MaterialCardView cardBkash = findViewById(R.id.cardBkash);
        MaterialCardView cardNagad = findViewById(R.id.cardNagad);
        MaterialCardView cardCredit = findViewById(R.id.cardCredit);

        cardBkash.setOnClickListener(v -> processMockPayment("bKash"));
        cardNagad.setOnClickListener(v -> processMockPayment("Nagad"));
        cardCredit.setOnClickListener(v -> processMockPayment("Credit/Debit Card"));
    }

    private void processMockPayment(String method) {
        // Show loading overlay
        overlayLoading.setVisibility(View.VISIBLE);
        tvLoadingText.setText("Connecting to " + method + "...");

        // Simulate 2.5 second network delay
        new Handler().postDelayed(() -> {
            updateFirestorePaymentStatus(method);
        }, 2500);
    }

    private void updateFirestorePaymentStatus(String method) {
        tvLoadingText.setText("Verifying Payment...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentStatus", "PAID");
        updates.put("paymentMethod", method);

        FirebaseFirestore.getInstance().collection("orders").document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();

                    // Clear the cart ONLY when payment succeeds
                    CartManager.getInstance().clearCart();

                    // Save last order ID for the Receipt/Progress screen
                    SharedPreferences prefs = getSharedPreferences("DinePulsePrefs", MODE_PRIVATE);
                    prefs.edit().putString("LAST_ORDER_ID", orderId).apply();

                    // Route to the tracking screen
                    Intent intent = new Intent(PaymentSelectionActivity.this, OrderStatusActivity.class);
                    intent.putExtra("ORDER_ID", orderId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    overlayLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Payment Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}