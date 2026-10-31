package com.example.dinepulse;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;

public class ManagerReportActivity extends AppCompatActivity {

    private TextView tvGrossSales, tvTotalTips, tvOrderCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_report);

        tvGrossSales = findViewById(R.id.tvGrossSales);
        tvTotalTips = findViewById(R.id.tvTotalTips);
        tvOrderCount = findViewById(R.id.tvOrderCount);

        findViewById(R.id.btnBackDash).setOnClickListener(v -> finish());
        generateDailyReport();
    }

    private void generateDailyReport() {
        // Calculate midnight today for the query
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        Date startOfDay = cal.getTime();

        FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("status", "Completed")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalSales = 0;
                    double totalTips = 0;
                    int count = queryDocumentSnapshots.size();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Double subtotal = doc.getDouble("subtotal");
                        Double tip = doc.getDouble("tip");

                        if (subtotal != null) totalSales += subtotal;
                        if (tip != null) {
                            totalTips += tip;
                            totalSales += tip; // Gross includes tip
                        }
                    }

                    tvGrossSales.setText(String.format("৳%.2f", totalSales));
                    tvTotalTips.setText(String.format("৳%.2f", totalTips));
                    tvOrderCount.setText(String.valueOf(count));
                });
    }
}