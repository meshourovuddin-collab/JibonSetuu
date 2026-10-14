package com.jibonsetu.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_login);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // Connect XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Login button
        btnLogin.setOnClickListener(v -> loginAdmin());
    }

    private void loginAdmin() {

        String email = etEmail.getText()
                .toString()
                .trim();

        String password = etPassword.getText()
                .toString()
                .trim();

        // Email validation
        if (email.isEmpty()) {
            etEmail.setError("Enter admin email");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        // Password validation
        if (password.isEmpty()) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        // Firebase Authentication
        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        if (firebaseAuth.getCurrentUser() == null) {
                            btnLogin.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Authentication error",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        String userId =
                                firebaseAuth
                                        .getCurrentUser()
                                        .getUid();

                        // Check admin role
                        firestore
                                .collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {

                                    btnLogin.setEnabled(true);

                                    if (documentSnapshot.exists()) {

                                        String role =
                                                documentSnapshot.getString("role");

                                        if ("ADMIN".equals(role)) {

                                            Toast.makeText(
                                                    this,
                                                    "Admin Login Successful!",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            Intent intent = new Intent(
                                                    AdminLoginActivity.this,
                                                    AdminHomeActivity.class
                                            );

                                            startActivity(intent);
                                            finish();

                                        } else {

                                            firebaseAuth.signOut();

                                            Toast.makeText(
                                                    this,
                                                    "Access denied. Admin account required.",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }

                                    } else {

                                        firebaseAuth.signOut();

                                        Toast.makeText(
                                                this,
                                                "Admin profile not found.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                })
                                .addOnFailureListener(e -> {

                                    btnLogin.setEnabled(true);

                                    Toast.makeText(
                                            this,
                                            "Database error:\n"
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });

                    } else {

                        btnLogin.setEnabled(true);

                        String errorMessage =
                                "Login failed";

                        if (task.getException() != null) {
                            errorMessage =
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}