package com.example.parada_finals;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LocationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        String username = getIntent().getStringExtra("USERNAME");

        // Back Button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Bottom Navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_settings);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_routes) {
                Intent intent = new Intent(this, RoutesActivity.class);
                intent.putExtra("USERNAME", username);
                startActivity(intent);
                finish();
                return true;
            }

            if (id == R.id.nav_map) {
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("USERNAME", username);
                startActivity(intent);
                finish();
                return true;
            }

            if (id == R.id.nav_settings) {
                finish(); // Go back to Settings
                return true;
            }

            return false;
        });
    }
}