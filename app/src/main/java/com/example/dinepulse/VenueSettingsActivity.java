package com.example.dinepulse;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class VenueSettingsActivity extends AppCompatActivity {

    private EditText etVenueName, etVenueBio, etRating, etLogoUrl, etCoverUrl, etPromoHeadline, etPromoSubtext, etHours, etWifiSSID, etWifiPass;
    private SwitchMaterial switchPromo;
    private final String VENUE_ID = "venue_alpha";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_settings);

        etVenueName = findViewById(R.id.etSettingsVenueName);
        etVenueBio = findViewById(R.id.etSettingsVenueBio);
        etRating = findViewById(R.id.etSettingsRating);
        etLogoUrl = findViewById(R.id.etSettingsLogoUrl);
        etCoverUrl = findViewById(R.id.etSettingsCoverUrl);
        switchPromo = findViewById(R.id.switchPromo);
        etPromoHeadline = findViewById(R.id.etSettingsPromoHeadline);
        etPromoSubtext = findViewById(R.id.etSettingsPromoSubtext);
        etHours = findViewById(R.id.etSettingsHours);
        etWifiSSID = findViewById(R.id.etSettingsWifiSSID);
        etWifiPass = findViewById(R.id.etSettingsWifiPass);

        findViewById(R.id.btnBackToManager).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveVenueSettings());

        loadCurrentSettings();
    }

    private void loadCurrentSettings() {
        FirebaseFirestore.getInstance().collection("venues").document(VENUE_ID).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etVenueName.setText(doc.getString("venueName"));
                        etVenueBio.setText(doc.getString("venueBio"));
                        etRating.setText(doc.getString("ratingText"));
                        etLogoUrl.setText(doc.getString("logoUrl"));
                        etCoverUrl.setText(doc.getString("coverUrl"));
                        Boolean promoOn = doc.getBoolean("promoEnabled");
                        switchPromo.setChecked(promoOn != null && promoOn);
                        etPromoHeadline.setText(doc.getString("promoHeadline"));
                        etPromoSubtext.setText(doc.getString("promoSubtext"));
                        etHours.setText(doc.getString("operatingHours"));
                        etWifiSSID.setText(doc.getString("wifiSSID"));
                        etWifiPass.setText(doc.getString("wifiPass"));
                    }
                });
    }

    private void saveVenueSettings() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("venueName", etVenueName.getText().toString().trim());
        updates.put("venueBio", etVenueBio.getText().toString().trim());
        updates.put("ratingText", etRating.getText().toString().trim());
        updates.put("logoUrl", etLogoUrl.getText().toString().trim());
        updates.put("coverUrl", etCoverUrl.getText().toString().trim());
        updates.put("promoEnabled", switchPromo.isChecked());
        updates.put("promoHeadline", etPromoHeadline.getText().toString().trim());
        updates.put("promoSubtext", etPromoSubtext.getText().toString().trim());
        updates.put("operatingHours", etHours.getText().toString().trim());
        updates.put("wifiSSID", etWifiSSID.getText().toString().trim());
        updates.put("wifiPass", etWifiPass.getText().toString().trim());

        FirebaseFirestore.getInstance().collection("venues").document(VENUE_ID)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
    }
}