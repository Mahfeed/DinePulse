package com.example.dinepulse;
import com.example.dinepulse.repository.InventoryRepository;
import android.os.Bundle;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dinepulse.models.MenuItem;

import java.util.List;

public class AnalyticsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        TextView tvTotalItems = findViewById(R.id.tvTotalItems);
        TextView tvOutStockCount = findViewById(R.id.tvOutStockCount);
        TextView tvTableActivity = findViewById(R.id.tvTableActivity);

        InventoryRepository repo = InventoryRepository.getInstance();

        if (repo != null) {
            List<MenuItem> items = repo.getMenuItems();
            int total = items.size();
            int outOfStock = 0;

            for (MenuItem item : items) {
                if (!item.isAvailable())  {
                    outOfStock++;
                }
            }

            if (tvTotalItems != null) tvTotalItems.setText("Total Menu Items: " + total);
            if (tvOutStockCount != null) tvOutStockCount.setText("86'd Items: " + outOfStock);

            boolean hasPing = repo.isTable4AlertActive();
            if (tvTableActivity != null) tvTableActivity.setText("Scan Count: 42 Scans | Active Alert: " + (hasPing ? "Table 4 Active Ping!" : "None"));
        }
    }
}