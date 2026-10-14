
package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RescueTeamRegisterActivity extends Activity {

    private EditText etTeamName, etLeaderName, etPhone;
    private EditText etEmail, etPassword, etLocation;

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
            startActivity(new Intent(
                    RescueTeamRegisterActivity.this,
                    RescueTeamLoginActivity.class
            ));
        });
    }

    private void registerTeam() {

        String teamName = etTeamName.getText().toString().trim();
        String leaderName = etLeaderName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString()
                .trim().toLowerCase();
        String password = etPassword.getText().toString();
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
            etEmail.setError("Email address is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (password.trim().isEmpty()) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            etLocation.setError("Enter team location");
            etLocation.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        String message = task.getException() != null
                                ? task.getException().getLocalizedMessage()
                                : "Registration failed";

                        Log.e(TAG, "Registration error", task.getException());
                        resetButton();

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    if (user == null) {
                        resetButton();
                        Toast.makeText(this,
                                "Account creation failed",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    saveTeamProfile(
                            user,
                            teamName,
                            leaderName,
                            phone,
                            email,
                            location
                    );
                });
    }

    private void saveTeamProfile(
            FirebaseUser user,
            String teamName,
            String leaderName,
            String phone,
            String email,
            String location) {

        String uid = user.getUid();

        Map<String, Object> team = new HashMap<>();
        team.put("uid", uid);
        team.put("teamName", teamName);
        team.put("leaderName", leaderName);
        team.put("name", teamName);
        team.put("phone", phone);
        team.put("email", email);
        team.put("location", location);
        team.put("role", "RESCUE_TEAM");

        // Pending verification/approval is safer than automatic activation.
        team.put("accountStatus", "PENDING_VERIFICATION");

        firestore.collection("users")
                .document(uid)
                .set(team)
                .addOnSuccessListener(unused -> {

                    user.sendEmailVerification()
                            .addOnCompleteListener(emailTask -> {

                                if (emailTask.isSuccessful()) {

                                    firebaseAuth.signOut();

                                    Toast.makeText(
                                            this,
                                            "Verification link sent to "
                                                    + email
                                                    + ". Verify your email before login.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    Intent intent = new Intent(
                                            RescueTeamRegisterActivity.this,
                                            RescueTeamLoginActivity.class
                                    );

                                    intent.setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);
                                    finish();

                                } else {
                                    resetButton();

                                    Log.e(TAG,
                                            "Verification email failed",
                                            emailTask.getException());

                                    Toast.makeText(
                                            this,
                                            "Profile saved, but verification email could not be sent. Please try again.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    resetButton();

                    Log.e(TAG, "Firestore error", e);

                    Toast.makeText(
                            this,
                            "Profile save failed: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Register");
    }
}