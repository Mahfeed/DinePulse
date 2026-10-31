package com.example.dinepulse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dinepulse.models.MenuItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

public class AdminAddMenuActivity extends AppCompatActivity {

    private ImageView ivAdminImagePreview;
    private EditText etItemName, etItemCategory, etItemPrice, etItemDesc;
    private Button btnSelectImage, btnSaveItem;

    private Uri imageUri = null;
    private FirebaseFirestore db;
    private StorageReference storageReference;

    // Opens the phone gallery
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivAdminImagePreview.setImageURI(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_menu);

        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("menu_images");

        ivAdminImagePreview = findViewById(R.id.ivAdminImagePreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        etItemName = findViewById(R.id.etItemName);
        etItemCategory = findViewById(R.id.etItemCategory);
        etItemPrice = findViewById(R.id.etItemPrice);
        etItemDesc = findViewById(R.id.etItemDesc);
        btnSaveItem = findViewById(R.id.btnSaveItem);

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        btnSaveItem.setOnClickListener(v -> uploadImageAndSaveData());
    }

    private void uploadImageAndSaveData() {
        String name = etItemName.getText().toString().trim();
        String category = etItemCategory.getText().toString().trim();
        String priceStr = etItemPrice.getText().toString().trim();
        String desc = etItemDesc.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Name and Price are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        btnSaveItem.setEnabled(false);
        btnSaveItem.setText("Uploading...");

        if (imageUri != null) {
            StorageReference fileRef = storageReference.child(UUID.randomUUID().toString() + ".jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveToFirestore(name, category, price, desc, uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
                        btnSaveItem.setEnabled(true);
                        btnSaveItem.setText("Save Item to Menu");
                    });
        } else {
            saveToFirestore(name, category, price, desc, null);
        }
    }

    private void saveToFirestore(String name, String category, double price, String desc, String imageUrl) {
        String id = db.collection("menu").document().getId();
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName(name);
        item.setCategory(category);
        item.setPrice(price);
        item.setDescription(desc);
        item.setAvailable(true);
        item.setImageUrl(imageUrl);

        db.collection("menu").document(id).set(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item Added Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
                    btnSaveItem.setEnabled(true);
                    btnSaveItem.setText("Save Item to Menu");
                });
    }
}