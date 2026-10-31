package com.example.dinepulse;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText nameInput, emailInput, passwordInput;
    private RadioGroup roleGroup;
    private Button registerButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameInput = findViewById(R.id.nameEditText);
        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
        roleGroup = findViewById(R.id.roleRadioGroup);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        TextView loginRedirect = findViewById(R.id.loginRedirectText);

        registerButton.setOnClickListener(v -> createAccount());
        loginRedirect.setOnClickListener(v -> finish());
    }

    private void createAccount() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || password.length() < 6) {
            Toast.makeText(this, "Please fill all fields (Password min 6 chars)", Toast.LENGTH_SHORT).show();
            return;
        }

        setInProgress(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    saveUserToFirestore(userId, name, email);
                })
                .addOnFailureListener(e -> {
                    setInProgress(false);
                    Toast.makeText(RegisterActivity.this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        int selectedId = roleGroup.getCheckedRadioButtonId();

        // 🛠️ THE FIX: Assign 'role' exactly once so it remains "effectively final" for the lambda
        String role = (selectedId == R.id.roleManager) ? "Manager" : "Guest";

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", name);
        userMap.put("email", email);
        userMap.put("role", role);

        db.collection("users").document(uid).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    setInProgress(false);
                    Toast.makeText(this, "Account Created Successfully", Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if ("Manager".equals(role)) {
                        intent = new Intent(RegisterActivity.this, ManagerDashboardActivity.class);
                    } else {
                        intent = new Intent(RegisterActivity.this, GuestMenuActivity.class);
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setInProgress(false);
                    Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
        }
    }
}