
package com.jibonsetu.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class RescueTeamHomeActivity extends Activity {

    LinearLayout cardIncomingRequests;
    LinearLayout cardActiveRescues;
    LinearLayout cardMap;
    LinearLayout cardHistory;
    LinearLayout cardEmergency;

    TextView tvRequestCount;
    TextView tvTeamName;
    TextView tvStatus;

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_team_home);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        cardIncomingRequests = findViewById(R.id.cardIncomingRequests);
        cardActiveRescues = findViewById(R.id.cardActiveRescues);
        cardMap = findViewById(R.id.cardMap);
        cardHistory = findViewById(R.id.cardHistory);
        cardEmergency = findViewById(R.id.cardEmergency);

        tvRequestCount = findViewById(R.id.tvRequestCount);
        tvTeamName = findViewById(R.id.tvTeamName);
        tvStatus = findViewById(R.id.tvStatus);

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadTeamInfo();
        loadPendingCount();

        cardIncomingRequests.setOnClickListener(v -> loadIncomingRequests());
        cardActiveRescues.setOnClickListener(v -> loadActiveRescues());
        cardHistory.setOnClickListener(v -> loadRescueHistory());

        cardMap.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=rescue")
            );
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Map app not found", Toast.LENGTH_SHORT).show();
            }
        });

        cardEmergency.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Emergency Contacts")
                        .setItems(
                                new String[]{"Call 999", "Call 102", "Cancel"},
                                (dialog, which) -> {
                                    if (which == 0) openDialer("999");
                                    else if (which == 1) openDialer("102");
                                }
                        )
                        .show()
        );
    }

    private void loadTeamInfo() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("teamName");
                        if (name == null || name.trim().isEmpty()) {
                            name = document.getString("name");
                        }
                        if (name != null && !name.trim().isEmpty()) {
                            tvTeamName.setText(name);
                        }
                    }
                });
    }

    private void loadPendingCount() {
        db.collection("rescueRequests")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(snapshot ->
                        tvRequestCount.setText(
                                snapshot.size() + " pending requests"
                        ))
                .addOnFailureListener(e ->
                        tvRequestCount.setText("Unable to load requests"));
    }

    private void loadIncomingRequests() {
        db.collection("rescueRequests")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> requests = snapshot.getDocuments();

                    if (requests.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Incoming Rescue Requests")
                                .setMessage("No pending rescue requests.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    List<String> labels = new ArrayList<>();

                    for (DocumentSnapshot doc : requests) {
                        String location = doc.getString("location");
                        String details = doc.getString("details");

                        if (location == null) location = "Location not provided";
                        if (details == null) details = "No details provided";

                        labels.add("Location: " + location + "\nDetails: " + details);
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Incoming Requests (" + requests.size() + ")")
                            .setItems(labels.toArray(new String[0]),
                                    (dialog, which) ->
                                            showRequestDetails(requests.get(which)))
                            .setNegativeButton("Close", null)
                            .show();

                    loadPendingCount();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load requests: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }


    private void showRequestDetails(DocumentSnapshot request) {
        String location = request.getString("location");
        String details = request.getString("details");
        String userId = request.getString("userId");

        if (location == null || location.trim().isEmpty()) {
            location = "Not provided";
        }

        if (details == null) {
            details = "Not provided";
        }

        if (userId == null) {
            userId = "Not provided";
        }

        // use for lambda: Lambda- final variable
        final String mapLocation = location;

        String message = "Location: " + location
                + "\n\nEmergency details: " + details
                + "\n\nHelp Seeker ID: " + userId;

        new AlertDialog.Builder(this)
                .setTitle("Rescue Request")
                .setMessage(message)
                .setPositiveButton("Accept Rescue",
                        (dialog, which) -> acceptRequest(request.getId()))
                .setNeutralButton("Open Map",
                        (dialog, which) -> openMap(mapLocation))
                .setNegativeButton("Back", null)
                .show();
    }


    private void acceptRequest(String requestId) {
        String teamId = auth.getCurrentUser().getUid();

        db.collection("rescueRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Request no longer exists",
                                Toast.LENGTH_LONG).show();
                        loadPendingCount();
                        return;
                    }

                    String status = document.getString("status");

                    if (!"PENDING".equals(status)) {
                        Toast.makeText(this,
                                "Another team may have accepted this request",
                                Toast.LENGTH_LONG).show();
                        loadPendingCount();
                        return;
                    }

                    // Transaction checks the status again before accepting.
                    db.runTransaction(transaction -> {
                        DocumentSnapshot current = transaction.get(
                                db.collection("rescueRequests").document(requestId)
                        );

                        if (!current.exists()
                                || !"PENDING".equals(current.getString("status"))) {
                            throw new IllegalStateException(
                                    "This request has already been accepted."
                            );
                        }

                        transaction.update(
                                current.getReference(),
                                "status", "ACCEPTED",
                                "acceptedBy", teamId,
                                "acceptedAt", FieldValue.serverTimestamp()
                        );

                        return null;
                    }).addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Rescue request accepted!",
                                Toast.LENGTH_LONG).show();
                        loadPendingCount();
                    }).addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Could not accept request: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void loadActiveRescues() {
        String teamId = auth.getCurrentUser().getUid();

        db.collection("rescueRequests")
                .whereEqualTo("acceptedBy", teamId)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> requests = snapshot.getDocuments();

                    if (requests.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Active Rescues")
                                .setMessage("You have no active accepted rescues.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    List<String> labels = new ArrayList<>();

                    for (DocumentSnapshot doc : requests) {
                        String location = doc.getString("location");
                        String details = doc.getString("details");

                        labels.add("Location: "
                                + (location == null ? "Not provided" : location)
                                + "\nDetails: "
                                + (details == null ? "Not provided" : details));
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Active Rescues")
                            .setItems(labels.toArray(new String[0]),
                                    (dialog, which) -> {
                                        DocumentSnapshot selected = requests.get(which);
                                        String location = selected.getString("location");
                                        if (location != null) openMap(location);

                                        new AlertDialog.Builder(this)
                                                .setTitle("Complete Rescue?")
                                                .setMessage("Mark this rescue as completed?")
                                                .setPositiveButton("Complete",
                                                        (d, w) -> completeRescue(
                                                                selected.getId()))
                                                .setNegativeButton("Cancel", null)
                                                .show();
                                    })
                            .setNegativeButton("Close", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load active rescues: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void completeRescue(String requestId) {
        String teamId = auth.getCurrentUser().getUid();

        db.collection("rescueRequests").document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()
                            || !teamId.equals(document.getString("acceptedBy"))
                            || !"ACCEPTED".equals(document.getString("status"))) {
                        Toast.makeText(this,
                                "Could not complete this rescue.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    document.getReference()
                            .update("status", "COMPLETED",
                                    "completedAt", FieldValue.serverTimestamp())
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Rescue completed!",
                                        Toast.LENGTH_SHORT).show();
                                loadPendingCount();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Update failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                });
    }

    private void loadRescueHistory() {
        String teamId = auth.getCurrentUser().getUid();

        db.collection("rescueRequests")
                .whereEqualTo("acceptedBy", teamId)
                .whereEqualTo("status", "COMPLETED")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Rescue History")
                                .setMessage("No completed rescues yet.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    List<String> labels = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String location = doc.getString("location");
                        String details = doc.getString("details");

                        labels.add("Location: "
                                + (location == null ? "Not provided" : location)
                                + "\nDetails: "
                                + (details == null ? "Not provided" : details));
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Completed Rescues")
                            .setItems(labels.toArray(new String[0]), null)
                            .setPositiveButton("Close", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load history: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void openMap(String location) {
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Map app not found",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openDialer(String number) {
        Intent intent = new Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:" + number)
        );
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (auth != null && auth.getCurrentUser() != null && db != null) {
            loadPendingCount();
        }
    }
}
