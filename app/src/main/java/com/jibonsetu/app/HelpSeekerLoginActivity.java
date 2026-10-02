package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class HelpSeekerLoginActivity extends Activity {

    EditText etPhone, etPassword;
    Button btnLogin;
    TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help_seeker_login);

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {

            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (phone.isEmpty()) {
                etPhone.setError("Enter phone number");
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Enter password");
                return;
            }

            Toast.makeText(
                    HelpSeekerLoginActivity.this,
                    "Login system will be connected to Firebase later",
                    Toast.LENGTH_LONG
            ).show();
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