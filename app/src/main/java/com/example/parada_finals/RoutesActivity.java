package com.example.parada_finals;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class RoutesActivity extends AppCompatActivity {

    private EditText etOrigin, etDestination;
    private Button btnCalculate;
    private CardView cardResult;
    private TextView tvResultDistance, tvResultFare;
    private BottomNavigationView bottomNavigationView;
    
    private LinearLayout btnTricycle, btnJeepney;
    private String selectedVehicle = "Tricycle"; // Default
    private String username;

    private String[] barangayList = {
            "Arena Blanco", "Ayala", "Baliwasan", "Boalan", "Bolong", "Bunguiao",
            "Cabaluay", "Cabatangan", "Calarian", "Camino Nuevo",
            "Campo Islam", "Canelar", "Culianan", "Curuan",
            "Guiwan", "La Paz", "Labuan", "Limpapa",
            "Lumbangan", "Lunzuran", "Maasin",
            "Mampang", "Mercedes", "Pasobolong",
            "Patalon", "San Jose Cawa-Cawa", "San Jose Gusu",
            "San Roque", "Sangali", "Santa Barbara", "Santa Catalina",
            "Santa Maria", "Santo Niño", "Sinubong", "Sinunoc",
            "Talisayan", "Talon-Talon", "Tetuan", "Tugbungan",
            "Vitali", "Zambowood", "San Ramon", "Manicahan",
            "Pasonanca", "Putik", "Tumaga"
    };

    private Map<String, LatLng> coordinatesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);

        initializeCoordinates();
        updateUsername();

        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        btnTricycle = findViewById(R.id.btnTricycle);
        btnJeepney = findViewById(R.id.btnJeepney);
        btnCalculate = findViewById(R.id.btnCalculate);
        cardResult = findViewById(R.id.cardResult);
        tvResultDistance = findViewById(R.id.tvResultDistance);
        tvResultFare = findViewById(R.id.tvResultFare);

        View cardProfileHeader = findViewById(R.id.cardProfileHeader);
        if (cardProfileHeader != null) {
            cardProfileHeader.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(RoutesActivity.this, v);
                popup.getMenu().add("Logout");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Logout")) {
                        Intent intent = new Intent(RoutesActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        etOrigin.setOnClickListener(v -> showLocationPickerDialog(etOrigin));
        etDestination.setOnClickListener(v -> showLocationPickerDialog(etDestination));
        btnTricycle.setOnClickListener(v -> selectVehicle("Tricycle"));
        btnJeepney.setOnClickListener(v -> selectVehicle("Jeepney"));

        String destinationFromMap = getIntent().getStringExtra("DESTINATION");
        if (destinationFromMap != null) {
            etDestination.setText(destinationFromMap);
        }

        btnCalculate.setOnClickListener(v -> calculateFare());

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_routes);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
             if (id == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("USERNAME", username);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_map) {
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("USERNAME", username);
                startActivity(intent);
                finish();
                return true;
            }
            return id == R.id.nav_routes;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUsername();
    }

    private void updateUsername() {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        username = prefs.getString("username", getIntent().getStringExtra("USERNAME"));

        TextView tvUsernameHeader = findViewById(R.id.tvUsernameHeader);
        TextView tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        
        if (username != null && !username.isEmpty()) {
            if (tvUsernameHeader != null) tvUsernameHeader.setText(username);
            if (tvWelcomeUser != null) tvWelcomeUser.setText("Hello, " + username);
        } else {
            if (tvUsernameHeader != null) tvUsernameHeader.setText("Guest");
            if (tvWelcomeUser != null) tvWelcomeUser.setText("Hello, Guest");
        }
    }

    private void showLocationPickerDialog(EditText targetField) {
        List<String> names = new ArrayList<>(Arrays.asList(barangayList));
        Collections.sort(names);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_location_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_NoTitleBar_Fullscreen)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.ui_main_bg)));
        }

        RecyclerView rvLocations = dialogView.findViewById(R.id.rvLocations);
        EditText etSearch = dialogView.findViewById(R.id.etSearchLocation);
        View btnClose = dialogView.findViewById(R.id.btnClosePicker);

        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        
        LocationAdapter adapter = new LocationAdapter(names, locationName -> {
            targetField.setText(locationName);
            dialog.dismiss();
        });
        rvLocations.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                List<String> filtered = names.stream()
                        .filter(n -> n.toLowerCase().contains(query))
                        .collect(Collectors.toList());
                adapter.updateList(filtered);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void selectVehicle(String type) {
        selectedVehicle = type;
        int orange = ContextCompat.getColor(this, R.color.ui_orange);
        int dark = ContextCompat.getColor(this, R.color.ui_input_bg);
        int white = Color.WHITE;
        int grey = ContextCompat.getColor(this, R.color.grey_600);

        if (type.equals("Tricycle")) {
            btnTricycle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orange));
            ((TextView) btnTricycle.getChildAt(1)).setTextColor(white);
            ((android.widget.ImageView) btnTricycle.getChildAt(0)).setColorFilter(white);

            btnJeepney.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dark));
            ((TextView) btnJeepney.getChildAt(1)).setTextColor(grey);
            ((android.widget.ImageView) btnJeepney.getChildAt(0)).setColorFilter(grey);
        } else {
            btnJeepney.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orange));
            ((TextView) btnJeepney.getChildAt(1)).setTextColor(white);
            ((android.widget.ImageView) btnJeepney.getChildAt(0)).setColorFilter(white);

            btnTricycle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dark));
            ((TextView) btnTricycle.getChildAt(1)).setTextColor(grey);
            ((android.widget.ImageView) btnTricycle.getChildAt(0)).setColorFilter(grey);
        }
    }

    private void initializeCoordinates() {
        coordinatesMap.put("KCC Mall", new LatLng(6.9174, 122.0754));
        coordinatesMap.put("SM Mindpro", new LatLng(6.9067, 122.0772));
        coordinatesMap.put("Town", new LatLng(6.905, 122.075));
        coordinatesMap.put("Arena Blanco", new LatLng(6.919773034344579, 122.15343937022541));
        coordinatesMap.put("Ayala", new LatLng(6.96373558093434, 121.94816715064628));
        coordinatesMap.put("Baliwasan", new LatLng(6.915959560863099, 122.05998500908423));
        coordinatesMap.put("Boalan", new LatLng(6.952658391380117, 122.11848732442542));
        coordinatesMap.put("Bolong", new LatLng(7.097771919216988, 122.24040953976699));
        coordinatesMap.put("Bunguiao", new LatLng(7.107556004415077, 122.20027894767257));
        coordinatesMap.put("Cabaluay", new LatLng(6.99535001119771, 122.17729443791978));
        coordinatesMap.put("Cabatangan", new LatLng(6.943481595456743, 122.05737190908422));
        coordinatesMap.put("Calarian", new LatLng(6.9243845553103265, 122.02980908209567));
        coordinatesMap.put("Camino Nuevo", new LatLng(6.914615272553048, 122.07337289876416));
        coordinatesMap.put("Canelar", new LatLng(6.9165683574969865, 122.0706138514137));
        coordinatesMap.put("Campo Islam", new LatLng(6.914864258228712, 122.0460553379195));
        coordinatesMap.put("Capisan", new LatLng(6.974283883154848, 122.03457646675494));
        coordinatesMap.put("Culianan", new LatLng(6.973446925097431, 122.14670820297727));
        coordinatesMap.put("Curuan", new LatLng(7.210296116702678, 122.23172212812018));
        coordinatesMap.put("Guiwan", new LatLng(6.928898798817435, 122.09211653976607));
        coordinatesMap.put("La Paz", new LatLng(6.9874008303006025, 121.95809296860172));
        coordinatesMap.put("Labuan", new LatLng(7.0982952055643045, 121.90299965326125));
        coordinatesMap.put("Limpapa", new LatLng(7.14305704852182, 121.90252791093206));
        coordinatesMap.put("Lumbangan", new LatLng(6.970478733340677, 122.10312333976624));
        coordinatesMap.put("Lunzuran", new LatLng(6.952279342122488, 122.09064333791947));
        coordinatesMap.put("Maasin", new LatLng(6.965875223422909, 121.98564167261706));
        coordinatesMap.put("Mampang", new LatLng(6.915846113260775, 122.13447833976619));
        coordinatesMap.put("Mercedes", new LatLng(6.9582373404550335, 122.14805194559018));
        coordinatesMap.put("Pasobolong", new LatLng(6.9776803900728295, 122.12828080221642));
        coordinatesMap.put("Patalon", new LatLng(7.052948750702311, 121.90958083792019));
        coordinatesMap.put("San Jose Cawa-Cawa", new LatLng(6.911551461401163, 122.06519119374309));
        coordinatesMap.put("San Jose Gusu", new LatLng(6.908299192422751, 122.07635139575659));
        coordinatesMap.put("San Ramon", new LatLng(7.0000, 121.9210));
        coordinatesMap.put("San Roque", new LatLng(6.93078520358863, 122.04634125515484));
        coordinatesMap.put("Sangali", new LatLng(7.078864084812816, 122.21381369887183));
        coordinatesMap.put("Santa Barbara", new LatLng(6.90358190569268, 122.08195263791943));
        coordinatesMap.put("Santa Catalina", new LatLng(6.909225909721007, 122.08696331093078));
        coordinatesMap.put("Santa Maria", new LatLng(6.93179326275812, 122.07474627159064));
        coordinatesMap.put("Santo Niño", new LatLng(7.033107105218401, 122.03946021633));
        coordinatesMap.put("Sinubung", new LatLng(7.023001649854029, 121.91933236675514));
        coordinatesMap.put("Sinunoc", new LatLng(6.934310848424623, 122.00106731093099));
        coordinatesMap.put("Talisayan", new LatLng(6.987823377594933, 121.92978945326065));
        coordinatesMap.put("Talon-Talon", new LatLng(6.90975476225303, 122.1123403839424));
        coordinatesMap.put("Tetuan", new LatLng(6.917908009117975, 122.09089715326036));
        coordinatesMap.put("Tugbungan", new LatLng(6.919832110919491, 122.10460962442515));
        coordinatesMap.put("Vitali", new LatLng(7.376906813442156, 122.29004090908656));
        coordinatesMap.put("Zambowood", new LatLng(6.9414561961921795, 122.1325018802491));
        coordinatesMap.put("Manicahan", new LatLng(7.023358516474465, 122.18827529444269));
        coordinatesMap.put("Pasonanca", new LatLng(6.953136288350877, 122.07172806121453));
        coordinatesMap.put("Putik", new LatLng(6.941437497400123, 122.09569929559007));
        coordinatesMap.put("Tumaga", new LatLng(6.939582380538536, 122.07943400034364));
    }

    private LatLng getLatLng(String name) {
        if (coordinatesMap.containsKey(name)) {
            return coordinatesMap.get(name);
        }
        Random r = new Random(name.hashCode());
        double lat = 6.90 + (r.nextDouble() * 0.15);
        double lng = 122.05 + (r.nextDouble() * 0.15);
        return new LatLng(lat, lng);
    }

    private void calculateFare() {
        String from = etOrigin.getText().toString();
        String to = etDestination.getText().toString();

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(this, "Please select locations", Toast.LENGTH_SHORT).show();
            return;
        }

        if (from.equals(to)) {
            Toast.makeText(this, "From and To cannot be the same", Toast.LENGTH_SHORT).show();
            cardResult.setVisibility(View.GONE);
            return;
        }

        LatLng start = getLatLng(from);
        LatLng end = getLatLng(to);
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        double roadDistanceKm = (results[0] / 1000.0) * 1.3;
        double fare = 0;

        if (selectedVehicle.equals("Tricycle")) {
            fare = roadDistanceKm * 15;
            tvResultDistance.setText(String.format("Estimated Distance: %.2f km (%d m)", roadDistanceKm, (int)(roadDistanceKm * 1000)));
        } else {
            fare = 14.0;
            if (roadDistanceKm > 4) {
                fare += (roadDistanceKm - 4) * 2.40;
            }
            tvResultDistance.setText("Route: " + from + " to " + to + String.format(" (%.2f km)", roadDistanceKm));
        }

        tvResultFare.setText(String.format("Fare: ₱%.2f", fare));
        cardResult.setVisibility(View.VISIBLE);
    }
}
