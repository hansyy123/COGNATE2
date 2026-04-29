package com.example.parada_finals;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin;
    private MaterialButton btnGuest;
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
                // Save username to SharedPreferences for global access
                SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
                prefs.edit().putString("username", username).apply();

                Intent intent = new Intent(MainActivity.this, RoutesActivity.class);
                intent.putExtra("USERNAME", username);
                startActivity(intent);
                finish(); // Finish MainActivity so user can't go back to login without logging out
            }
        });

        // 2. Direct to Register Page on "Create one" click
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 3. Continue as Guest
        btnGuest.setOnClickListener(v -> {
            // Clear any previous user data to treat as guest
            SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
            prefs.edit().remove("username").apply();

            Intent intent = new Intent(MainActivity.this, RoutesActivity.class);
            intent.putExtra("USERNAME", "Guest");
            startActivity(intent);
            finish();
        });
    }
}