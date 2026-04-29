package com.example.parada_finals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegPassword, etRegConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);

        btnRegister.setOnClickListener(v -> {
            Toast.makeText(this, "Registration Successful (Demo)", Toast.LENGTH_LONG).show();
            // Go back to login
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}