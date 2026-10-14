package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class HelpSeekerLoginActivity extends Activity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help_seeker_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        firebaseAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Enter email address");
                etEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Enter password");
                etPassword.requestFocus();
                return;
            }

            btnLogin.setEnabled(false);

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {

                            Toast.makeText(
                                    HelpSeekerLoginActivity.this,
                                    "Login Successful!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Intent intent = new Intent(
                                    HelpSeekerLoginActivity.this,
                                    HelpSeekerHomeActivity.class
                            );

                            startActivity(intent);
                            finish();

                        } else {

                            String errorMessage = "Login failed";

                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }

                            Toast.makeText(
                                    HelpSeekerLoginActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        tvRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HelpSeekerLoginActivity.this,
                    HelpSeekerRegisterActivity.class
            );

            startActivity(intent);
        });
    }
}