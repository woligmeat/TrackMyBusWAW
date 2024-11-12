package pl.creativesstudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.creativesstudio.api.WarsawApiService;
import pl.creativesstudio.models.ApiResponse;
import pl.creativesstudio.models.Bus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
    private static final long DATA_REFRESH_INTERVAL = 10000; // 10 sekund na odświeżenie


    private Map<String, Marker> activeMarkers = new HashMap<>();
    private String selectedBusId = null;
    private List<Bus> lastLoadedBuses = new ArrayList<>();
    private long lastApiCallTime = 0;
    private static final long MIN_API_CALL_INTERVAL = 5000; // Minimalny odstęp między zapytaniami (5 sekund)
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.id_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(WarsawApiService.class);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

//        LatLng initialLocation = new LatLng(52.2881717, 21.0061544);
//        currentMapCenter = initialLocation;
//        mMap.addMarker(new MarkerOptions().position(initialLocation).title("WSB Merito"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 16));
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            mMap.setMyLocationEnabled(true);
//        } else {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST_CODE);
//        }

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
                loadBusData(true);
                handler.postDelayed(this, DATA_REFRESH_INTERVAL);
            }
        };
        handler.postDelayed(runnable, DATA_REFRESH_INTERVAL);
    }

    private void updateVisibleBounds() {
        if (mMap != null) {
            visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        }
    }

    @Override
    public void onCameraIdle() {
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

        Call<ApiResponse> call = apiService.getBuses(
                RESOURCE_ID,
                API_KEY,
                1,
                null,
                null
        );

        lastApiCallTime = currentTime;

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    lastLoadedBuses = response.body().getResult();
                    List<Bus> visibleBuses = filterBusesWithinBounds(lastLoadedBuses);
                    displayBusesOnMap(visibleBuses);
                } else {
                    Toast.makeText(MainActivity.this, "Błąd podczas pobierania danych", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBusesOnMap(List<Bus> buses) {
        mMap.clear();

        LatLng initialLocation = new LatLng(52.2881717, 21.0061544);
        mMap.addMarker(new MarkerOptions().position(initialLocation).title("WSB Merito"));

        if (visibleBounds != null) {
            PolygonOptions polygonOptions = new PolygonOptions()
                    .add(new LatLng(visibleBounds.southwest.latitude, visibleBounds.southwest.longitude))
                    .add(new LatLng(visibleBounds.southwest.latitude, visibleBounds.northeast.longitude))
                    .add(new LatLng(visibleBounds.northeast.latitude, visibleBounds.northeast.longitude))
                    .add(new LatLng(visibleBounds.northeast.latitude, visibleBounds.southwest.longitude))
                    .add(new LatLng(visibleBounds.southwest.latitude, visibleBounds.southwest.longitude))
                    .strokeColor(Color.TRANSPARENT)
                    .strokeWidth(0)
                    .fillColor(Color.argb(0, 0, 0, 255));
            mMap.addPolygon(polygonOptions);
        }

        activeMarkers.clear();
        for (Bus bus : buses) {
            double lat = bus.getLat();
            double lon = bus.getLon();
            String line = bus.getLines();
            String busId = bus.getVehicleNumber();

            if (lat != 0 && lon != 0) {
                LatLng position = new LatLng(lat, lon);
                BitmapDescriptor customIcon = BitmapDescriptorFactory.fromBitmap(createCustomMarker(line));

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .icon(customIcon)
                        .title("Linia: " + line + " | Nr pojazdu: " + busId)
                        .snippet(busId);

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

    private Bitmap createCustomMarker(String lineNumber) {
        int textSize = 50;

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLUE);

        int width = 100;
        int height = 70;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        Rect bounds = new Rect();
        textPaint.getTextBounds(lineNumber, 0, lineNumber.length(), bounds);
        int x = width / 2;
        int y = (height + bounds.height()) / 2;

        canvas.drawText(lineNumber, x, y, textPaint);

        return bitmap;
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

    // Handle permission request results
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
    }
}
