package com.jibonsetu.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;

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

        btnHelpSeeker.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HelpSeekerLoginActivity.class);
            startActivity(intent);
        });

        btnRescueTeam.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RescueTeamLoginActivity.class);
            startActivity(intent);
        });

        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }
}