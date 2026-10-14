package com.jibonsetu.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    Button btnHelpSeeker;
    Button btnRescueTeam;
    Button btnAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnHelpSeeker = findViewById(R.id.btnHelpSeeker);
        btnRescueTeam = findViewById(R.id.btnRescueTeam);
        btnAdmin = findViewById(R.id.btnAdmin);

        // Help Seeker Button
        btnHelpSeeker.setOnClickListener(v -> {
            openActivity(HelpSeekerLoginActivity.class);
        });

        // Rescue Team Button
        btnRescueTeam.setOnClickListener(v -> {
            openActivity(RescueTeamLoginActivity.class);
        });

        // Admin Button
        btnAdmin.setOnClickListener(v -> {
            openActivity(AdminLoginActivity.class);
        });
    }

    private void openActivity(Class<?> activityClass) {
        try {
            Intent intent = new Intent(MainActivity.this, activityClass);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(
                    MainActivity.this,
                    "Page open korte problem hocche",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}