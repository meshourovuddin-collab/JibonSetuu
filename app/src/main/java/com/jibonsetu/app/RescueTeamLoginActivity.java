package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class RescueTeamLoginActivity extends Activity {

    private EditText etEmail;
    private EditText etPassword;

    private Button btnLogin;
    private TextView tvRegister;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rescue_team_login);

        // Connect XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        // Login button
        btnLogin.setOnClickListener(v -> loginTeam());

        // Register link
        tvRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    RescueTeamLoginActivity.this,
                    RescueTeamRegisterActivity.class
            );

            startActivity(intent);
        });
    }

    private void loginTeam() {

        String email = etEmail.getText()
                .toString()
                .trim();

        String password = etPassword.getText()
                .toString()
                .trim();

        // Email validation
        if (email.isEmpty()) {

            etEmail.setError("Enter email address");
            etEmail.requestFocus();

            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();

            return;
        }

        // Password validation
        if (password.isEmpty()) {

            etPassword.setError("Enter password");
            etPassword.requestFocus();

            return;
        }

        // Disable button
        btnLogin.setEnabled(false);

        // Firebase Login
        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                RescueTeamLoginActivity.this,
                                "Rescue Team Login Successful!",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                RescueTeamLoginActivity.this,
                                RescueTeamHomeActivity.class
                        );

                        startActivity(intent);
                        finish();
                    } else {

                        String errorMessage =
                                "Login failed";

                        if (task.getException() != null) {

                            errorMessage =
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                RescueTeamLoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}