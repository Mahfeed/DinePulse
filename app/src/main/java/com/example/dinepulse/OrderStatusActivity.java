package com.example.dinepulse;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderStatusActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvLiveStatus, tvStatusMessage, tvTimeReceived, tvStep2Text, tvStep3Text;
    private TextView ivStep2, ivStep3;
    private LottieAnimationView lottieChefAnimation;
    private String orderId;
    private ListenerRegistration orderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);

        db = FirebaseFirestore.getInstance();
        tvLiveStatus = findViewById(R.id.tvLiveStatus);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        tvTimeReceived = findViewById(R.id.tvTimeReceived);
        tvStep2Text = findViewById(R.id.tvStep2Text);
        tvStep3Text = findViewById(R.id.tvStep3Text);
        ivStep2 = findViewById(R.id.ivStep2);
        ivStep3 = findViewById(R.id.ivStep3);
        lottieChefAnimation = findViewById(R.id.lottieChefAnimation);
        MaterialButton btnBackToMenu = findViewById(R.id.btnBackToMenu);

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
        tvTimeReceived.setText(sdf.format(new Date()));

        orderId = getIntent().getStringExtra("ORDER_ID");

        btnBackToMenu.setOnClickListener(v -> finish());

        if (orderId != null) {
            startListeningToOrder();
        } else {
            Toast.makeText(this, "Order ID missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void startListeningToOrder() {
        DocumentReference docRef = db.collection("orders").document(orderId);
        orderListener = docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String status = snapshot.getString("status");
                if (status != null) {
                    updateUIBasedOnStatus(status);
                }
            }
        });
    }

    private void updateUIBasedOnStatus(String status) {
        tvLiveStatus.setText(status.toUpperCase());

        switch (status) {
            case "Pending":
                tvLiveStatus.setTextColor(Color.parseColor("#D97706"));
                tvStatusMessage.setText("Order received. Kitchen is accepting...");
                lottieChefAnimation.setAnimation(R.raw.chef_preparing);
                lottieChefAnimation.playAnimation();
                break;

            case "Preparing":
                tvLiveStatus.setTextColor(Color.parseColor("#D97706"));
                tvStatusMessage.setText("Your order is being prepared with care!");

                ivStep2.setText("●");
                ivStep2.setTextColor(Color.parseColor("#D97706"));
                ivStep2.setBackgroundResource(R.drawable.rounded_edittext_bg);
                ivStep2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FDE68A")));
                tvStep2Text.setText("Kitchen Preparing • In Progress");
                tvStep2Text.setTextColor(Color.parseColor("#111827"));

                lottieChefAnimation.setAnimation(R.raw.chef_preparing);
                lottieChefAnimation.playAnimation();
                break;

            case "Ready":
                tvLiveStatus.setTextColor(Color.parseColor("#16A34A"));
                tvStatusMessage.setText("Order complete! Please pay at the counter.");

                ivStep2.setText("✓");
                ivStep2.setTextColor(Color.parseColor("#16A34A"));
                ivStep2.setBackgroundTintList(null);
                tvStep2Text.setText("Kitchen Preparing • Completed");
                tvStep2Text.setTextColor(Color.parseColor("#111827"));

                ivStep3.setText("●");
                ivStep3.setTextColor(Color.parseColor("#16A34A"));
                ivStep3.setBackgroundResource(R.drawable.rounded_edittext_bg);
                ivStep3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BBF7D0")));
                tvStep3Text.setText("Out for Serving • Ready to Collect");
                tvStep3Text.setTextColor(Color.parseColor("#111827"));

                lottieChefAnimation.setAnimation(R.raw.chef_ready);
                lottieChefAnimation.playAnimation();
                break;

            case "Completed":
                Intent intent = new Intent(OrderStatusActivity.this, ReceiptActivity.class);
                intent.putExtra("ORDER_ID", orderId);
                startActivity(intent);
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}