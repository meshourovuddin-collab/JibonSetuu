package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminManageUsersActivity extends Activity {

    private LinearLayout userContainer;
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
        title.setText("Manage Users");
        title.setTextSize(27);
        title.setTextColor(Color.rgb(7, 59, 101));
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Registered JibonSetu users");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.GRAY);

        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(
                        -1, -2);
        subtitleParams.setMargins(0, 6, 0, 16);
        root.addView(subtitle, subtitleParams);

        ScrollView scrollView = new ScrollView(this);

        userContainer = new LinearLayout(this);
        userContainer.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(userContainer);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                -1, 0, 1));

        setContentView(root);

        loadUsers();
    }

    private void loadUsers() {
        userContainer.removeAllViews();

        ProgressBar progressBar = new ProgressBar(this);
        userContainer.addView(progressBar);

        db.collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {
                    userContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        addMessage("No registered users found.");
                        return;
                    }

                    TextView count = new TextView(this);
                    count.setText("Total users: " + snapshot.size());
                    count.setTextSize(17);
                    count.setTextColor(Color.rgb(19, 128, 92));
                    count.setTypeface(null,
                            android.graphics.Typeface.BOLD);
                    count.setPadding(0, 0, 0, 16);

                    userContainer.addView(count);

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = value(doc, "name");
                        String phone = value(doc, "phone");
                        String role = value(doc, "role");
                        String status = value(doc, "accountStatus");

                        LinearLayout card = new LinearLayout(this);
                        card.setOrientation(LinearLayout.VERTICAL);
                        card.setPadding(18, 16, 18, 16);
                        card.setBackgroundColor(Color.WHITE);

                        LinearLayout.LayoutParams params =
                                new LinearLayout.LayoutParams(-1, -2);
                        params.setMargins(0, 0, 0, 12);

                        TextView userName = new TextView(this);
                        userName.setText(name.isEmpty()
                                ? "Unnamed User" : name);
                        userName.setTextSize(19);
                        userName.setTextColor(
                                Color.rgb(7, 59, 101));
                        userName.setTypeface(null,
                                android.graphics.Typeface.BOLD);

                        TextView details = new TextView(this);
                        details.setText(
                                "Phone: " + display(phone)
                                        + "\nRole: " + display(role)
                                        + "\nStatus: " + display(status));
                        details.setTextSize(14);
                        details.setTextColor(
                                Color.rgb(71, 85, 105));
                        details.setPadding(0, 8, 0, 0);

                        card.addView(userName);
                        card.addView(details);

                        userContainer.addView(card, params);
                    }
                })
                .addOnFailureListener(e -> {
                    userContainer.removeAllViews();
                    addMessage("Failed to load users.\n"
                            + e.getLocalizedMessage());

                    Toast.makeText(this,
                            "Check Firestore permissions and internet",
                            Toast.LENGTH_LONG).show();
                });
    }

    private String value(
            QueryDocumentSnapshot doc, String field) {
        String result = doc.getString(field);
        return result == null ? "" : result;
    }

    private String display(String value) {
        return value == null || value.trim().isEmpty()
                ? "Not provided" : value;
    }

    private void addMessage(String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(16);
        text.setTextColor(Color.DKGRAY);
        text.setGravity(Gravity.CENTER);
        text.setPadding(16, 32, 16, 32);
        userContainer.addView(text);
    }
}