package com.example.parada_finals;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private BottomNavigationView bottomNavigation;
    private String username;
    private EditText etOriginMap, etDestMap;
    private MaterialCardView cardSearchBar, cardRouteSelection;
    private TextView tvSearchPlaceholder;
    private ImageView ivCloseRoute;
    private final List<Marker> allMarkers = new ArrayList<>();
    private final Map<String, GeoPoint> locationPoints = new HashMap<>();
    private MyLocationNewOverlay myLocationOverlay;
    private Polyline currentRouteLine;
    private CardView cardDistanceOverlay;
    private TextView tvOverlayDistance;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx,
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        updateUsername();

        MaterialCardView cardProfileHeader = findViewById(R.id.cardProfileHeader);
        if (cardProfileHeader != null) {
            cardProfileHeader.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(MapActivity.this, cardProfileHeader);
                popup.getMenu().add("Logout");
                popup.setOnMenuItemClickListener(item -> {
                    if ("Logout".equals(item.getTitle())) {
                        Intent intent = new Intent(MapActivity.this, MainActivity.class);
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

        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        cardDistanceOverlay = findViewById(R.id.cardDistanceOverlay);
        tvOverlayDistance = findViewById(R.id.tvOverlayDistance);

        GeoPoint zamboanga = new GeoPoint(6.9214, 122.0790);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(zamboanga);

        checkLocationPermissions();
        initializeMarkers();

        cardSearchBar = findViewById(R.id.cardSearchBar);
        cardRouteSelection = findViewById(R.id.cardRouteSelection);
        tvSearchPlaceholder = findViewById(R.id.tvSearchPlaceholder);
        ivCloseRoute = findViewById(R.id.ivCloseRoute);
        etOriginMap = findViewById(R.id.etOriginMap);
        etDestMap = findViewById(R.id.etDestMap);

        View.OnClickListener showSearchListener = v -> showLocationPickerDialog(null);
        if (cardSearchBar != null) cardSearchBar.setOnClickListener(showSearchListener);
        if (tvSearchPlaceholder != null) tvSearchPlaceholder.setOnClickListener(showSearchListener);

        if (ivCloseRoute != null) {
            ivCloseRoute.setOnClickListener(v -> {
                cardRouteSelection.setVisibility(View.GONE);
                cardSearchBar.setVisibility(View.VISIBLE);
                if (currentRouteLine != null) {
                    mapView.getOverlays().remove(currentRouteLine);
                    currentRouteLine = null;
                }
                cardDistanceOverlay.setVisibility(View.GONE);
                mapView.invalidate();
            });
        }

        if (etOriginMap != null) {
            etOriginMap.setFocusable(false);
            etOriginMap.setOnClickListener(v -> showLocationPickerDialog(etOriginMap));
        }

        if (etDestMap != null) {
            etDestMap.setFocusable(false);
            etDestMap.setOnClickListener(v -> showLocationPickerDialog(etDestMap));
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_map);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_routes) {
                    Intent intent = new Intent(this, RoutesActivity.class);
                    intent.putExtra("USERNAME", username);
                    startActivity(intent);
                    finish();
                    return true;
                }
                if (id == R.id.nav_settings) {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    intent.putExtra("USERNAME", username);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return id == R.id.nav_map;
            });
        }
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
        if (tvUsernameHeader != null) {
            tvUsernameHeader.setText(username != null ? username : "Guest");
        }
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeMarkers() {
        addMarker(6.9174, 122.0754, "KCC Mall de Zamboanga", "Gov. Camins Ave", "Large shopping mall with dining and entertainment");
        addMarker(6.9093, 122.0753, "ADZU", "La Purisima St", "Private university known for academic excellence");
        addMarker(6.9400, 122.0488, "Pasonanca Park", "Pasonanca", "Popular park with pools and picnic areas");
        addMarker(6.9004, 122.0825, "Fort Pilar Shrine", "NS Valderosa St", "Historic shrine and cultural landmark");
        addMarker(6.9248, 122.0594, "Zamboanga Airport", "Moret IT", "Main airport serving the city");
        addMarker(6.9090, 122.0750, "SM Mindpro", "La Purisima St", "Modern mall in the city center");
        addMarker(6.9156, 122.0619, "WMSU", "Normal Rd", "State university with diverse programs");
        addMarker(6.9126, 122.0560, "Grandstand", "San Jose", "Public venue for events and sports");
        addMarker(6.9061, 122.0748, "Pilar College", "Justice RT Lim Blvd", "Well-known private school");
        addMarker(6.9284, 122.0467, "Yubenco Gusu", "San Jose Gusu", "Local supermarket and shopping area");
        addMarker(6.9183, 122.0838, "Yubenco Tetuan", "Tetuan", "Convenient shopping center in Tetuan");
        addMarker(6.9651, 121.9829, "Yubenco Ayala", "Ayala", "Mall serving western barangays");
        addMarker(6.9265, 122.0612, "Garden Orchid", "Gov. Camins Ave", "Hotel with events and dining services");

        addMarker(6.919773034344579, 122.15343937022541, "Arena Blanco", "Brgy. Arena Blanco", "Coastal barangay with beaches and fishing areas");
        addMarker(6.96373558093434, 121.94816715064628, "Ayala", "Brgy. Ayala", "Busy transport and commercial hub");
        addMarker(6.915959560863099, 122.05998500908423, "Baliwasan", "Brgy. Baliwasan", "Residential area near city proper");
        addMarker(6.952658391380117, 122.11848732442542, "Boalan", "Brgy. Boalan", "Growing residential and commercial zone");
        addMarker(7.097771919216988, 122.24040953976699, "Bolong", "Brgy. Bolong", "Beach area known for resorts and seafood");
        addMarker(7.107556004415077, 122.20027894767257, "Bunguiao", "Brgy. Bunguiao", "Quiet coastal community with scenic views");
        addMarker(6.99535001119771, 122.17729443791978, "Cabaluay", "Brgy. Cabaluay", "Coastal barangay with resorts and fishing");
        addMarker(6.943481595456743, 122.05737190908422, "Cabatangan", "Brgy. Cabatangan", "Residential area near airport zone");
        addMarker(6.9243845553103265, 122.02980908209567, "Calarian", "Brgy. Calarian", "Home of golf course and seaside spots");
        addMarker(6.914615272553048, 122.07337289876416, "Camino Nuevo", "Brgy. Camino Nuevo", "Central barangay with commercial activity");
        addMarker(6.914864258228712, 122.0460553379195, "Campo Islam", "Brgy. Campo Islam", "Historic coastal community");
        addMarker(6.9165683574969865, 122.0706138514137, "Canelar", "Brgy. Canelar", "Known for barter trade and markets");
        addMarker(6.974283883154848, 122.03457646675494, "Capisan", "Brgy. Capisan", "Cool elevated area with greenery");
        addMarker(6.973446925097431, 122.14670820297727, "Culianan", "Brgy. Culianan", "Agricultural hub with local markets");
        addMarker(7.210296116702678, 122.23172212812018, "Curuan", "Brgy. Curuan", "Major transport and trading hub");
        addMarker(6.928898798817435, 122.09211653976607, "Guiwan", "Brgy. Guiwan", "Urban barangay with mixed residential areas");
        addMarker(6.9874008303006025, 121.95809296860172, "La Paz", "Brgy. La Paz", "Scenic rural area with farms");
        addMarker(7.0982952055643045, 121.90299965326125, "Labuan", "Brgy. Labuan", "Coastal barangay with port access");
        addMarker(7.14305704852182, 121.90252791093206, "Limpapa", "Brgy. Limpapa", "Boundary barangay near neighboring province");
        addMarker(6.970478733340677, 122.10312333976624, "Lumbangan", "Brgy. Lumbangan", "Active residential and farming area");
        addMarker(6.952279342122488, 122.09064333791947, "Lunzuran", "Brgy. Lunzuran", "Quiet community with residential homes");
        addMarker(6.965875223422909, 121.98564167261706, "Maasin", "Brgy. Maasin", "Riverside barangay with agriculture");
        addMarker(6.915846113260775, 122.13447833976619, "Mampang", "Brgy. Mampang", "Known for salt production and coastal life");
        addMarker(6.9582373404550335, 122.14805194559018, "Mercedes", "Brgy. Mercedes", "Residential suburb with growing developments");
        addMarker(6.9776803900728295, 122.12828080221642, "Pasobolong", "Brgy. Pasobolong", "Agricultural area with farms and fields");
        addMarker(7.052948750702311, 121.90958083792019, "Patalon", "Brgy. Patalon", "Scenic area with hills and greenery");
        addMarker(6.911551461401163, 122.06519119374309, "San Jose Cawa-Cawa", "Brgy. San Jose", "Boulevard area near the sea");
        addMarker(6.908299192422751, 122.07635139575659, "San Jose Gusu", "Brgy. San Jose Gusu", "Busy urban center with markets");
        addMarker(6.93078520358863, 122.04634125515484, "San Roque", "Brgy. San Roque", "Large residential barangay");
        addMarker(7.078864084812816, 122.21381369887183, "Sangali", "Brgy. Sangali", "Port area with ferry connections");
        addMarker(6.90358190569268, 122.08195263791943, "Santa Barbara", "Brgy. Sta. Barbara", "Historic area with cultural sites");
        addMarker(6.909225909721007, 122.08696331093078, "Santa Catalina", "Brgy. Sta. Catalina", "Vibrant community near city center");
        addMarker(6.93179326275812, 122.07474627159064, "Santa Maria", "Brgy. Sta. Maria", "Large residential area");
        addMarker(7.033107105218401, 122.03946021633, "Santo Niño", "Brgy. Santo Niño", "Community with suburban feel");
        addMarker(6.934310848424623, 122.00106731093099, "Sinunoc", "Brgy. Sinunoc", "Scenic coastal and farming area");
        addMarker(7.023001649854029, 121.91933236675514, "Sinubong", "Brgy. Sinubong", "Pickup and transport point");
        addMarker(6.987823377594933, 121.92978945326065, "Talisayan", "Brgy. Talisayan", "Beach area with resorts");
        addMarker(6.90975476225303, 122.1123403839424, "Talon-Talon", "Brgy. Talon-Talon", "Busy residential and commercial area");
        addMarker(6.917908009117975, 122.09089715326036, "Tetuan", "Brgy. Tetuan", "Major barangay with schools and markets");
        addMarker(6.919832110919491, 122.10460962442515, "Tugbungan", "Brgy. Tugbungan", "Residential area with local businesses");
        addMarker(7.376906813442156, 122.29004090908656, "Vitali", "Brgy. Vitali", "Northernmost barangay with rural setting");
        addMarker(6.9414561961921795, 122.1325018802491, "Zambowood", "Brgy. Zambowood", "Peaceful residential community");
        addMarker(7.0000, 121.9210, "San Ramon", "Brgy. San Ramon", "Known for penal colony and coastal area");
        addMarker(7.023358516474465, 122.18827529444269, "Manicahan", "Brgy. Manicahan", "Rural barangay with farming and coastal communities");
        addMarker(6.953136288350877, 122.07172806121453, "Pasonanca", "Brgy. Pasonanca", "Known for its park, tree house, and natural attractions");
        addMarker(6.941437497400123, 122.09569929559007, "Putik", "Brgy. Putik", "Residential area with schools and growing developments");
        addMarker(6.939582380538536, 122.07943400034364, "Tumaga", "Brgy. Tumaga", "Urban barangay with mixed residential and commercial areas");
    }

    private void addMarker(double lat, double lon, String title, String snippet, String description) {
        GeoPoint point = new GeoPoint(lat, lon);
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        marker.setSnippet(snippet);

        marker.setOnMarkerClickListener((m, mv) -> {
            showMarkerDetailDialog(title, snippet, description, point);
            return true;
        });

        mapView.getOverlays().add(marker);
        allMarkers.add(marker);
        locationPoints.put(title, point);
    }

    private void showMarkerDetailDialog(String title, String snippet, String description, GeoPoint point) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_marker_detail, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvSnippet = dialogView.findViewById(R.id.tvDialogSnippet);
        TextView tvDescription = dialogView.findViewById(R.id.tvDialogDescription);
        MaterialCardView btnGetDirections = dialogView.findViewById(R.id.btnGetDirections);

        tvTitle.setText(title);
        tvSnippet.setText(snippet);
        tvDescription.setText(description);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnGetDirections.setOnClickListener(v -> {
            dialog.dismiss();
            prepareRoute(point, title);
        });

        dialog.show();
    }

    private void prepareRoute(GeoPoint destination, String destName) {
        cardSearchBar.setVisibility(View.GONE);
        cardRouteSelection.setVisibility(View.VISIBLE);
        etDestMap.setText(destName);
        etOriginMap.setText("Current Location");

        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            drawRoute(myLocationOverlay.getMyLocation(), destination);
        } else {
            Toast.makeText(this, "Waiting for GPS location...", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        new Thread(() -> {
            try {
                String urlString = String.format("https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=polyline",
                        start.getLongitude(), start.getLatitude(), end.getLongitude(), end.getLatitude());

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray routes = jsonResponse.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    String encodedPolyline = route.getString("geometry");
                    double distance = route.getDouble("distance") / 1000.0;
                    List<GeoPoint> routePoints = decodePolyline(encodedPolyline);

                    runOnUiThread(() -> {
                        if (currentRouteLine != null) {
                            mapView.getOverlays().remove(currentRouteLine);
                        }

                        currentRouteLine = new Polyline();
                        currentRouteLine.setPoints(routePoints);
                        currentRouteLine.setColor(Color.parseColor("#3F51B5"));
                        currentRouteLine.setWidth(12.0f);

                        mapView.getOverlays().add(currentRouteLine);
                        mapView.invalidate();

                        cardDistanceOverlay.setVisibility(View.VISIBLE);
                        tvOverlayDistance.setText(String.format("Distance: %.2f km", distance));
                        mapView.getController().animateTo(end);
                    });
                }
            } catch (Exception e) {
                Log.e("MapActivity", "Error fetching route", e);
                runOnUiThread(() -> {
                    Toast.makeText(MapActivity.this, "Error finding road route. Drawing straight line.", Toast.LENGTH_SHORT).show();
                    if (currentRouteLine != null) {
                        mapView.getOverlays().remove(currentRouteLine);
                    }
                    currentRouteLine = new Polyline();
                    currentRouteLine.addPoint(start);
                    currentRouteLine.addPoint(end);
                    currentRouteLine.setColor(Color.parseColor("#FF9800"));
                    mapView.getOverlays().add(currentRouteLine);
                    mapView.invalidate();
                    
                    float[] results = new float[1];
                    Location.distanceBetween(start.getLatitude(), start.getLongitude(),
                            end.getLatitude(), end.getLongitude(), results);
                    tvOverlayDistance.setText(String.format("Distance: %.2f km (Direct)", results[0] / 1000));
                    cardDistanceOverlay.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            GeoPoint p = new GeoPoint((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    private void showLocationPickerDialog(EditText targetEditText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_location_picker, null);
        builder.setView(dialogView);

        EditText etSearch = dialogView.findViewById(R.id.etSearchLocation);
        RecyclerView rvLocations = dialogView.findViewById(R.id.rvLocations);
        rvLocations.setLayoutManager(new LinearLayoutManager(this));

        List<String> locationNames = new ArrayList<>(locationPoints.keySet());
        Collections.sort(locationNames);

        LocationAdapter adapter = new LocationAdapter(locationNames, location -> {
            GeoPoint point = locationPoints.get(location);
            if (point != null) {
                if (targetEditText == null) {
                    prepareRoute(point, location);
                } else {
                    targetEditText.setText(location);
                    updateRouteFromInputs();
                }
            }
        });
        rvLocations.setAdapter(adapter);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
        adapter.setDialog(dialog);
    }

    private void updateRouteFromInputs() {
        String origin = etOriginMap.getText().toString();
        String dest = etDestMap.getText().toString();

        GeoPoint startPoint = null;
        GeoPoint endPoint = null;

        if (origin.equals("Current Location")) {
            if (myLocationOverlay != null) startPoint = myLocationOverlay.getMyLocation();
        } else {
            startPoint = locationPoints.get(origin);
        }

        endPoint = locationPoints.get(dest);

        if (startPoint != null && endPoint != null) {
            drawRoute(startPoint, endPoint);
        }
    }

    private static class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {
        private final List<String> originalList;
        private List<String> filteredList;
        private final OnLocationSelectedListener listener;
        private AlertDialog dialog;

        interface OnLocationSelectedListener {
            void onLocationSelected(String location);
        }

        LocationAdapter(List<String> list, OnLocationSelectedListener listener) {
            this.originalList = list;
            this.filteredList = new ArrayList<>(list);
            this.listener = listener;
        }

        void setDialog(AlertDialog dialog) { this.dialog = dialog; }

        void filter(String query) {
            if (query.isEmpty()) {
                filteredList = new ArrayList<>(originalList);
            } else {
                filteredList = originalList.stream()
                        .filter(s -> s.toLowerCase().contains(query.toLowerCase()))
                        .collect(Collectors.toList());
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = filteredList.get(position);
            holder.textView.setText(name);
            holder.itemView.setOnClickListener(v -> {
                listener.onLocationSelected(name);
                if (dialog != null) dialog.dismiss();
            });
        }

        @Override
        public int getItemCount() { return filteredList.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View view) {
                super(view);
                textView = view.findViewById(android.R.id.text1);
            }
        }
    }
}
