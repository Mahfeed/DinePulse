package com.example.dinepulse;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReceiptActivity extends AppCompatActivity {

    private TextView tvReceiptVenue, tvReceiptOrderId, tvReceiptSubtotal;
    private TextView tvReceiptDiscount, tvReceiptTip, tvReceiptTotal, tvReceiptNotes;
    private LinearLayout containerReceiptItems, layoutRating;
    private RatingBar ratingBar;
    private MaterialButton btnSubmitReview;

    private String currentOrderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        tvReceiptVenue = findViewById(R.id.tvReceiptVenue);
        tvReceiptOrderId = findViewById(R.id.tvReceiptOrderId);
        tvReceiptSubtotal = findViewById(R.id.tvReceiptSubtotal);
        tvReceiptDiscount = findViewById(R.id.tvReceiptDiscount);
        tvReceiptTip = findViewById(R.id.tvReceiptTip);
        tvReceiptTotal = findViewById(R.id.tvReceiptTotal);
        tvReceiptNotes = findViewById(R.id.tvReceiptNotes);
        containerReceiptItems = findViewById(R.id.containerReceiptItems);

        layoutRating = findViewById(R.id.layoutRating);
        ratingBar = findViewById(R.id.ratingBar);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);

        MaterialButton btnDone = findViewById(R.id.btnDone);

        // Hide rating features if viewed by a manager
        boolean isManager = getIntent().getBooleanExtra("IS_MANAGER", false);
        if (isManager) {
            btnDone.setText("Return to Dashboard");
            layoutRating.setVisibility(View.GONE);
        }

        btnDone.setOnClickListener(v -> returnToMenu(isManager));

        currentOrderId = getIntent().getStringExtra("ORDER_ID");
        if (currentOrderId != null) {
            tvReceiptOrderId.setText("Order: " + currentOrderId);
            fetchOrderDetails(currentOrderId);

            // Set up review submission
            btnSubmitReview.setOnClickListener(v -> submitReview());
        } else {
            Toast.makeText(this, "Error: Order ID missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        if (rating == 0.0f) {
            Toast.makeText(this, "Please select a star rating first.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitReview.setEnabled(false);
        btnSubmitReview.setText("Submitting...");

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("orderId", currentOrderId);
        feedback.put("rating", rating);
        feedback.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("feedback").add(feedback)
                .addOnSuccessListener(docRef -> {
                    btnSubmitReview.setText("Thank you!");
                    ratingBar.setIsIndicator(true); // Locks the stars so they can't change it
                    Toast.makeText(this, "Review submitted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSubmitReview.setEnabled(true);
                    btnSubmitReview.setText("Submit Review");
                    Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchOrderDetails(String orderId) {
        FirebaseFirestore.getInstance().collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        populateReceipt(doc);
                    } else {
                        Toast.makeText(this, "Receipt data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load receipt", Toast.LENGTH_SHORT).show());
    }

    private void populateReceipt(DocumentSnapshot doc) {
        String venueId = doc.getString("venueId");
        if ("venue_alpha".equals(venueId)) {
            tvReceiptVenue.setText("DINEPULSE ROOFTOP");
        }

        Double subtotal = doc.getDouble("subtotal");
        Double total = doc.getDouble("totalAmount");
        String discountApplied = doc.getString("discountApplied");
        String customerNotes = doc.getString("customerNotes"); // Fetch the chef notes

        if (subtotal == null) subtotal = 0.0;
        if (total == null) total = 0.0;

        double discountAmount = 0.0;
        if ("LUXURY20".equals(discountApplied)) {
            discountAmount = subtotal * 0.20;
        }

        double serviceTip = 0.00;

        tvReceiptSubtotal.setText(String.format(Locale.US, "৳%.2f", subtotal));
        tvReceiptDiscount.setText(String.format(Locale.US, "- ৳%.2f", discountAmount));
        tvReceiptTip.setText(String.format(Locale.US, "৳%.2f", serviceTip));
        tvReceiptTotal.setText(String.format(Locale.US, "৳%.2f", total));

        // Show notes if they exist
        if (customerNotes != null && !customerNotes.trim().isEmpty()) {
            tvReceiptNotes.setText("Notes: " + customerNotes);
            tvReceiptNotes.setVisibility(View.VISIBLE);
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("items");
        if (items != null) {
            containerReceiptItems.removeAllViews();
            for (Map<String, Object> item : items) {
                String name = (String) item.get("name");
                Object qtyObj = item.get("quantity");
                Object lineTotalObj = item.get("lineTotal");

                long qty = qtyObj instanceof Number ? ((Number) qtyObj).longValue() : 1;
                double lineTotal = lineTotalObj instanceof Number ? ((Number) lineTotalObj).doubleValue() : 0.0;

                addItemToReceipt(qty + "x " + name, lineTotal);
            }
        }
    }

    private void addItemToReceipt(String itemDesc, double itemTotal) {
        RelativeLayout row = new RelativeLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setPadding(0, 8, 0, 8);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(itemDesc);
        tvDesc.setTextColor(Color.parseColor("#374151"));
        tvDesc.setTextSize(14f);
        RelativeLayout.LayoutParams paramsDesc = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsDesc.addRule(RelativeLayout.ALIGN_PARENT_START);
        tvDesc.setLayoutParams(paramsDesc);

        TextView tvTotal = new TextView(this);
        tvTotal.setText(String.format(Locale.US, "৳%.2f", itemTotal));
        tvTotal.setTextColor(Color.parseColor("#111827"));
        tvTotal.setTextSize(14f);
        RelativeLayout.LayoutParams paramsTotal = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsTotal.addRule(RelativeLayout.ALIGN_PARENT_END);
        tvTotal.setLayoutParams(paramsTotal);

        row.addView(tvDesc);
        row.addView(tvTotal);
        containerReceiptItems.addView(row);
    }

    private void returnToMenu(boolean isManager) {
        Intent intent;
        if (isManager) {
            intent = new Intent(this, ManagerDashboardActivity.class);
        } else {
            intent = new Intent(this, GuestMenuActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}