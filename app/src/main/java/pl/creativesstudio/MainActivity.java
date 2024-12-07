package pl.creativesstudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.creativesstudio.api.WarsawApiService;
import pl.creativesstudio.models.ApiResponse;
import pl.creativesstudio.models.Bus;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnCameraIdleListener,
        ActivityCompat.OnRequestPermissionsResultCallback {


    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private LatLng currentMapCenter;
    private LatLngBounds visibleBounds;

    private WarsawApiService apiService;
    private static final String BASE_URL = "https://api.um.warszawa.pl/";
    private static final String API_KEY = "3fb6fadd-9c21-43fc-998b-c41cc14663ff";
    private static final String RESOURCE_ID = "f2e5503e-927d-4ad3-9500-4ab9e55deb59";

    private Handler handler = new Handler();
    private Runnable runnable;
    private Runnable mapUpdateRunnable;
    private static final long MAP_UPDATE_DELAY = 1000; // 1 sekunda opóźnienia
    private static final long DATA_REFRESH_INTERVAL_HIGH_ZOOM = 5000; // 5 sekund
    private static final long DATA_REFRESH_INTERVAL_LOW_ZOOM = 15000; // 15 sekund
    private static final long DATA_REFRESH_INTERVAL_DEFAULT = 10000; // 10 sekund
    private static final long MIN_API_CALL_INTERVAL = 5000; // Minimalny odstęp między zapytaniami (5 sekund)

    private static final float MIN_ZOOM_LEVEL = 14.0f; // Minimalny poziom powiększenia

    private Map<String, Marker> activeMarkers = new HashMap<>();
    private String selectedBusId = null;
    private List<Bus> lastLoadedBuses = new ArrayList<>();
    private long lastApiCallTime = 0;
    private boolean isInitialLoad = true;

    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obsługa przycisku menu
        ImageButton buttonMenu = findViewById(R.id.button_menu);
        buttonMenu.setOnClickListener(v -> showBottomSheetWithLines());


        // Inicjalizacja mapy
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.id_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicjalizacja Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(WarsawApiService.class);

        executorService = Executors.newSingleThreadExecutor();
    }

    private void showBottomSheetWithLines() {
        // Utwórz BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_lines, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Pobierz RecyclerView z układu
        RecyclerView recyclerView = bottomSheetView.findViewById(R.id.recycler_view_lines);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Przygotuj dane (linie autobusowe)
        List<String> busLines = new ArrayList<>();
        for (Bus bus : lastLoadedBuses) {
            if (!busLines.contains(bus.getLines())) {
                busLines.add(bus.getLines());
            }
        }

        // Posortuj linie
        busLines = sortBusLines(busLines);

        // Adapter do wyświetlania linii
        BusLinesAdapter adapter = new BusLinesAdapter(busLines, line -> {
            Toast.makeText(MainActivity.this, "Wybrano linię: " + line, Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
            filterAndZoomToLine(line); // Wywołanie metody dopasowującej mapę do wybranej linii
        });

        recyclerView.setAdapter(adapter);

        // Pokaż dialog
        bottomSheetDialog.show();
    }

    private List<String> sortBusLines(List<String> lines) {
        // Niestandardowy komparator dla linii autobusowych
        lines.sort((line1, line2) -> {
            // Rozdziel numery i litery w liniach
            String numberPart1 = line1.replaceAll("[^0-9]", ""); // Wyodrębnij część numeryczną
            String numberPart2 = line2.replaceAll("[^0-9]", "");

            String letterPart1 = line1.replaceAll("[0-9]", ""); // Wyodrębnij część literową
            String letterPart2 = line2.replaceAll("[0-9]", "");

            // Najpierw porównaj część literową (alfabetycznie)
            int letterComparison = letterPart1.compareTo(letterPart2);
            if (letterComparison != 0) {
                return letterComparison;
            }

            // Jeśli litery są takie same, porównaj numery (liczbowo)
            if (!numberPart1.isEmpty() && !numberPart2.isEmpty()) {
                return Integer.compare(Integer.parseInt(numberPart1), Integer.parseInt(numberPart2));
            }

            // W przypadku braku numerów porównaj pełne linie (zapewnia stabilność)
            return line1.compareTo(line2);
        });

        return lines;
    }

    private void filterAndZoomToLine(String line) {
        if (mMap == null || lastLoadedBuses.isEmpty()) return;

        // Lista autobusów dla wybranej linii
        List<Bus> filteredBuses = new ArrayList<>();
        for (Bus bus : lastLoadedBuses) {
            if (bus.getLines().equals(line)) {
                filteredBuses.add(bus);
            }
        }

        if (filteredBuses.isEmpty()) {
            Toast.makeText(this, "Brak autobusów dla linii: " + line, Toast.LENGTH_SHORT).show();
            return;
        }

        // Usuń obecne markery i dodaj tylko dla wybranej linii
        mMap.clear();
        activeMarkers.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (Bus bus : filteredBuses) {
            LatLng position = new LatLng(bus.getLat(), bus.getLon());
            BitmapDescriptor icon = createCustomMarker(line);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title("Linia: " + line + " | Nr pojazdu: " + bus.getVehicleNumber())
                    .icon(icon)
                    .anchor(0.5f, 1f);

            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                activeMarkers.put(bus.getVehicleNumber(), marker);
                boundsBuilder.include(position);
            }
        }

        // Dostosuj kamerę, aby objąć wszystkie autobusy danej linii
        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            currentMapCenter = currentLocation;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        } else {
                            LatLng defaultLocation = new LatLng(52.2881717, 21.0061544);
                            currentMapCenter = defaultLocation;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);

            LatLng defaultLocation = new LatLng(52.2881717, 21.0061544);
            currentMapCenter = defaultLocation;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
        }

        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.setOnCameraIdleListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getSnippet() != null) {
                selectedBusId = marker.getSnippet();
            }
            return false;
        });

        updateVisibleBounds();
        loadBusData(true);

        runnable = new Runnable() {
            @Override
            public void run() {
                float currentZoom = mMap.getCameraPosition().zoom;
                if (currentZoom >= MIN_ZOOM_LEVEL) {
                    loadBusData(true);
                    handler.postDelayed(this, DATA_REFRESH_INTERVAL_HIGH_ZOOM);
                } else {
                    handler.postDelayed(this, DATA_REFRESH_INTERVAL_LOW_ZOOM);
                }
            }
        };
        handler.postDelayed(runnable, DATA_REFRESH_INTERVAL_HIGH_ZOOM);
    }

    private void updateVisibleBounds() {
        if (mMap != null) {
            visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        }
    }

    @Override
    public void onCameraIdle() {
        if (mMap == null) return;

        currentMapCenter = mMap.getCameraPosition().target;
        updateVisibleBounds();

        if (mapUpdateRunnable != null) {
            handler.removeCallbacks(mapUpdateRunnable);
        }

        mapUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateMapWithCurrentData();
            }
        };
        handler.postDelayed(mapUpdateRunnable, MAP_UPDATE_DELAY);
    }

    private void updateMapWithCurrentData() {
        if (mMap == null) return;

        float currentZoom = mMap.getCameraPosition().zoom;
        Log.d("ZoomLevel", "Aktualny poziom zoomu: " + currentZoom);

        if (currentZoom < MIN_ZOOM_LEVEL) {
            Log.d("ZoomLevel", "Zoom poniżej progu. Usuwanie markerów.");
            // Usuń markery
            for (Marker marker : activeMarkers.values()) {
                marker.remove();
            }
            activeMarkers.clear();
            return;
        }

        if (!lastLoadedBuses.isEmpty()) {
            List<Bus> visibleBuses = filterBusesWithinBounds(lastLoadedBuses);
            displayBusesOnMap(visibleBuses);
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastApiCallTime >= MIN_API_CALL_INTERVAL || isInitialLoad) {
            loadBusData(false);
            isInitialLoad = false;
        }
    }

    private List<Bus> filterBusesWithinBounds(List<Bus> allBuses) {
        List<Bus> visibleBuses = new ArrayList<>();
        if (visibleBounds != null) {
            for (Bus bus : allBuses) {
                LatLng busLocation = new LatLng(bus.getLat(), bus.getLon());
                if (visibleBounds.contains(busLocation)) {
                    visibleBuses.add(bus);
                }
            }
        }
        return visibleBuses;
    }

    private void loadBusData(boolean forced) {
        long currentTime = System.currentTimeMillis();
        if (!forced && currentTime - lastApiCallTime < MIN_API_CALL_INTERVAL) {
            return;
        }

        if (visibleBounds == null) {
            return; // Nie ma dostępnych granic, więc nie ładuj danych
        }

        double minLat = visibleBounds.southwest.latitude;
        double maxLat = visibleBounds.northeast.latitude;
        double minLon = visibleBounds.southwest.longitude;
        double maxLon = visibleBounds.northeast.longitude;

        executorService.execute(() -> {
            try {
                // Jeśli API wspiera pobieranie na podstawie granic, użyj poniższego wywołania
                // Call<ApiResponse> call = apiService.getBusesWithinBounds(
                //         RESOURCE_ID,
                //         API_KEY,
                //         minLat,
                //         maxLat,
                //         minLon,
                //         maxLon
                // );

                // Jeśli API nie wspiera, użyj istniejącego wywołania
                Call<ApiResponse> call = apiService.getBuses(
                        RESOURCE_ID,
                        API_KEY,
                        1,
                        null,
                        null
                );

                Response<ApiResponse> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    List<Bus> result = response.body().getResult();

                    if (result == null || result.isEmpty()) {
                        // Brak danych: Wyświetl ostatnio pobrane dane
                        runOnUiThread(() -> {
                            if (!lastLoadedBuses.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Brak nowych danych. Wyświetlam ostatnio pobrane dane z czasu: "
                                        + formatTimestamp(lastApiCallTime), Toast.LENGTH_LONG).show();
                                displayBusesOnMap(lastLoadedBuses);
                            } else {
                                Toast.makeText(MainActivity.this, "Brak danych do wyświetlenia.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        // Nowe dane zostały poprawnie pobrane
                        lastLoadedBuses = result;
                        lastApiCallTime = currentTime;

                        runOnUiThread(() -> {
                            List<Bus> visibleBuses = filterBusesWithinBounds(lastLoadedBuses);
                            displayBusesOnMap(visibleBuses);
                        });
                    }
                } else {
                    // Błąd w odpowiedzi API: Wyświetl ostatnio pobrane dane
                    runOnUiThread(() -> {
                        if (!lastLoadedBuses.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Błąd API. Wyświetlam ostatnio pobrane dane z czasu: "
                                    + formatTimestamp(lastApiCallTime), Toast.LENGTH_LONG).show();
                            displayBusesOnMap(lastLoadedBuses);
                        } else {
                            Toast.makeText(MainActivity.this, "Błąd API i brak danych do wyświetlenia.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                // Błąd sieci: Wyświetl ostatnio pobrane dane
                runOnUiThread(() -> {
                    if (!lastLoadedBuses.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Błąd połączenia. Wyświetlam ostatnio pobrane dane z czasu: "
                                + formatTimestamp(lastApiCallTime), Toast.LENGTH_LONG).show();
                        displayBusesOnMap(lastLoadedBuses);
                    } else {
                        Toast.makeText(MainActivity.this, "Błąd połączenia i brak danych do wyświetlenia.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date date = new java.util.Date(timestamp);
        return sdf.format(date);
    }

    private void displayBusesOnMap(List<Bus> buses) {
        if (mMap == null) return;

        // Najpierw wyczyść istniejące markery
        mMap.clear();

        // Dodaj marker początkowy
        LatLng initialLocation = new LatLng(52.2881717, 21.0061544);
        mMap.addMarker(new MarkerOptions().position(initialLocation).title("WSB Merito"));

        activeMarkers.clear();
        for (Bus bus : buses) {
            double lat = bus.getLat();
            double lon = bus.getLon();
            String line = bus.getLines();
            String busId = bus.getVehicleNumber();

            if (lat != 0 && lon != 0) {
                LatLng position = new LatLng(lat, lon);
                BitmapDescriptor icon = createCustomMarker(line);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title("Linia: " + line + " | Nr pojazdu: " + busId)
                        .snippet(busId)
                        .icon(icon)
                        .anchor(0.5f, 1f); // Ustawienie kotwicy, aby marker był poprawnie wyświetlany

                Marker marker = mMap.addMarker(markerOptions);
                if (marker != null) {
                    activeMarkers.put(busId, marker);
                    if (busId.equals(selectedBusId)) {
                        marker.showInfoWindow();
                    }
                }
            }
        }
    }

    private BitmapDescriptor createCustomMarker(String line) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(50);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);

        Rect textBounds = new Rect();
        textPaint.getTextBounds(line, 0, line.length(), textBounds);

        int textWidth = textBounds.width() + 20;
        int textHeight = textBounds.height() + 20;

        // Rozmiar pinezki
        int pinWidth = 124;
        int pinHeight = 212;

        int width = Math.max(textWidth, pinWidth);
        int height = textHeight + pinHeight;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Rysuj tło dla tekstu
        canvas.drawRect(0, 0, width, textHeight, backgroundPaint);

        // Rysuj tekst w centrum
        canvas.drawText(line, width / 2, textHeight - 10, textPaint);

        // Rysuj ikonę pinezki poniżej tekstu
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker_icon);
        if (drawable != null) {
            drawable.setBounds((width - pinWidth) / 2, textHeight, (width + pinWidth) / 2, height);
            drawable.draw(canvas);
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Obecna lokalizacja:\n" + location, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "Wykrywanie lokalizacji", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Uprawnienia nie zostały przyznane", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(runnable);
            if (mapUpdateRunnable != null) {
                handler.removeCallbacks(mapUpdateRunnable);
            }
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}