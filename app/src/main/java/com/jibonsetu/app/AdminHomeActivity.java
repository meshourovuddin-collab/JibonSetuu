package com.jibonsetu.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminHomeActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_admin_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvSummary = findViewById(R.id.tvAdminSummary);

        findViewById(R.id.btnManageUsers).setOnClickListener(v ->
                openPage(AdminManageUsersActivity.class));

        findViewById(R.id.btnManageTeams).setOnClickListener(v ->
                openPage(AdminManageTeamsActivity.class));

        findViewById(R.id.btnManageRequests).setOnClickListener(v ->
                openPage(AdminManageRequestsActivity.class));

        findViewById(R.id.btnManageCenters).setOnClickListener(v ->
                openPage(AdminManageCentersActivity.class));

        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> {
            auth.signOut();
            goToLogin();
        });

        verifyAdmin();
    }

    private void verifyAdmin() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()
                            || !"ADMIN".equals(doc.getString("role"))) {
                        auth.signOut();
                        goToLogin();
                        return;
                    }

                    loadSummary();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Admin verification failed",
                            Toast.LENGTH_LONG).show();
                    auth.signOut();
                    goToLogin();
                });
    }

    private void loadSummary() {
        tvSummary.setText("Loading dashboard data...");

        db.collection("users").get()
                .addOnSuccessListener(users ->
                        db.collection("rescueTeams").get()
                                .addOnSuccessListener(teams ->
                                        db.collection("sosRequests").get()
                                                .addOnSuccessListener(requests ->
                                                        db.collection("reliefCenters").get()
                                                                .addOnSuccessListener(centers ->
                                                                        tvSummary.setText(
                                                                                "Users: " + users.size()
                                                                                        + "\nRescue Teams: " + teams.size()
                                                                                        + "\nSOS Requests: " + requests.size()
                                                                                        + "\nRelief Centers: " + centers.size()
                                                                        )
                                                                )
                                                )
                                )
                )
                .addOnFailureListener(e ->
                        tvSummary.setText(
                                "Could not load dashboard data"
                        ));
    }

    private void openPage(Class<?> page) {
        startActivity(new Intent(this, page));
    }

    private void goToLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}