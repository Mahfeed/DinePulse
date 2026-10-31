package com.example.dinepulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.UUID;

public class GuestHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_home);

        // Button 1: Opens the actual Camera Scanner
        findViewById(R.id.btnScanTableQR).setOnClickListener(v -> {
            startActivity(new Intent(GuestHomeActivity.this, QrScannerActivity.class));
        });

        // Button 2: Opens the text dialog instantly, keeping the camera OFF
        findViewById(R.id.btnManualEmulatorEntry).setOnClickListener(v -> showManualEntryDialog());

        // Guest Logout
        findViewById(R.id.btnGuestLogout).setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("DinePulsePrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                FirebaseAuth.getInstance().signOut();
            }

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showManualEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Emulator Demo Entry");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etVenue = new EditText(this);
        etVenue.setHint("Venue ID (e.g., venue_alpha)");
        etVenue.setText("venue_alpha");
        layout.addView(etVenue);

        final EditText etTable = new EditText(this);
        etTable.setHint("Table ID (e.g., table_08)");
        etTable.setText("table_08");
        layout.addView(etTable);

        builder.setView(layout);
        builder.setPositiveButton("Enter", (dialog, which) -> {
            String venueId = etVenue.getText().toString().trim();
            String tableId = etTable.getText().toString().trim();
            if (!venueId.isEmpty() && !tableId.isEmpty()) {
                saveSessionAndLaunch(venueId, tableId);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveSessionAndLaunch(String venueId, String tableId) {
        SharedPreferences prefs = getSharedPreferences("DinePulsePrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("CURRENT_VENUE", venueId)
                .putString("CURRENT_TABLE", tableId)
                .putString("SESSION_ID", UUID.randomUUID().toString())
                .apply();

        Toast.makeText(this, "Connected to " + tableId.toUpperCase(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, VenueProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}