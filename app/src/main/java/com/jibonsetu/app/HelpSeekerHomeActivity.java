
package com.jibonsetu.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HelpSeekerHomeActivity extends Activity {

    private Button btnSOS;
    private LinearLayout cardRequestRescue;
    private LinearLayout cardLocation;
    private LinearLayout cardTeams;
    private LinearLayout cardEmergency;
    private LinearLayout cardRequests;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final int RESCUE_LOCATION_PERMISSION_CODE = 102;

    private String pendingLocationAddress;
    private String pendingEmergencyDetails;
    private boolean waitingForLocationSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_seeker_home);

        btnSOS = findViewById(R.id.btnSOS);
        cardRequestRescue = findViewById(R.id.cardRequestRescue);
        cardLocation = findViewById(R.id.cardLocation);
        cardTeams = findViewById(R.id.cardTeams);
        cardEmergency = findViewById(R.id.cardEmergency);
        cardRequests = findViewById(R.id.cardRequests);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        btnSOS.setOnClickListener(v -> showRescueForm());

        cardRequestRescue.setOnClickListener(v -> showRescueForm());

        cardLocation.setOnClickListener(v -> getMyLocation());

        cardTeams.setOnClickListener(v -> showRescueTeams());

        cardEmergency.setOnClickListener(v -> showEmergencyContacts());

        cardRequests.setOnClickListener(v -> showMyRequests());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (waitingForLocationSettings && isGpsEnabled()) {
            waitingForLocationSettings = false;

            String location = pendingLocationAddress;
            String details = pendingEmergencyDetails;

            pendingLocationAddress = null;
            pendingEmergencyDetails = null;

            if (location != null && details != null) {
                saveRescueRequest(location, details);
            }
        }
    }

    // Show the rescue request form
    private void showRescueForm() {
        EditText locationInput = new EditText(this);
        locationInput.setHint("Enter your address or area");

        EditText detailsInput = new EditText(this);
        detailsInput.setHint("Describe your emergency");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        layout.addView(locationInput);
        layout.addView(detailsInput);

        new AlertDialog.Builder(this)
                .setTitle("Request Rescue")
                .setView(layout)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String location =
                            locationInput.getText().toString().trim();
                    String details =
                            detailsInput.getText().toString().trim();

                    if (location.isEmpty() || details.isEmpty()) {
                        Toast.makeText(this,
                                "Please enter location and emergency details",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    saveRescueRequest(location, details);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Save request and start live GPS tracking
    private void saveRescueRequest(String location, String details) {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "Please login first",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!hasLocationPermission()) {
            pendingLocationAddress = location;
            pendingEmergencyDetails = details;

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    RESCUE_LOCATION_PERMISSION_CODE
            );
            return;
        }

        if (!isGpsEnabled()) {
            pendingLocationAddress = location;
            pendingEmergencyDetails = details;
            waitingForLocationSettings = true;

            Toast.makeText(this,
                    "Please enable your device location",
                    Toast.LENGTH_LONG).show();

            startActivity(new Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null
        ).addOnSuccessListener(currentLocation -> {

            if (currentLocation == null) {
                Toast.makeText(this,
                        "Could not get GPS location. Try again.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String userId = auth.getCurrentUser().getUid();

            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("location", location);
            request.put("details", details);
            request.put("status", "PENDING");
            request.put("latitude", currentLocation.getLatitude());
            request.put("longitude", currentLocation.getLongitude());
            request.put("locationUpdatedAt", FieldValue.serverTimestamp());
            request.put("createdAt", FieldValue.serverTimestamp());

            db.collection("rescueRequests")
                    .add(request)
                    .addOnSuccessListener(documentReference -> {

                        String requestId = documentReference.getId();

                        Intent serviceIntent = new Intent(
                                HelpSeekerHomeActivity.this,
                                LocationTrackingService.class
                        );

                        serviceIntent.putExtra(
                                LocationTrackingService.EXTRA_REQUEST_ID,
                                requestId
                        );

                        try {
                            ContextCompat.startForegroundService(
                                    HelpSeekerHomeActivity.this,
                                    serviceIntent
                            );

                            new AlertDialog.Builder(
                                    HelpSeekerHomeActivity.this)
                                    .setTitle("Request Submitted")
                                    .setMessage(
                                            "Your rescue request was submitted.\n\n"
                                                    + "Live location tracking has started."
                                    )
                                    .setPositiveButton("OK", null)
                                    .show();

                        } catch (Exception e) {
                            Toast.makeText(
                                    HelpSeekerHomeActivity.this,
                                    "Request saved, but tracking could not start: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    HelpSeekerHomeActivity.this,
                                    "Request failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );

        }).addOnFailureListener(e ->
                Toast.makeText(this,
                        "Unable to get current location: " + e.getMessage(),
                        Toast.LENGTH_LONG).show()
        );
    }

    private boolean hasLocationPermission() {
        boolean fine = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarse = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        return fine || coarse;
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            return false;
        }

        try {
            return locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    // Display current location
    private void getMyLocation() {

        if (!hasLocationPermission()) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_CODE
            );
            return;
        }

        if (!isGpsEnabled()) {
            Toast.makeText(this,
                    "Please enable device location",
                    Toast.LENGTH_LONG).show();

            startActivity(new Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null
        ).addOnSuccessListener(location -> {

            if (location != null) {
                String coordinates =
                        "Latitude: " + location.getLatitude()
                                + "\nLongitude: " + location.getLongitude();

                new AlertDialog.Builder(this)
                        .setTitle("My Current Location")
                        .setMessage(coordinates)
                        .setPositiveButton("Open Map", (dialog, which) -> {
                            Uri uri = Uri.parse(
                                    "https://www.google.com/maps/search/?api=1&query="
                                            + location.getLatitude() + ","
                                            + location.getLongitude()
                            );

                            startActivity(new Intent(
                                    Intent.ACTION_VIEW, uri));
                        })
                        .setNegativeButton("Close", null)
                        .show();
            } else {
                Toast.makeText(this,
                        "Location unavailable. Please try again.",
                        Toast.LENGTH_LONG).show();
            }

        }).addOnFailureListener(e ->
                Toast.makeText(this,
                        "Could not get location: " + e.getMessage(),
                        Toast.LENGTH_LONG).show()
        );
    }

    // Display rescue teams
    private void showRescueTeams() {

        db.collection("users")
                .whereEqualTo("role", "RESCUE_TEAM")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    StringBuilder result = new StringBuilder();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        result.append("Team: ")
                                .append(document.getString("teamName"))
                                .append("\nLeader: ")
                                .append(document.getString("leaderName"))
                                .append("\nPhone: ")
                                .append(document.getString("phone"))
                                .append("\nLocation: ")
                                .append(document.getString("location"))
                                .append("\n\n");
                    }

                    if (result.length() == 0) {
                        result.append("No rescue teams found.");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Rescue Teams")
                            .setMessage(result.toString())
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load rescue teams: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    // Display rescue requests made by the current user
    private void showMyRequests() {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "Please login first",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection("rescueRequests")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    StringBuilder result = new StringBuilder();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        result.append("Location: ")
                                .append(document.getString("location"))
                                .append("\nDetails: ")
                                .append(document.getString("details"))
                                .append("\nStatus: ")
                                .append(document.getString("status"))
                                .append("\nRequest ID: ")
                                .append(document.getId())
                                .append("\n\n");
                    }

                    if (result.length() == 0) {
                        result.append("You have not submitted any requests.");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("My Rescue Requests")
                            .setMessage(result.toString())
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load requests: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    // Emergency contacts
    private void showEmergencyContacts() {

        String[] contacts = {
                "National Emergency Service - 999",
                "Fire Service - 102"
        };

        new AlertDialog.Builder(this)
                .setTitle("Emergency Contacts")
                .setItems(contacts, (dialog, which) -> {

                    String phone;

                    if (which == 0) {
                        phone = "999";
                    } else {
                        phone = "102";
                    }

                    openDialer(phone);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void openDialer(String phone) {
        Intent intent = new Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:" + phone)
        );
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        boolean granted = hasLocationPermission();

        if (requestCode == RESCUE_LOCATION_PERMISSION_CODE) {

            if (granted
                    && pendingLocationAddress != null
                    && pendingEmergencyDetails != null) {

                String location = pendingLocationAddress;
                String details = pendingEmergencyDetails;

                pendingLocationAddress = null;
                pendingEmergencyDetails = null;

                saveRescueRequest(location, details);

            } else {
                pendingLocationAddress = null;
                pendingEmergencyDetails = null;

                Toast.makeText(this,
                        "Location permission is required for rescue tracking.",
                        Toast.LENGTH_LONG).show();
            }

        } else if (requestCode == LOCATION_PERMISSION_CODE) {

            if (granted) {
                getMyLocation();
            } else {
                Toast.makeText(this,
                        "Location permission denied",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
