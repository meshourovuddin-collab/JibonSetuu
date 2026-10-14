
package com.jibonsetu.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HelpSeekerRegisterActivity extends Activity {

    EditText etName, etPhone, etEmail, etPassword;
    Button btnRegister;
    TextView tvLogin;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_seeker_register);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registerHelpSeeker());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(
                    HelpSeekerRegisterActivity.this,
                    HelpSeekerLoginActivity.class
            ));
            finish();
        });
    }

    private void registerHelpSeeker() {

        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString()
                .trim().toLowerCase();
        String password = etPassword.getText().toString();

        // Name validation
        if (name.isEmpty()) {
            etName.setError("Enter your name");
            etName.requestFocus();
            return;
        }

        // Phone validation
        if (phone.isEmpty()) {
            etPhone.setError("Enter phone number");
            etPhone.requestFocus();
            return;
        }

        // Email required
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        // Email format validation
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Password validation
        if (password.trim().isEmpty()) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(
                    "Password must be at least 6 characters"
            );
            etPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        showError(
                                "Registration failed",
                                task.getException()
                        );
                        return;
                    }

                    FirebaseUser firebaseUser =
                            firebaseAuth.getCurrentUser();

                    if (firebaseUser == null) {
                        resetButton();
                        showToast("Account creation failed");
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("name", name);
                    user.put("phone", phone);
                    user.put("email", email);
                    user.put("role", "HELP_SEEKER");
                    user.put("accountStatus", "ACTIVE");

                    // Save the profile in Firestore
                    firestore.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {

                                FirebaseUser currentUser =
                                        firebaseAuth.getCurrentUser();

                                if (currentUser == null) {
                                    resetButton();
                                    showToast(
                                            "Account created. Please login."
                                    );
                                    return;
                                }

                                // Send email verification
                                currentUser.sendEmailVerification()
                                        .addOnCompleteListener(
                                                emailTask -> {

                                                    if (emailTask.isSuccessful()) {

                                                        firebaseAuth.signOut();

                                                        Toast.makeText(
                                                                HelpSeekerRegisterActivity.this,
                                                                "Account created! Check your email and verify it before login.",
                                                                Toast.LENGTH_LONG
                                                        ).show();

                                                        Intent intent = new Intent(
                                                                HelpSeekerRegisterActivity.this,
                                                                HelpSeekerLoginActivity.class
                                                        );

                                                        intent.setFlags(
                                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        );

                                                        startActivity(intent);
                                                        finish();

                                                    } else {
                                                        resetButton();

                                                        Toast.makeText(
                                                                HelpSeekerRegisterActivity.this,
                                                                "Account created, but verification email could not be sent. Please try sending it again from the login screen.",
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    }
                                                });
                            })
                            .addOnFailureListener(e -> {
                                resetButton();

                                Toast.makeText(
                                        HelpSeekerRegisterActivity.this,
                                        "Profile save failed: "
                                                + e.getLocalizedMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                });
    }

    private void showError(String title, Exception e) {
        resetButton();

        String message = e != null
                ? e.getLocalizedMessage()
                : "Please try again.";

        Toast.makeText(
                this,
                title + ":\n" + message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Register");
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}