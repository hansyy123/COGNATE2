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
import androidx.core.content.ContextCompat;
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
import java.util.Arrays;
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
    private Marker startMarker, endMarker;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private String[] barangayList = {
            "Town", "Arena Blanco", "Ayala", "Baliwasan", "Baluno", "Boalan", "Bolong", "Buenavista", "Bunguiao",
            "Cabaluay", "Cabatangan", "Calarian", "Camino Nuevo", "Campo Islam", "Canelar", "Cawit", "Culianan",
            "Curuan", "Divisoria", "Guisao", "Guiwan", "Kasanyangan", "La Paz", "Labuan", "Lamisahan",
            "Landang Gua", "Landang Laum", "Lanzones", "Lapakan", "Latuan (Curuan)", "Licomo", "Limaong",
            "Limpapa", "Lubigan", "Lumayang", "Lumbangan", "Lunzuran", "Maasin", "Malagutay", "Mampang",
            "Manalipa", "Mangusu", "Manicahan", "Mariki", "Mercedes", "Muti", "Pamucutan", "Pangapuyan",
            "Panubigan", "Pasilmanta (Sacol Island)", "Pasobolong", "Pasonanca", "Patalon", "Putik",
            "Recodo", "Rio Hondo", "Salaan", "San Jose Cawa-Cawa", "San Jose Gusu", "San Ramon",
            "San Roque", "Sangali", "Santa Barbara", "Santa Catalina", "Santa Maria", "Santo Niño",
            "Sibulao (Caruan)", "Sinubung", "Sinunoc", "Tagasilay", "Taguiti", "Talabaan", "Talisayan",
            "Talon-Talon", "Taluksangay", "Tetuan", "Tictapul", "Tigbalabag", "Tigtabon", "Tugbungan",
            "Tulungatung", "Tumaga", "Victoria", "Vitali", "Zambowood"
    };

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
                clearRoute();
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

    private void clearRoute() {
        if (currentRouteLine != null) {
            mapView.getOverlays().remove(currentRouteLine);
            currentRouteLine = null;
        }
        if (startMarker != null) {
            mapView.getOverlays().remove(startMarker);
            startMarker = null;
        }
        if (endMarker != null) {
            mapView.getOverlays().remove(endMarker);
            endMarker = null;
        }
        cardDistanceOverlay.setVisibility(View.GONE);
        mapView.invalidate();
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
        // Markers with precise coordinates from RoutesActivity
        addMarker(6.9174, 122.0754, "KCC Mall de Zamboanga", "Mall", "Main center");
        addMarker(6.9067, 122.0772, "SM Mindpro", "Mall", "City center");
        addMarker(6.905, 122.075, "Town", "City Proper", "Downtown");
        addMarker(6.919773, 122.153439, "Arena Blanco", "Brgy", "Coastal");
        addMarker(6.963736, 121.948167, "Ayala", "Brgy", "Transport hub");
        addMarker(6.915959, 122.059985, "Baliwasan", "Brgy", "Residential");
        addMarker(6.952658, 122.118487, "Boalan", "Brgy", "Active zone");
        addMarker(7.097772, 122.240410, "Bolong", "Brgy", "Beach area");
        addMarker(7.107556, 122.200279, "Bunguiao", "Brgy", "Coastal");
        addMarker(6.995350, 122.177294, "Cabaluay", "Brgy", "Fishing");
        addMarker(6.943482, 122.057372, "Cabatangan", "Brgy", "Near airport");
        addMarker(6.924385, 122.029809, "Calarian", "Brgy", "Golf area");
        addMarker(6.914615, 122.073373, "Camino Nuevo", "Brgy", "Central");
        addMarker(6.914864, 122.046055, "Campo Islam", "Brgy", "Coastal");
        addMarker(6.916568, 122.070614, "Canelar", "Brgy", "Barter trade");
        addMarker(6.973447, 122.146708, "Culianan", "Brgy", "Agricultural");
        addMarker(7.210296, 122.231722, "Curuan", "Brgy", "Trading hub");
        addMarker(6.928899, 122.092117, "Guiwan", "Brgy", "Urban");
        addMarker(6.987401, 121.958093, "La Paz", "Brgy", "Rural");
        addMarker(7.098295, 121.902999, "Labuan", "Brgy", "Port");
        addMarker(7.143057, 121.902528, "Limpapa", "Brgy", "Boundary");
        addMarker(6.970479, 122.103123, "Lumbangan", "Brgy", "Farming");
        addMarker(6.952279, 122.090643, "Lunzuran", "Brgy", "Residential");
        addMarker(6.965875, 121.985642, "Maasin", "Brgy", "Riverside");
        addMarker(6.915846, 122.134478, "Mampang", "Brgy", "Salt production");
        addMarker(6.958237, 122.148052, "Mercedes", "Brgy", "Suburb");
        addMarker(6.977680, 122.128281, "Pasobolong", "Brgy", "Agricultural");
        addMarker(7.052949, 121.909581, "Patalon", "Brgy", "Scenic");
        addMarker(6.911551, 122.065191, "San Jose Cawa-Cawa", "Brgy", "Boulevard");
        addMarker(6.908299, 122.076351, "San Jose Gusu", "Brgy", "Urban center");
        addMarker(6.930785, 122.046341, "San Roque", "Brgy", "Residential");
        addMarker(7.078864, 122.213814, "Sangali", "Brgy", "Port");
        addMarker(6.903582, 122.081953, "Santa Barbara", "Brgy", "Historic");
        addMarker(6.909226, 122.086963, "Santa Catalina", "Brgy", "Vibrant");
        addMarker(6.931793, 122.074746, "Santa Maria", "Brgy", "Residential");
        addMarker(7.033107, 122.039460, "Santo Niño", "Brgy", "Suburban");
        addMarker(6.934311, 122.001067, "Sinunoc", "Brgy", "Coastal");
        addMarker(7.023002, 121.919332, "Sinubung", "Brgy", "Transport");
        addMarker(6.987823, 121.929789, "Talisayan", "Brgy", "Resorts");
        addMarker(6.909755, 122.112340, "Talon-Talon", "Brgy", "Commercial");
        addMarker(6.917908, 122.090897, "Tetuan", "Brgy", "Major");
        addMarker(6.919832, 122.104610, "Tugbungan", "Brgy", "Residential");
        addMarker(7.376907, 122.290041, "Vitali", "Brgy", "Northern");
        addMarker(6.941456, 122.132502, "Zambowood", "Brgy", "Peaceful");
        addMarker(7.000000, 121.921000, "San Ramon", "Brgy", "Coastal");
        addMarker(7.023359, 122.188275, "Manicahan", "Brgy", "Rural");
        addMarker(6.953136, 122.071728, "Pasonanca", "Brgy", "Park");
        addMarker(6.941437, 122.095699, "Putik", "Brgy", "Growing");
        addMarker(6.939582, 122.079434, "Tumaga", "Brgy", "Mixed");
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
            Toast.makeText(this, "Finding your location...", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        new Thread(() -> {
            try {
                // Using OSRM for actual road-based routing
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
                    double durationSeconds = route.getDouble("duration");
                    int durationMin = (int) (durationSeconds / 60);

                    List<GeoPoint> routePoints = decodePolyline(encodedPolyline);

                    runOnUiThread(() -> {
                        clearRoute();

                        // Route Polyline - Thick Blue like the image
                        currentRouteLine = new Polyline();
                        currentRouteLine.setPoints(routePoints);
                        currentRouteLine.setColor(Color.parseColor("#1A73E8")); // Google Blue
                        currentRouteLine.setWidth(16.0f);
                        mapView.getOverlays().add(currentRouteLine);

                        // Start Marker
                        startMarker = new Marker(mapView);
                        startMarker.setPosition(start);
                        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                        startMarker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.presence_online)); // Small green dot
                        mapView.getOverlays().add(startMarker);

                        // End Marker
                        endMarker = new Marker(mapView);
                        endMarker.setPosition(end);
                        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        mapView.getOverlays().add(endMarker);

                        mapView.invalidate();

                        // Display Time and Distance in overlay
                        cardDistanceOverlay.setVisibility(View.VISIBLE);
                        tvOverlayDistance.setText(String.format("%d min • %.1f km", durationMin, distance));
                        
                        // Center map on route
                        mapView.getController().animateTo(end);
                    });
                }
            } catch (Exception e) {
                Log.e("MapActivity", "Routing error", e);
                runOnUiThread(() -> {
                    Toast.makeText(MapActivity.this, "Network error. Drawing direct line.", Toast.LENGTH_SHORT).show();
                    clearRoute();
                    currentRouteLine = new Polyline();
                    currentRouteLine.addPoint(start);
                    currentRouteLine.addPoint(end);
                    currentRouteLine.setColor(Color.parseColor("#1A73E8"));
                    currentRouteLine.setWidth(10.0f);
                    mapView.getOverlays().add(currentRouteLine);
                    mapView.invalidate();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_NoTitleBar_Fullscreen);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_location_picker, null);
        builder.setView(dialogView);

        EditText etSearch = dialogView.findViewById(R.id.etSearchLocation);
        RecyclerView rvLocations = dialogView.findViewById(R.id.rvLocations);
        View btnClose = dialogView.findViewById(R.id.btnClosePicker);

        rvLocations.setLayoutManager(new LinearLayoutManager(this));

        List<String> combinedList = new ArrayList<>(locationPoints.keySet());
        Collections.sort(combinedList);

        LocationAdapter adapter = new LocationAdapter(combinedList, location -> {
            GeoPoint point = locationPoints.get(location);
            if (point != null) {
                if (targetEditText == null) {
                    prepareRoute(point, location);
                } else {
                    targetEditText.setText(location);
                    updateRouteFromInputs();
                }
            } else {
                if (targetEditText != null) {
                    targetEditText.setText(location);
                }
            }
        });
        rvLocations.setAdapter(adapter);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.ui_main_bg)));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.updateList(
                    combinedList.stream()
                        .filter(l -> l.toLowerCase().contains(s.toString().toLowerCase()))
                        .collect(Collectors.toList())
                );
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void updateRouteFromInputs() {
        String origin = etOriginMap.getText().toString();
        String dest = etDestMap.getText().toString();

        GeoPoint startPoint = null;
        GeoPoint endPoint = null;

        if (origin.equals("Current Location")) {
            if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                startPoint = myLocationOverlay.getMyLocation();
            } else {
                Toast.makeText(this, "Obtaining GPS location...", Toast.LENGTH_SHORT).show();
                return;
            }
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

        interface OnLocationSelectedListener {
            void onLocationSelected(String location);
        }

        LocationAdapter(List<String> list, OnLocationSelectedListener listener) {
            this.originalList = list;
            this.filteredList = new ArrayList<>(list);
            this.listener = listener;
        }

        void updateList(List<String> newList) {
            this.filteredList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_search, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = filteredList.get(position);
            holder.tvName.setText(name);
            holder.tvDesc.setText("Zamboanga City");
            holder.itemView.setOnClickListener(v -> {
                listener.onLocationSelected(name);
            });
        }

        @Override
        public int getItemCount() { return filteredList.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc;
            ViewHolder(View view) {
                super(view);
                tvName = view.findViewById(R.id.tvLocationName);
                tvDesc = view.findViewById(R.id.tvLocationDesc);
            }
        }
    }
}
