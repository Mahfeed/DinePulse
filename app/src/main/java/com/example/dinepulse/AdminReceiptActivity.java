package com.example.dinepulse;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminReceiptActivity extends AppCompatActivity {

    private TextView tvReceiptVenue, tvReceiptOrderId, tvReceiptSubtotal;
    private TextView tvReceiptDiscount, tvReceiptTotal, tvReceiptNotes;
    private LinearLayout containerReceiptItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_receipt);

        tvReceiptVenue = findViewById(R.id.tvAdminReceiptVenue);
        tvReceiptOrderId = findViewById(R.id.tvAdminReceiptOrderId);
        tvReceiptSubtotal = findViewById(R.id.tvAdminReceiptSubtotal);
        tvReceiptDiscount = findViewById(R.id.tvAdminReceiptDiscount);
        tvReceiptTotal = findViewById(R.id.tvAdminReceiptTotal);
        tvReceiptNotes = findViewById(R.id.tvAdminReceiptNotes);
        containerReceiptItems = findViewById(R.id.containerAdminReceiptItems);

        MaterialButton btnDone = findViewById(R.id.btnDoneAdmin);

        // FIX: Simply close the receipt to reveal the manager dashboard underneath
        btnDone.setOnClickListener(v -> finish());

        String orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId != null) {
            tvReceiptOrderId.setText("Order: " + orderId);
            fetchOrderDetails(orderId);
        } else {
            Toast.makeText(this, "Error: Order ID missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchOrderDetails(String orderId) {
        FirebaseFirestore.getInstance().collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) populateReceipt(doc);
                });
    }

    private void populateReceipt(DocumentSnapshot doc) {
        String venueId = doc.getString("venueId");
        if ("venue_alpha".equals(venueId)) {
            tvReceiptVenue.setText("DINEPULSE ROOFTOP");
        }

        Double subtotal = doc.getDouble("subtotal");
        Double total = doc.getDouble("totalAmount");
        String discountApplied = doc.getString("discountApplied");
        String customerNotes = doc.getString("customerNotes");

        if (subtotal == null) subtotal = 0.0;
        if (total == null) total = 0.0;

        double discountAmount = 0.0;
        if ("LUXURY20".equals(discountApplied)) discountAmount = subtotal * 0.20;

        tvReceiptSubtotal.setText(String.format(Locale.US, "৳%.2f", subtotal));
        tvReceiptDiscount.setText(String.format(Locale.US, "- ৳%.2f", discountAmount));
        tvReceiptTotal.setText(String.format(Locale.US, "৳%.2f", total));

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
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 8, 0, 8);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(itemDesc);
        tvDesc.setTextColor(Color.parseColor("#374151"));
        tvDesc.setTextSize(14f);
        RelativeLayout.LayoutParams paramsDesc = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsDesc.addRule(RelativeLayout.ALIGN_PARENT_START);
        tvDesc.setLayoutParams(paramsDesc);

        TextView tvTotal = new TextView(this);
        tvTotal.setText(String.format(Locale.US, "৳%.2f", itemTotal));
        tvTotal.setTextColor(Color.parseColor("#111827"));
        tvTotal.setTextSize(14f);
        RelativeLayout.LayoutParams paramsTotal = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsTotal.addRule(RelativeLayout.ALIGN_PARENT_END);
        tvTotal.setLayoutParams(paramsTotal);

        row.addView(tvDesc);
        row.addView(tvTotal);
        containerReceiptItems.addView(row);
    }
}