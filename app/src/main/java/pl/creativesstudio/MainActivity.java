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

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.List;

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
        ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Initialize WarsawApiService
    private WarsawApiService apiService;

    // Stałe dla API
    private static final String BASE_URL = "https://api.um.warszawa.pl/";
    private static final String API_KEY = "3fb6fadd-9c21-43fc-998b-c41cc14663ff";
    private static final String RESOURCE_ID = "f2e5503e-927d-4ad3-9500-4ab9e55deb59"; // Upewnij się, że to jest poprawny resource_id

    // Handler do aktualizacji danych co 10 sekund
    private Handler handler = new Handler();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.id_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configure Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(WarsawApiService.class);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Set initial location and marker
        LatLng initialLocation = new LatLng(52.2881717, 21.0061544);
        mMap.addMarker(new MarkerOptions().position(initialLocation).title("WSB Merito"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 12));

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            // Request permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Map UI settings
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Location listener
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        // Load bus data initially
        loadBusData();

        // Ustawienie aktualizacji co 10 sekund
        runnable = new Runnable() {
            @Override
            public void run() {
                loadBusData();
                handler.postDelayed(this, 10000); // 10 sekund
            }
        };
        handler.postDelayed(runnable, 10000); // Pierwsze wywołanie po 10 sekundach
    }

    // Method to fetch data from API
    private void loadBusData() {
        // Wywołanie API z resource_id, apiKey i type=1 (autobusy)
        Call<ApiResponse> call = apiService.getBuses(
                RESOURCE_ID,
                API_KEY,
                1,
                null,    // Możesz podać konkretny numer linii, np. "219"
                null     // Możesz podać konkretny numer brygady, np. "1"
        );

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Bus> buses = response.body().getResult();
                    displayBusesOnMap(buses);
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

    // Method to display buses on the map
    private void displayBusesOnMap(List<Bus> buses) {
        // Clear existing markers except dla "WSB Merito"
        mMap.clear();
        LatLng initialLocation = new LatLng(52.2881717, 21.0061544);
        mMap.addMarker(new MarkerOptions().position(initialLocation).title("WSB Merito"));

        for (Bus bus : buses) {
            double lat = bus.getLat();
            double lon = bus.getLon();
            String line = bus.getLines();

            if (lat != 0 && lon != 0) {
                LatLng position = new LatLng(lat, lon);

                // Create a custom marker with the bus line number
                BitmapDescriptor customIcon = BitmapDescriptorFactory.fromBitmap(createCustomMarker(line));

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .icon(customIcon)
                        .title("Linia: " + line + " | Nr pojazdu: " + bus.getVehicleNumber());

                mMap.addMarker(markerOptions);
            }
        }
    }

    // Method to create custom marker bitmap
    private Bitmap createCustomMarker(String lineNumber) {
        // Adjust text size based on map zoom level if needed
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

        // Center the text
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
        handler.removeCallbacks(runnable);
    }
}
