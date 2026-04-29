package com.example.parada_finals;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AccountActivity extends AppCompatActivity {

    EditText etUsername, etPassword, etConfirmPassword;
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSave = findViewById(R.id.btnSave);

        // Load saved data
        loadUserData();

        // Back Button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Save button
        btnSave.setOnClickListener(v -> saveData());

        // Bottom Navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_settings);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_routes) {
                Intent intent = new Intent(this, RoutesActivity.class);
                startActivity(intent);
                finish();
                return true;
            }

            if (id == R.id.nav_map) {
                Intent intent = new Intent(this, MapActivity.class);
                startActivity(intent);
                finish();
                return true;
            }

            if (id == R.id.nav_settings) {
                finish(); // Return to Settings
                return true;
            }

            return false;
        });
    }

    private void saveData() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) ||
                TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(confirmPassword)) {

            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("username", username);
        editor.putString("password", password);
        editor.apply();

        Toast.makeText(this, "Account updated successfully!", Toast.LENGTH_SHORT).show();

        // Return to Settings after saving
        finish();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);

        String savedUsername = prefs.getString("username", "");
        String savedPassword = prefs.getString("password", "");

        etUsername.setText(savedUsername);
        etPassword.setText(savedPassword);
        etConfirmPassword.setText(savedPassword);
    }
}
