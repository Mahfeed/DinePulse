package com.example.dinepulse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnGuestMenu = findViewById(R.id.btnGuestMenu);
        Button btnManagerDashboard = findViewById(R.id.btnManagerDashboard);
        btnGuestMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GuestMenuActivity.class);
                startActivity(intent);
            }
        });
        btnManagerDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ManagerDashboardActivity.class);
                startActivity(intent);
            }
        });
    }
}