package com.example.dinepulse;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class QrScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private PreviewView viewFinder;
    private boolean isProcessing = false;

    private CameraControl cameraControl;
    private CameraInfo cameraInfo;
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        viewFinder = findViewById(R.id.viewFinder);
        findViewById(R.id.btnManualEntry).setOnClickListener(v -> showManualEntryDialog());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::scanBarcode);

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

                // Initialize Flash and Zoom controls
                cameraControl = camera.getCameraControl();
                cameraInfo = camera.getCameraInfo();
                setupCameraControls();

            } catch (ExecutionException | InterruptedException e) {
                Log.e("QrScanner", "Camera initialization failed.", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupCameraControls() {
        MaterialButton btnToggleFlash = findViewById(R.id.btnToggleFlash);
        SeekBar zoomSlider = findViewById(R.id.zoomSlider);

        if (cameraInfo.hasFlashUnit()) {
            btnToggleFlash.setOnClickListener(v -> {
                isFlashOn = !isFlashOn;
                cameraControl.enableTorch(isFlashOn);
                btnToggleFlash.setText(isFlashOn ? "Flash: ON" : "Flash: OFF");
            });
        } else {
            btnToggleFlash.setEnabled(false);
            btnToggleFlash.setText("No Flash");
        }

        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (cameraControl != null) {
                    cameraControl.setLinearZoom(progress / 100f);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void scanBarcode(ImageProxy imageProxy) {
        if (isProcessing || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        BarcodeScanner scanner = BarcodeScanning.getClient();

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && rawValue.contains("dinepulse.app/r/")) {
                            isProcessing = true;
                            parseUrlAndLaunch(rawValue);
                            break;
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
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

    private void parseUrlAndLaunch(String url) {
        try {
            String[] parts = url.split("/");
            String venueId = parts[4];
            String tableId = parts[6];
            saveSessionAndLaunch(venueId, tableId);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid QR Format", Toast.LENGTH_SHORT).show();
            isProcessing = false;
        }
    }

    private void saveSessionAndLaunch(String venueId, String tableId) {
        SharedPreferences prefs = getSharedPreferences("DinePulsePrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("CURRENT_VENUE", venueId)
                .putString("CURRENT_TABLE", tableId)
                .putString("SESSION_ID", java.util.UUID.randomUUID().toString())
                .apply();

        Toast.makeText(this, "Connected to " + tableId.toUpperCase(), Toast.LENGTH_SHORT).show();

        // FIX: Route to the new Venue Profile Page
        Intent intent = new Intent(this, VenueProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show();
        }
    }
}