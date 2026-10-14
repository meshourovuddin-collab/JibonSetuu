
package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminManageRequestsActivity extends Activity {

    private LinearLayout container;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.rgb(244, 248, 252));

        TextView title = new TextView(this);
        title.setText("Manage SOS Requests");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(7, 59, 101));
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        Button refresh = new Button(this);
        refresh.setText("Refresh Requests");
        root.addView(refresh);

        ScrollView scroll = new ScrollView(this);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(container);

        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        setContentView(root);
        refresh.setOnClickListener(v -> loadRequests());
        loadRequests();
    }

    private void loadRequests() {
        container.removeAllViews();

        ProgressBar progress = new ProgressBar(this);
        container.addView(progress);

        db.collection("rescueRequests")
                .get()
                .addOnSuccessListener(snapshot -> {
                    container.removeAllViews();

                    if (snapshot.isEmpty()) {
                        addMessage("No SOS requests found.");
                        return;
                    }

                    int pending = 0;
                    int accepted = 0;
                    int completed = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String status = value(doc, "status");

                        if ("PENDING".equalsIgnoreCase(status)) {
                            pending++;
                        } else if ("ACCEPTED".equalsIgnoreCase(status)) {
                            accepted++;
                        } else if ("COMPLETED".equalsIgnoreCase(status)) {
                            completed++;
                        }
                    }

                    addMessage(
                            "Total: " + snapshot.size()
                                    + "\nPending: " + pending
                                    + "\nAccepted: " + accepted
                                    + "\nCompleted: " + completed
                    );

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String userId = value(doc, "userId");
                        String status = value(doc, "status");
                        String latitude = value(doc, "latitude");
                        String longitude = value(doc, "longitude");
                        String acceptedBy = value(doc, "acceptedBy");
                        String details = value(doc, "emergencyDetails");

                        String info =
                                "Request ID: " + doc.getId()
                                        + "\nUser ID: " + userId
                                        + "\nStatus: " + status
                                        + "\nLatitude: " + latitude
                                        + "\nLongitude: " + longitude
                                        + "\nAccepted By: " + acceptedBy
                                        + "\nEmergency Details: " + details;

                        addCard("SOS Request", info);
                    }
                })
                .addOnFailureListener(e -> {
                    container.removeAllViews();
                    addMessage("Could not load requests.\n"
                            + e.getLocalizedMessage());

                    Toast.makeText(this,
                            "Check Admin role and Firestore Rules",
                            Toast.LENGTH_LONG).show();
                });
    }

    private String value(QueryDocumentSnapshot doc, String field) {
        Object data = doc.get(field);
        return data == null ? "Not provided" : data.toString();
    }

    private void addCard(String heading, String details) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 18, 18, 18);
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 8, 0, 12);

        TextView name = new TextView(this);
        name.setText(heading);
        name.setTextSize(19);
        name.setTextColor(Color.rgb(7, 59, 101));
        name.setTypeface(null, Typeface.BOLD);

        TextView info = new TextView(this);
        info.setText(details);
        info.setTextSize(14);
        info.setTextColor(Color.DKGRAY);
        info.setPadding(0, 8, 0, 0);

        card.addView(name);
        card.addView(info);
        container.addView(card, params);
    }

    private void addMessage(String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(16);
        text.setTextColor(Color.DKGRAY);
        text.setGravity(Gravity.CENTER);
        text.setPadding(12, 24, 12, 24);
        container.addView(text);
    }
}