
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

public class AdminManageCentersActivity extends Activity {

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
        title.setText("Manage Help Centers");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(7, 59, 101));
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        Button refresh = new Button(this);
        refresh.setText("Refresh Help Centers");
        root.addView(refresh);

        ScrollView scroll = new ScrollView(this);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(container);

        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        setContentView(root);
        refresh.setOnClickListener(v -> loadCenters());
        loadCenters();
    }

    private void loadCenters() {
        container.removeAllViews();

        ProgressBar progress = new ProgressBar(this);
        container.addView(progress);

        db.collection("helpCenters")
                .get()
                .addOnSuccessListener(snapshot -> {
                    container.removeAllViews();

                    if (snapshot.isEmpty()) {
                        addMessage("No help centers found.");
                        return;
                    }

                    addMessage("Total Help Centers: " + snapshot.size());

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = value(doc, "name");
                        String address = value(doc, "address");
                        String capacity = value(doc, "capacity");
                        String resources = value(doc, "resources");
                        String phone = value(doc, "phone");

                        String info =
                                "Center ID: " + doc.getId()
                                        + "\nAddress: " + address
                                        + "\nCapacity: " + capacity
                                        + "\nResources: " + resources
                                        + "\nPhone: " + phone;

                        addCard(
                                name.isEmpty() ? "Help Center" : name,
                                info
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    container.removeAllViews();
                    addMessage("Could not load help centers.\n"
                            + e.getLocalizedMessage());

                    Toast.makeText(this,
                            "Check internet and Firestore Rules",
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