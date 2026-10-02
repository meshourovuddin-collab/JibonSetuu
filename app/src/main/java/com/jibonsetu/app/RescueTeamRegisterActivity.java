package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RescueTeamRegisterActivity extends Activity {

    private EditText etTeamName;
    private EditText etLeaderName;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etLocation;

    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String TAG = "RESCUE_TEAM_REGISTER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rescue_team_register);

        etTeamName = findViewById(R.id.etTeamName);
        etLeaderName = findViewById(R.id.etLeaderName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etLocation = findViewById(R.id.etLocation);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registerTeam());

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(
                    RescueTeamRegisterActivity.this,
                    RescueTeamLoginActivity.class
            );
            startActivity(intent);
        });
    }

    private void registerTeam() {

        String teamName = etTeamName.getText().toString().trim();
        String leaderName = etLeaderName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (teamName.isEmpty()) {
            etTeamName.setError("Enter rescue team name");
            etTeamName.requestFocus();
            return;
        }

        if (leaderName.isEmpty()) {
            etLeaderName.setError("Enter team leader name");
            etLeaderName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Enter phone number");
            etPhone.requestFocus();
            return;
        }

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

        if (password.isEmpty()) {
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

        if (location.isEmpty()) {
            etLocation.setError("Enter team location");
            etLocation.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);

        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        saveTeamProfile(
                                teamName,
                                leaderName,
                                phone,
                                email,
                                location
                        );

                    } else {

                        btnRegister.setEnabled(true);

                        String message = "Registration failed";

                        if (task.getException()
                                instanceof FirebaseAuthException) {

                            FirebaseAuthException exception =
                                    (FirebaseAuthException)
                                            task.getException();

                            message = exception.getMessage();

                        } else if (task.getException() != null) {

                            message =
                                    task.getException().getMessage();
                        }

                        Log.e(TAG, message, task.getException());

                        Toast.makeText(
                                RescueTeamRegisterActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void saveTeamProfile(
            String teamName,
            String leaderName,
            String phone,
            String email,
            String location) {

        if (firebaseAuth.getCurrentUser() == null) {

            btnRegister.setEnabled(true);

            Toast.makeText(
                    this,
                    "Authentication failed",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String userId =
                firebaseAuth.getCurrentUser().getUid();

        Map<String, Object> team =
                new HashMap<>();

        team.put("teamName", teamName);
        team.put("leaderName", leaderName);
        team.put("phone", phone);
        team.put("email", email);
        team.put("location", location);
        team.put("role", "RESCUE_TEAM");
        team.put("accountStatus", "ACTIVE");

        firestore
                .collection("users")
                .document(userId)
                .set(team)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            RescueTeamRegisterActivity.this,
                            "Rescue Team registered successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    Intent intent = new Intent(
                            RescueTeamRegisterActivity.this,
                            RescueTeamLoginActivity.class
                    );

                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnRegister.setEnabled(true);

                    Log.e(
                            TAG,
                            "Firestore error",
                            e
                    );

                    Toast.makeText(
                            RescueTeamRegisterActivity.this,
                            "Profile save failed:\n"
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
