
package com.jibonsetu.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.DateFormat;

public class RescueLiveMapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private Marker seekerMarker;
    private TextView locationStatus;
    private ListenerRegistration registration;
    private DocumentReference requestRef;

    private boolean firstLocationReceived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_live_map);

        locationStatus = findViewById(R.id.locationStatus);

        String requestId = getIntent().getStringExtra("requestId");

        if (requestId == null || requestId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.invalid_request_id,
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        requestRef = FirebaseFirestore.getInstance()
                .collection("rescueRequests")
                .document(requestId);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.rescueMap);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            locationStatus.setText(R.string.map_load_error);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        listenForLocation();
    }

    private void listenForLocation() {

        registration = requestRef.addSnapshotListener((snapshot, error) -> {

            if (error != null) {
                locationStatus.setText(R.string.location_load_error);
                return;
            }

            if (snapshot == null || !snapshot.exists()) {
                locationStatus.setText(R.string.request_not_found);
                return;
            }

            String status = snapshot.getString("status");

            Double latitude = snapshot.getDouble("latitude");
            Double longitude = snapshot.getDouble("longitude");

            if (latitude == null || longitude == null
                    || googleMap == null) {

                locationStatus.setText(
                        "COMPLETED".equals(status)
                                ? R.string.rescue_completed
                                : R.string.waiting_gps
                );
                return;
            }

            LatLng position = new LatLng(latitude, longitude);

            if (seekerMarker == null) {
                seekerMarker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(position)
                                .title(getString(R.string.help_seeker))
                );
            } else {
                seekerMarker.setPosition(position);
            }

            if (!firstLocationReceived) {
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(position, 16f)
                );
                firstLocationReceived = true;
            }

            if ("COMPLETED".equals(status)) {
                locationStatus.setText(R.string.rescue_completed);
                return;
            }

            Timestamp updatedAt =
                    snapshot.getTimestamp("locationUpdatedAt");

            if (updatedAt != null) {
                String time = DateFormat.getDateTimeInstance()
                        .format(updatedAt.toDate());

                locationStatus.setText(
                        getString(
                                R.string.live_location_updated,
                                status,
                                time
                        )
                );
            } else {
                locationStatus.setText(
                        getString(R.string.live_location_status, status)
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }

        super.onDestroy();
    }
}
