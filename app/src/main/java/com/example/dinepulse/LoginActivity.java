package com.example.dinepulse;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvRegisterLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeUI();
        configureGoogleSignIn();

        // Auto-login if already authenticated
        if (mAuth.getCurrentUser() != null) {
            setLoadingState(true, "Resuming...");
            routeUser(mAuth.getCurrentUser());
        }

        setupClickListeners();
    }

    private void initializeUI() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
    }

    private void configureGoogleSignIn() {
        // NOTE: Ensure your default_web_client_id is correct in strings.xml from your google-services.json
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptEmailLogin());

        btnGoogleSignIn.setOnClickListener(v -> {
            setLoadingState(true, "Connecting...");
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void attemptEmailLogin() {
        String email = etEmail.getText().toString().trim();
        String pwd = etPassword.getText().toString().trim();

        if (!validateForm(email, pwd)) return;

        setLoadingState(true, "Authenticating...");

        mAuth.signInWithEmailAndPassword(email, pwd)
                .addOnSuccessListener(authResult -> routeUser(authResult.getUser()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login Failed", e);
                    Toast.makeText(this, "Login Failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    setLoadingState(false, "Sign In");
                });
    }

    private boolean validateForm(String email, String pwd) {
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return false;
        }
        if (pwd.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In Failed.", Toast.LENGTH_SHORT).show();
                setLoadingState(false, "Sign In");
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> routeUser(authResult.getUser()))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Firebase Auth Failed.", Toast.LENGTH_SHORT).show();
                    setLoadingState(false, "Sign In");
                });
    }

    /**
     * 🛠️ THE FAILSAFE: Routes the user. If they don't exist in Firestore, it creates them.
     */
    private void routeUser(FirebaseUser user) {
        if (user == null) {
            setLoadingState(false, "Sign In");
            return;
        }

        String uid = user.getUid();
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // User exists, route based on role
                            String role = document.getString("role");
                            navigateBasedOnRole(role);
                        } else {
                            // 🛠️ NO DOCUMENT FOUND? Create a default Guest profile automatically.
                            createDefaultGuestProfile(user);
                        }
                    } else {
                        Toast.makeText(this, "Database connection error.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        setLoadingState(false, "Sign In");
                    }
                });
    }

    private void createDefaultGuestProfile(FirebaseUser user) {
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("email", user.getEmail());
        newUser.put("role", "Guest");
        newUser.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(user.getUid()).set(newUser)
                .addOnSuccessListener(aVoid -> navigateBasedOnRole("Guest"))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to initialize profile.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    setLoadingState(false, "Sign In");
                });
    }

    private void navigateBasedOnRole(String role) {
        Intent intent;
        if ("Manager".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, ManagerDashboardActivity.class);
        } else {
            // FIX: Route the guest to the welcoming Home screen instead of the Scanner
            intent = new Intent(LoginActivity.this, GuestHomeActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading, String buttonText) {
        if (btnLogin != null && btnGoogleSignIn != null) {
            btnLogin.setText(buttonText);
            btnLogin.setEnabled(!isLoading);
            btnGoogleSignIn.setEnabled(!isLoading);
            etEmail.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);
            tvRegisterLink.setEnabled(!isLoading);
        }
    }
}