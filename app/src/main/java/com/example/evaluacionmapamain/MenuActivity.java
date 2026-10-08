package com.example.evaluacionmapamain;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        MaterialButton btnOSM = findViewById(R.id.btnOpenStreetMap);
        MaterialButton btnGoogle = findViewById(R.id.btnGoogleMaps);

        // Abre la vista de OpenStreetMap que ya hicimos
        btnOSM.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Abre la nueva vista de Google Maps
        btnGoogle.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, GoogleMapsActivity.class);
            startActivity(intent);
        });
    }
}