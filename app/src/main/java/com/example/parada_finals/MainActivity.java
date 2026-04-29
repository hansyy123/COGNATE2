package com.example.parada_finals;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin, btnGuest;
    private TextView tvSignUp;
    private EditText etUsername, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize elements
        btnLogin = findViewById(R.id.btnLogin);
        btnGuest = findViewById(R.id.btnGuest);
        tvSignUp = findViewById(R.id.tvSignUp);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);

        // 1. Direct to Routes Page on Login click
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();
            } else {
                loginAsUser(username);
            }
        });

        // 2. Continue as Guest click
        if (btnGuest != null) {
            btnGuest.setOnClickListener(v -> {
                loginAsUser("Guest");
            });
        }

        // 3. Direct to Register Page on "Sign up" click
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginAsUser(String username) {
        // Save username to SharedPreferences for global access
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        prefs.edit().putString("username", username).apply();

        Intent intent = new Intent(MainActivity.this, RoutesActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish(); // Finish MainActivity so user can't go back to login without logging out
    }
}