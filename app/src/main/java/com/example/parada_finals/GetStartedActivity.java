package com.example.parada_finals;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class GetStartedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always show the Get Started screen for debugging
        setContentView(R.layout.activity_get_started);

        MaterialButton btnGetStarted = findViewById(R.id.btnGetStarted);
        btnGetStarted.setOnClickListener(v -> {
            // Go to Login
            startActivity(new Intent(GetStartedActivity.this, MainActivity.class));
            finish();
        });
    }
}
