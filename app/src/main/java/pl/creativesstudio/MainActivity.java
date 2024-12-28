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


/**
 * Main activity for the bus tracking application.
 * This activity manages the Google Map and handles user interactions,
 * such as displaying bus locations, managing permissions, and updating map data.
 *
 * ### Inheritance and Interfaces:
 * - Extends `AppCompatActivity`: Provides compatibility support for modern Android features.
 * - Implements the following interfaces:
 *   - `OnMapReadyCallback`: Handles initialization and setup of the Google Map when it is ready.
 *   - `GoogleMap.OnMyLocationButtonClickListener`: Responds to clicks on the "My Location" button.
 *   - `GoogleMap.OnMyLocationClickListener`: Handles clicks on the user's location dot on the map.
 *   - `GoogleMap.OnCameraIdleListener`: Responds to camera movement and idle states to update map data.
 *   - `ActivityCompat.OnRequestPermissionsResultCallback`: Handles the results of runtime permission requests.
 *
 * ### Responsibilities:
 * - Displays a Google Map and manages map-related features such as zooming, panning, and markers.
 * - Handles user location permissions and displays the user's location on the map.
 * - Fetches and displays real-time bus data using the Warsaw API.
 * - Updates map data dynamically based on user interactions and zoom levels.
 * - Provides feedback and interaction capabilities via toast messages and custom markers.
 *
 * ### Key Components:
 * - **Google Maps Integration**:
 *   - Displays a map with real-time updates.
 *   - Customizes map markers with bus line information.
 * - **Permission Management**:
 *   - Requests and processes location permissions at runtime.
 * - **Event Listeners**:
 *   - Handles user interactions with the map and updates visible data accordingly.
 * - **Background Operations**:
 *   - Uses an `ExecutorService` to fetch bus data from the API asynchronously.
 *
 * ### Notes:
 * - This activity is the central point of interaction for the user.
 * - Ensure all lifecycle methods are properly implemented to handle resource cleanup and state management.
 */
public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnCameraIdleListener,
        ActivityCompat.OnRequestPermissionsResultCallback {


    /**
     * Google Map instance used to interact with the map displayed in the activity.
     */
    private GoogleMap mMap;

    /**
     * Request code for location permissions.
     * Used to identify the result of the location permission request.
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Current center position of the map.
     * Updated whenever the camera position changes.
     */
    public LatLng currentMapCenter;

    /**
     * Visible bounds of the map.
     * Represents the geographical area currently visible on the map.
     */
    public LatLngBounds visibleBounds;

    /**
     * Retrofit API service for interacting with Warsaw's public transport API.
     * Used to fetch real-time bus data.
     */
    private WarsawApiService apiService;

    /**
     * Base URL for the Warsaw public transport API.
     */
    private static final String BASE_URL = "https://api.um.warszawa.pl/";

    /**
     * API key for authenticating requests to the Warsaw public transport API.
     */
    private static final String API_KEY = "3fb6fadd-9c21-43fc-998b-c41cc14663ff";

    /**
     * Resource ID for the specific API endpoint used to fetch bus data.
     */
    private static final String RESOURCE_ID = "f2e5503e-927d-4ad3-9500-4ab9e55deb59";

    /**
     * Handler for scheduling and managing periodic tasks.
     */
    private Handler handler = new Handler();

    /**
     * Runnable for periodic updates of bus data.
     */
    private Runnable runnable;

    /**
     * Runnable for delayed map updates after camera movement.
     */
    private Runnable mapUpdateRunnable;

    /**
     * Delay for map updates in milliseconds.
     */
    private static final long MAP_UPDATE_DELAY = 1000;

    /**
     * High zoom data refresh interval in milliseconds.
     */
    private static final long DATA_REFRESH_INTERVAL_HIGH_ZOOM = 5000;

    /**
     * Low zoom data refresh interval in milliseconds.
     */
    private static final long DATA_REFRESH_INTERVAL_LOW_ZOOM = 15000;

    /**
     * Default data refresh interval in milliseconds.
     */
    private static final long DATA_REFRESH_INTERVAL_DEFAULT = 10000;

    /**
     * Minimum interval between API calls in milliseconds.
     */
    private static final long MIN_API_CALL_INTERVAL = 5000;

    /**
     * Minimum zoom level required to display buses on the map.
     */
    private static final float MIN_ZOOM_LEVEL = 14.0f;

    /**
     * Map of active markers for displayed buses.
     * The key is the bus ID, and the value is the corresponding map marker.
     */
    private Map<String, Marker> activeMarkers = new HashMap<>();

    /**
     * ID of the currently selected bus.
     * Used to identify which bus marker's info window should be displayed.
     */
    private String selectedBusId = null;

    /**
     * List of buses last loaded from the API.
     * Represents the most recent dataset fetched from the API.
     */
    private List<Bus> lastLoadedBuses = new ArrayList<>();

    /**
     * Timestamp of the last successful API call in milliseconds.
     * Used to manage the interval between consecutive API requests.
     */
    private long lastApiCallTime = 0;

    /**
     * Flag indicating whether this is the first data load for the map.
     * Ensures that initial data loading is handled correctly.
     */
    private boolean isInitialLoad = true;

    /**
     * Executor service for managing background operations, such as API calls.
     */
    private ExecutorService executorService;

    /**
     * State indicating whether a specific bus line is selected.
     * If `true`, only buses from the selected line are displayed on the map.
     */
    private boolean lineSelected = false;

    /**
     * Complete list of all buses fetched from the API.
     * Used to support filtering and redisplaying buses as needed.
     */
    private List<Bus> allBuses = new ArrayList<>();


    /**
     * Called when the activity is first created.
     * Initializes the UI components, Google Map, Retrofit API client, and other required services.
     *
     * ### Functionality:
     * - Sets up the main layout for the activity.
     * - Configures UI elements, such as menu and location buttons, with their respective listeners.
     * - Initializes the Google Map using `SupportMapFragment`.
     * - Sets up Retrofit for API communication with Warsaw's public transport API.
     * - Creates an `ExecutorService` for managing background tasks.
     *
     * ### Parameters:
     * @param savedInstanceState A `Bundle` object containing the activity's previously saved state, if available.
     *                           Can be `null` if no state was saved.
     *
     * ### Behavior:
     * - When the "Menu" button is clicked, a bottom sheet dialog is displayed with bus line options.
     * - When the "Current Location" button is clicked, the user's last known location is retrieved and the map animates to center on it.
     *   - If location permissions are missing, a toast message informs the user.
     * - Initializes the map fragment to asynchronously load the Google Map.
     * - Prepares the `Retrofit` client for future API calls to fetch real-time bus data.
     *
     * ### Usage:
     * - This method is automatically called by the Android framework when the activity is created.
     *
     * ### Preconditions:
     * - The activity's layout (`R.layout.activity_main`) must contain the required UI elements (e.g., `button_menu`, `button_current_location`, `id_map`).
     * - Location permissions should be handled appropriately for "Current Location" functionality.
     *
     * ### Postconditions:
     * - The activity is fully initialized with UI components and background services ready to handle user interactions.
     *
     * ### Example:
     * ```java
     * @Override
     * protected void onCreate(Bundle savedInstanceState) {
     *     super.onCreate(savedInstanceState);
     *     // Activity setup
     * }
     * ```
     *
     * ### Notes:
     * - The Google Map setup relies on `onMapReady` being triggered asynchronously after the map is initialized.
     * - Location permissions are checked and managed dynamically.
     * - Ensure proper cleanup of the `ExecutorService` in `onDestroy` to prevent memory leaks.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call the parent implementation
        super.onCreate(savedInstanceState);

        // Set the main layout for the activity
        setContentView(R.layout.activity_main);

        // Setup menu button with a click listener to show the bottom sheet dialog
        ImageButton buttonMenu = findViewById(R.id.button_menu);
        buttonMenu.setOnClickListener(v -> showBottomSheetWithLines());

        // Setup the current location button
        ImageButton buttonCurrentLocation = findViewById(R.id.button_current_location);
        buttonCurrentLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                // Get the user's last known location and move the map camera
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
                fusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Nie udało się pobrać lokalizacji użytkownika", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Notify the user about missing location permissions
                Toast.makeText(MainActivity.this, "Brak uprawnień do pobrania lokalizacji", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize the map fragment and set up the map asynchronously
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.id_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize Retrofit for API communication
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(WarsawApiService.class);

        // Create a single-threaded executor service for background tasks
        executorService = Executors.newSingleThreadExecutor();
    }


    /**
     * Displays a bottom sheet dialog containing a list of bus lines for filtering.
     * Allows the user to select a specific bus line or view all buses.
     *
     * ### Functionality:
     * - Creates a bottom sheet dialog and inflates its layout.
     * - Populates the dialog with a list of bus lines, including an option to show all buses.
     * - Sorts the bus lines alphabetically for easier navigation.
     * - Handles user selection to update the map based on the chosen bus line.
     *
     * ### Behavior:
     * - If the user selects "SHOW ALL BUSES":
     *   - Clears the filter, displays all buses, and centers the map on the current location.
     * - If the user selects a specific bus line:
     *   - Filters the buses to display only those from the selected line.
     *   - Adjusts the map view to focus on the buses from the selected line.
     *
     * ### Example:
     * ```java
     * // Show the bottom sheet dialog with bus lines
     * showBottomSheetWithLines();
     * ```
     *
     * ### Usage:
     * - Typically invoked when the user clicks a button to filter bus lines.
     *
     * ### Preconditions:
     * - The layout file `bottom_sheet_lines` must exist and include a `RecyclerView` with the ID `recycler_view_lines`.
     * - The list of all buses (`allBuses`) should be populated.
     *
     * ### Postconditions:
     * - Displays a bottom sheet dialog to the user.
     * - Updates the map to reflect the selected bus line.
     *
     * ### Notes:
     * - The list of bus lines is dynamically generated from the `allBuses` list to ensure it reflects real-time data.
     * - The `sortBusLines` method is used to alphabetize the bus lines for better usability.
     */
    void showBottomSheetWithLines() {
        // Create a new bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_lines, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Set up the RecyclerView for displaying bus lines
        RecyclerView recyclerView = bottomSheetView.findViewById(R.id.recycler_view_lines);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Prepare the list of bus lines
        List<String> busLines = new ArrayList<>();
        busLines.add("SHOW ALL BUSES"); // Add the default option to show all buses

        // Add unique bus lines from the `allBuses` list
        for (Bus bus : allBuses) {
            if (!busLines.contains(bus.getLines())) {
                busLines.add(bus.getLines());
            }
        }

        // Sort the bus lines alphabetically (excluding the first entry)
        List<String> linesToSort = busLines.subList(1, busLines.size());
        linesToSort = sortBusLines(linesToSort);
        for (int i = 1; i < busLines.size(); i++) {
            busLines.set(i, linesToSort.get(i - 1));
        }

        // Set up the RecyclerView adapter
        BusLinesAdapter adapter = new BusLinesAdapter(busLines, line -> {
            bottomSheetDialog.dismiss(); // Dismiss the bottom sheet dialog on selection

            if (line.equals("SHOW ALL BUSES")) {
                // Handle the "SHOW ALL BUSES" option
                lineSelected = false;
                lastLoadedBuses = new ArrayList<>(allBuses);
                Toast.makeText(MainActivity.this, "Selected: SHOW ALL BUSES", Toast.LENGTH_SHORT).show();
                if (currentMapCenter != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentMapCenter, 15f));
                }
                displayBusesOnMap(lastLoadedBuses);
            } else {
                // Handle a specific bus line selection
                Toast.makeText(MainActivity.this, "Selected line: " + line, Toast.LENGTH_SHORT).show();
                lineSelected = true;
                filterAndZoomToLine(line);
            }
        });

        // Attach the adapter to the RecyclerView
        recyclerView.setAdapter(adapter);

        // Show the bottom sheet dialog
        bottomSheetDialog.show();
    }

    /**
     * Sorts a list of bus lines alphabetically and numerically.
     * The sorting order is as follows:
     * - Lines are first sorted by their alphabetic part (e.g., "A", "B", etc.).
     * - If the alphabetic parts are the same, lines are sorted numerically by their numeric part (e.g., "10", "15").
     *
     * ### Functionality:
     * - Extracts the numeric and alphabetic parts of each line.
     * - Compares lines based on their alphabetic and numeric components.
     * - If a line contains no numeric part, it is sorted alphabetically.
     *
     * ### Parameters:
     * @param lines A `List<String>` containing bus line identifiers to be sorted.
     *
     * ### Returns:
     * @return A sorted `List<String>` where bus lines are ordered alphabetically and numerically.
     *
     * ### Behavior:
     * - Lines such as "A1" and "A2" are sorted as "A1", "A2".
     * - Lines with different alphabetic prefixes (e.g., "A", "B") are sorted alphabetically.
     * - Lines without numeric parts are placed based on their alphabetic value.
     *
     * ### Example:
     * ```java
     * List<String> busLines = Arrays.asList("B15", "A2", "B10", "A1", "B");
     * List<String> sortedLines = sortBusLines(busLines);
     * // Result: ["A1", "A2", "B", "B10", "B15"]
     * ```
     *
     * ### Usage:
     * - Typically used to sort bus line identifiers for display in the UI.
     *
     * ### Notes:
     * - Assumes that all input strings are valid bus line identifiers containing optional numeric and alphabetic parts.
     * - The method modifies the input list directly and also returns it for convenience.
     */
    List<String> sortBusLines(List<String> lines) {
        // Sort the lines using a custom comparator
        lines.sort((line1, line2) -> {
            // Extract numeric and alphabetic parts of each line
            String numberPart1 = line1.replaceAll("[^0-9]", ""); // Numeric part of line1
            String numberPart2 = line2.replaceAll("[^0-9]", ""); // Numeric part of line2
            String letterPart1 = line1.replaceAll("[0-9]", "");  // Alphabetic part of line1
            String letterPart2 = line2.replaceAll("[0-9]", "");  // Alphabetic part of line2

            // Compare alphabetic parts first
            int letterComparison = letterPart1.compareTo(letterPart2);
            if (letterComparison != 0) {
                return letterComparison; // Return result if alphabetic parts differ
            }

            // Compare numeric parts if both are non-empty
            if (!numberPart1.isEmpty() && !numberPart2.isEmpty()) {
                return Integer.compare(Integer.parseInt(numberPart1), Integer.parseInt(numberPart2));
            }

            // Fallback to lexicographical comparison if numeric parts are empty
            return line1.compareTo(line2);
        });

        return lines; // Return the sorted list
    }


    /**
     * Filters buses by the specified line and adjusts the map to focus on their locations.
     * Displays only the buses matching the provided line on the map and zooms the camera to include all of them.
     *
     * ### Functionality:
     * - Filters the `allBuses` list to include only buses that belong to the specified line.
     * - Displays the filtered buses on the map using `displayBusesOnMap`.
     * - Adjusts the map's camera to focus on the geographical bounds of the filtered buses.
     * - Notifies the user if no buses are available for the specified line.
     *
     * ### Parameters:
     * @param line A `String` representing the bus line to filter (e.g., "105", "A1").
     *
     * ### Behavior:
     * - If no buses match the specified line, a toast message is displayed, and the method exits.
     * - If buses are found:
     *   - Only those buses are displayed on the map.
     *   - The map camera animates to include all the filtered buses within its bounds.
     *
     * ### Preconditions:
     * - The Google Map instance (`mMap`) must be initialized.
     * - The `allBuses` list must not be empty.
     *
     * ### Postconditions:
     * - The map displays only the buses matching the specified line.
     * - The camera view adjusts to show all filtered buses.
     *
     * ### Example:
     * ```java
     * // Focus on buses from line "105"
     * filterAndZoomToLine("105");
     * ```
     *
     * ### Notes:
     * - The method uses a `LatLngBounds.Builder` to calculate the bounds of the filtered buses.
     * - A padding of 100 pixels is applied when adjusting the camera view.
     * - Assumes that buses in the `allBuses` list have valid latitude and longitude coordinates.
     */
    void filterAndZoomToLine(String line) {
        // Exit if the map is not initialized or there are no buses available
        if (mMap == null || allBuses.isEmpty()) return;

        // Filter buses that match the specified line
        List<Bus> filteredBuses = new ArrayList<>();
        for (Bus bus : allBuses) {
            if (bus.getLines().equals(line)) {
                filteredBuses.add(bus);
            }
        }

        // Notify the user if no buses match the specified line
        if (filteredBuses.isEmpty()) {
            Toast.makeText(this, "No buses available for line: " + line, Toast.LENGTH_SHORT).show();
            return;
        }

        // Display the filtered buses on the map
        displayBusesOnMap(filteredBuses);

        // Calculate the bounds of the filtered buses and adjust the map's camera
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (Bus bus : filteredBuses) {
            boundsBuilder.include(new LatLng(bus.getLat(), bus.getLon()));
        }
        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    /**
     * Called when the Google Map is ready to be used.
     * Initializes the map with necessary settings, permissions, and event listeners.
     * Displays the user's location or a default location and sets up periodic updates for real-time data.
     *
     * @param googleMap The Google Map instance provided by the `OnMapReadyCallback`.
     *
     * ### Functionality:
     * - Checks and requests location permissions.
     * - Enables user location on the map if permissions are granted.
     * - Sets the camera to the user's last known location or a default location.
     * - Configures map UI settings such as gestures, zoom controls, compass, and location button.
     * - Adds listeners for map interactions:
     *   - `OnCameraIdleListener`: Updates data when the camera stops moving.
     *   - `OnMyLocationButtonClickListener`: Handles clicks on the "My Location" button.
     *   - `OnMyLocationClickListener`: Displays user's current location details.
     *   - `OnMarkerClickListener`: Handles clicks on map markers and selects a bus based on the marker.
     * - Periodically updates visible data based on zoom level and refresh intervals.
     *
     * ### Permissions:
     * - Requests `Manifest.permission.ACCESS_FINE_LOCATION` if not already granted.
     * - Displays a default location (`52.2881717, 21.0061544`) if the location is unavailable or permissions are denied.
     *
     * ### Event Listeners:
     * - `OnCameraIdleListener`: Updates the visible map bounds and triggers data loading.
     * - `OnMarkerClickListener`: Selects a bus based on marker clicks.
     *
     * ### Periodic Updates:
     * - Data refresh intervals vary based on zoom level:
     *   - High zoom level: 5 seconds.
     *   - Low zoom level: 15 seconds.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // Assign the map instance
        mMap = googleMap;

        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Enable user's location
            mMap.setMyLocationEnabled(true);

            // Retrieve the user's last known location
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Move the camera to the user's location
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            currentMapCenter = currentLocation;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        } else {
                            // Fallback to a default location
                            LatLng defaultLocation = new LatLng(52.2881717, 21.0061544);
                            currentMapCenter = defaultLocation;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
                        }
                    });
        } else {
            // Request location permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);

            // Default location as fallback
            LatLng defaultLocation = new LatLng(52.2881717, 21.0061544);
            currentMapCenter = defaultLocation;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));
        }

        // Configure map UI settings
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Set listeners for map interactions
        mMap.setOnCameraIdleListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        // Set a listener for marker clicks
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getSnippet() != null) {
                selectedBusId = marker.getSnippet(); // Save the selected bus ID
            }
            return false; // Allow default behavior
        });

        // Update visible bounds and load initial data
        updateVisibleBounds();
        loadBusData(true);

        // Setup periodic updates based on zoom level
        runnable = new Runnable() {
            @Override
            public void run() {
                float currentZoom = mMap.getCameraPosition().zoom;
                if (currentZoom >= MIN_ZOOM_LEVEL) {
                    // Refresh data more frequently at high zoom levels
                    loadBusData(true);
                    handler.postDelayed(this, DATA_REFRESH_INTERVAL_HIGH_ZOOM);
                } else {
                    // Less frequent refresh at low zoom levels
                    handler.postDelayed(this, DATA_REFRESH_INTERVAL_LOW_ZOOM);
                }
            }
        };
        handler.postDelayed(runnable, DATA_REFRESH_INTERVAL_HIGH_ZOOM);
    }

    /**
     * Updates the visible bounds of the map.
     * This method retrieves the currently visible region of the Google Map
     * and saves it as a `LatLngBounds` object for use in filtering and displaying data.
     *
     * ### Purpose:
     * - To determine the geographical area currently visible to the user on the map.
     * - Enables filtering data (e.g., buses) within the visible region.
     *
     * ### Usage:
     * - Called when the map's camera position changes, such as after zooming or panning.
     * - Ensures that data displayed on the map corresponds to the visible region.
     *
     * ### Preconditions:
     * - The Google Map instance (`mMap`) must not be null.
     *
     * ### Postconditions:
     * - The `visibleBounds` variable is updated with the new bounds of the visible region.
     * - If the map instance is null, no update is performed.
     */
    private void updateVisibleBounds() {
        if (mMap != null) {
            // Retrieve the visible region and store its bounds
            visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        }
    }

    /**
     * Callback triggered when the camera on the Google Map becomes idle.
     * This method is called after the user stops interacting with the map,
     * such as panning, zooming, or rotating.
     *
     * ### Functionality:
     * - Updates the center position of the map (`currentMapCenter`).
     * - Updates the visible bounds of the map by calling `updateVisibleBounds()`.
     * - Schedules a delayed task to refresh the map data using `updateMapWithCurrentData()`.
     *
     * ### Behavior:
     * - If the Google Map instance (`mMap`) is null, the method exits immediately.
     * - Cancels any previously scheduled updates to prevent redundant execution.
     * - Schedules a new runnable with a delay defined by `MAP_UPDATE_DELAY` to allow smooth user interaction.
     *
     * ### Usage:
     * - This method is automatically triggered by the Google Maps API when the camera becomes stationary.
     *
     * ### Preconditions:
     * - The Google Map instance (`mMap`) must be initialized and not null.
     *
     * ### Postconditions:
     * - Updates the map's visible bounds and center position.
     * - Ensures that map data is refreshed after the camera becomes idle.
     */
    @Override
    public void onCameraIdle() {
        if (mMap == null) return; // Exit if the map is not initialized

        // Update the map's center position
        currentMapCenter = mMap.getCameraPosition().target;

        // Update the visible region bounds
        updateVisibleBounds();

        // Cancel any pending updates to avoid duplication
        if (mapUpdateRunnable != null) {
            handler.removeCallbacks(mapUpdateRunnable);
        }

        // Schedule a new runnable to update the map data
        mapUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateMapWithCurrentData(); // Refresh map data
            }
        };

        // Schedule the runnable with a delay
        handler.postDelayed(mapUpdateRunnable, MAP_UPDATE_DELAY);
    }

    /**
     * Updates the map with the current data based on the visible region and zoom level.
     * This method ensures that only relevant data is displayed on the map
     * and triggers data refresh if necessary.
     *
     * ### Functionality:
     * - Checks if the map instance (`mMap`) is initialized.
     * - Skips updates if a specific bus line is currently selected.
     * - Logs and evaluates the current zoom level:
     *   - If the zoom level is below the defined threshold (`MIN_ZOOM_LEVEL`), all markers are cleared.
     *   - Otherwise, filters and displays buses visible within the current map bounds.
     * - Ensures data freshness by checking the time elapsed since the last API call.
     *   - Triggers a new API call if sufficient time has passed or if it's the initial load.
     *
     * ### Behavior:
     * - Removes markers when the zoom level is too low to display meaningful data.
     * - Dynamically updates markers for buses visible within the current bounds.
     * - Manages API calls efficiently to avoid redundant or excessive requests.
     *
     * ### Usage:
     * - Called periodically or when the map's camera becomes idle to update displayed data.
     *
     * ### Preconditions:
     * - The Google Map instance (`mMap`) must be initialized.
     *
     * ### Postconditions:
     * - Markers for buses are updated based on visibility and zoom level.
     * - API calls are made as needed to refresh data.
     *
     * ### Logging:
     * - Logs the current zoom level and actions taken (e.g., clearing markers).
     */
    private void updateMapWithCurrentData() {
        // Exit if the map is not initialized
        if (mMap == null) return;

        // Skip updates if a specific bus line is selected
        if (lineSelected) return;

        // Retrieve the current zoom level and log it
        float currentZoom = mMap.getCameraPosition().zoom;
        Log.d("ZoomLevel", "Current zoom level: " + currentZoom);

        // Clear markers if the zoom level is below the threshold
        if (currentZoom < MIN_ZOOM_LEVEL) {
            Log.d("ZoomLevel", "Zoom below threshold. Clearing markers.");
            for (Marker marker : activeMarkers.values()) {
                marker.remove();
            }
            activeMarkers.clear();
            return;
        }

        // Display buses visible within the current bounds
        if (!lastLoadedBuses.isEmpty()) {
            List<Bus> visibleBuses = filterBusesWithinBounds(lastLoadedBuses);
            displayBusesOnMap(visibleBuses);
        }

        // Check if a new API call is needed based on elapsed time or initial load
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastApiCallTime >= MIN_API_CALL_INTERVAL || isInitialLoad) {
            loadBusData(false);
            isInitialLoad = false;
        }
    }

    /**
     * Filters the list of buses to include only those within the current visible bounds of the map.
     *
     * ### Functionality:
     * - Checks if the visible bounds (`visibleBounds`) of the map are set.
     * - Iterates through all buses and determines if each bus's location is within the bounds.
     * - Returns a list of buses whose locations are inside the visible region of the map.
     *
     * ### Parameters:
     * @param allBuses The list of all buses to be filtered. Each bus has latitude and longitude coordinates.
     *
     * ### Returns:
     * @return A list of buses (`List<Bus>`) that are within the current visible bounds of the map.
     *         Returns an empty list if the `visibleBounds` is not set or no buses are within the bounds.
     *
     * ### Preconditions:
     * - The `visibleBounds` field must be set before this method is called.
     * - The `allBuses` list must not be null (though it can be empty).
     *
     * ### Postconditions:
     * - The returned list contains only buses located within the map's visible region.
     *
     * ### Example:
     * If the map shows a specific area of the city, only buses in that area will be included in the result.
     */
    List<Bus> filterBusesWithinBounds(List<Bus> allBuses) {
        List<Bus> visibleBuses = new ArrayList<>();

        // Check if the visible bounds of the map are set
        if (visibleBounds != null) {
            // Iterate through all buses and check if their location is within the bounds
            for (Bus bus : allBuses) {
                LatLng busLocation = new LatLng(bus.getLat(), bus.getLon());
                if (visibleBounds.contains(busLocation)) {
                    visibleBuses.add(bus); // Add the bus to the result list if it's within bounds
                }
            }
        }

        // Return the list of visible buses
        return visibleBuses;
    }

    /**
     * Loads bus data from the API and updates the map with the retrieved information.
     * Ensures efficient data loading by minimizing unnecessary API calls and handling errors gracefully.
     *
     * ### Functionality:
     * - Prevents redundant API calls by enforcing a minimum interval (`MIN_API_CALL_INTERVAL`) between requests unless forced.
     * - Retrieves visible map bounds and uses them to filter data if the API supports boundary-based queries.
     * - Fetches bus data asynchronously using `executorService` and updates the UI on the main thread.
     * - Handles API errors, network issues, and empty results by showing appropriate messages and fallback data.
     *
     * ### Parameters:
     * @param forced A boolean flag indicating whether to force an API call regardless of the minimum interval.
     *               - `true`: Forces an API call.
     *               - `false`: Skips the call if the interval condition is not met.
     *
     * ### Preconditions:
     * - The `visibleBounds` must be set; otherwise, no data is loaded.
     * - The API client (`apiService`) must be initialized.
     *
     * ### Postconditions:
     * - Updates the `allBuses` and `lastLoadedBuses` lists with the retrieved data.
     * - Updates the `lastApiCallTime` with the timestamp of the successful API call.
     * - Displays markers on the map for the visible buses.
     * - If an error occurs, displays appropriate messages and retains the last loaded data if available.
     *
     * ### API Behavior:
     * - If boundary-based queries are supported by the API, only data within the current map bounds is fetched.
     * - Otherwise, all data is fetched, and filtering is done locally.
     *
     * ### Error Handling:
     * - Handles API response errors by showing a toast and retaining the last successfully loaded data.
     * - Handles network errors by falling back to previously loaded data or displaying an error message if no data is available.
     *
     * ### Logging:
     * - Logs errors and timestamps for debugging and monitoring.
     */
    private void loadBusData(boolean forced) {
        // Get the current time
        long currentTime = System.currentTimeMillis();

        // Skip the API call if not forced and the minimum interval has not elapsed
        if (!forced && currentTime - lastApiCallTime < MIN_API_CALL_INTERVAL) {
            return;
        }

        // Skip if visible bounds are not set
        if (visibleBounds == null) {
            return;
        }

        // Extract the boundaries of the visible region
        double minLat = visibleBounds.southwest.latitude;
        double maxLat = visibleBounds.northeast.latitude;
        double minLon = visibleBounds.southwest.longitude;
        double maxLon = visibleBounds.northeast.longitude;

        // Execute the API call asynchronously
        executorService.execute(() -> {
            try {
                // If boundary-based queries are supported, use the commented-out code below:
                // Call<ApiResponse> call = apiService.getBusesWithinBounds(
                //         RESOURCE_ID,
                //         API_KEY,
                //         minLat,
                //         maxLat,
                //         minLon,
                //         maxLon
                // );

                // Otherwise, fetch all data and filter locally
                Call<ApiResponse> call = apiService.getBuses(
                        RESOURCE_ID,
                        API_KEY,
                        1,
                        null,
                        null
                );

                // Execute the API call
                Response<ApiResponse> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    List<Bus> result = response.body().getResult();

                    // Update the bus lists
                    allBuses = result;
                    lastLoadedBuses = new ArrayList<>(result);

                    if (result == null || result.isEmpty()) {
                        // No new data: show previously loaded data
                        runOnUiThread(() -> {
                            if (!lastLoadedBuses.isEmpty()) {
                                Toast.makeText(MainActivity.this,
                                        "No new data. Showing last loaded data from: "
                                                + formatTimestamp(lastApiCallTime),
                                        Toast.LENGTH_LONG).show();
                                displayBusesOnMap(lastLoadedBuses);
                            } else {
                                Toast.makeText(MainActivity.this, "No data to display.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        // Successfully retrieved new data
                        lastLoadedBuses = result;
                        lastApiCallTime = currentTime;

                        runOnUiThread(() -> {
                            List<Bus> visibleBuses = filterBusesWithinBounds(lastLoadedBuses);
                            displayBusesOnMap(visibleBuses);
                        });
                    }
                } else {
                    // API response error: fallback to previously loaded data
                    runOnUiThread(() -> {
                        if (!lastLoadedBuses.isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    "API error. Showing last loaded data from: "
                                            + formatTimestamp(lastApiCallTime),
                                    Toast.LENGTH_LONG).show();
                            displayBusesOnMap(lastLoadedBuses);
                        } else {
                            Toast.makeText(MainActivity.this, "API error and no data to display.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                // Network error: fallback to previously loaded data
                runOnUiThread(() -> {
                    if (!lastLoadedBuses.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                "Connection error. Showing last loaded data from: "
                                        + formatTimestamp(lastApiCallTime),
                                Toast.LENGTH_LONG).show();
                        displayBusesOnMap(lastLoadedBuses);
                    } else {
                        Toast.makeText(MainActivity.this, "Connection error and no data to display.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    /**
     * Formats a given timestamp into a human-readable date and time string.
     *
     * ### Functionality:
     * - Converts a timestamp (in milliseconds since the epoch) into a `java.util.Date` object.
     * - Formats the date object into a string with the pattern `yyyy-MM-dd HH:mm:ss`.
     * - Returns the formatted date-time string.
     *
     * ### Parameters:
     * @param timestamp The timestamp to format, in milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     * ### Returns:
     * @return A string representing the formatted date and time in the format `yyyy-MM-dd HH:mm:ss`.
     *
     * ### Usage:
     * - Used for displaying timestamps in a user-friendly format, such as for logging or showing last update times.
     *
     * ### Example:
     * ```java
     * long exampleTimestamp = 1672531199000L; // Equivalent to 2023-12-31 23:59:59
     * String formattedDate = formatTimestamp(exampleTimestamp);
     * // formattedDate: "2023-12-31 23:59:59"
     * ```
     *
     * ### Notes:
     * - The method uses the `java.text.SimpleDateFormat` class for formatting, which is not thread-safe.
     * - Ensure thread-safety if using this method in a multi-threaded context by synchronizing access or using `java.time` classes instead.
     */
    private String formatTimestamp(long timestamp) {
        // Create a SimpleDateFormat instance with the desired pattern
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Convert the timestamp into a Date object
        java.util.Date date = new java.util.Date(timestamp);

        // Format the Date object into a human-readable string
        return sdf.format(date);
    }


    /**
     * Displays a list of buses as markers on the Google Map.
     *
     * ### Functionality:
     * - Clears all existing markers from the map and the `activeMarkers` collection.
     * - Iterates through the list of buses and adds a marker for each bus with valid coordinates.
     * - Each marker is customized with the bus's line number and vehicle ID.
     * - If a specific bus is selected (`selectedBusId`), its marker's info window is displayed automatically.
     *
     * ### Parameters:
     * @param buses A list of `Bus` objects representing the buses to display on the map.
     *              Each `Bus` object must provide latitude, longitude, line number, and vehicle ID.
     *
     * ### Preconditions:
     * - The Google Map instance (`mMap`) must be initialized and not null.
     * - The `buses` list must not be null (though it can be empty, which results in clearing the map).
     *
     * ### Postconditions:
     * - All existing markers are removed from the map.
     * - New markers for the provided buses are added to the map and stored in `activeMarkers`.
     * - If `selectedBusId` matches a bus in the list, its marker's info window is displayed.
     *
     * ### Behavior:
     * - Ignores buses with invalid coordinates (`lat = 0` or `lon = 0`).
     * - Markers are anchored at the bottom center for proper alignment on the map.
     *
     * ### Example:
     * ```java
     * List<Bus> buses = new ArrayList<>();
     * buses.add(new Bus(52.2297, 21.0122, "105", "12345"));
     * buses.add(new Bus(52.2298, 21.0123, "150", "12346"));
     * displayBusesOnMap(buses);
     * // Displays two markers on the map for buses with line numbers "105" and "150".
     * ```
     *
     * ### Notes:
     * - The map is cleared before adding new markers to avoid overlapping or stale data.
     * - Uses a helper method `createCustomMarker()` to generate custom icons for the markers.
     */
    private void displayBusesOnMap(List<Bus> buses) {
        // Exit if the map instance is not initialized
        if (mMap == null) return;

        // Clear existing markers from the map
        mMap.clear();

        // Clear the active markers collection
        activeMarkers.clear();

        // Iterate through the list of buses and add markers for each valid bus
        for (Bus bus : buses) {
            double lat = bus.getLat();
            double lon = bus.getLon();
            String line = bus.getLines();
            String busId = bus.getVehicleNumber();

            // Skip buses with invalid coordinates
            if (lat != 0 && lon != 0) {
                LatLng position = new LatLng(lat, lon);

                // Create a custom marker icon for the bus line
                BitmapDescriptor icon = createCustomMarker(line);

                // Configure the marker options
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .title("Line: " + line + " | Vehicle ID: " + busId)
                        .snippet(busId)
                        .icon(icon)
                        .anchor(0.5f, 1f); // Anchor at the bottom center for proper alignment

                // Add the marker to the map
                Marker marker = mMap.addMarker(markerOptions);

                // Add the marker to the active markers collection
                if (marker != null) {
                    activeMarkers.put(busId, marker);

                    // Show the info window for the selected bus
                    if (busId.equals(selectedBusId)) {
                        marker.showInfoWindow();
                    }
                }
            }
        }
    }

    /**
     * Creates a custom marker icon for a bus line to be displayed on the Google Map.
     *
     * ### Functionality:
     * - Generates a custom bitmap containing the bus line number and a pin icon.
     * - Draws a background rectangle for the text and places the bus line number at the center.
     * - Positions a pin icon below the text to complete the custom marker design.
     *
     * ### Parameters:
     * @param line The bus line number (e.g., "105") to display on the custom marker.
     *
     * ### Returns:
     * @return A `BitmapDescriptor` representing the custom marker icon for use on the map.
     *
     * ### Behavior:
     * - Dynamically calculates the size of the marker based on the length of the bus line text.
     * - Ensures that the text and pin icon are properly aligned and visually balanced.
     *
     * ### Preconditions:
     * - The provided bus line (`line`) should not be null or empty.
     * - The drawable resource `R.drawable.ic_marker_icon` must exist and be a valid pin icon.
     *
     * ### Postconditions:
     * - Returns a `BitmapDescriptor` that can be used to customize markers on the map.
     *
     * ### Example:
     * ```java
     * String line = "105";
     * BitmapDescriptor markerIcon = createCustomMarker(line);
     * // The returned BitmapDescriptor can now be used to set a marker icon.
     * ```
     *
     * ### Notes:
     * - The text is drawn with anti-aliasing to ensure smooth rendering.
     * - The pin icon is anchored at the bottom-center of the marker for proper alignment.
     * - This method uses `ContextCompat.getDrawable()` to retrieve the drawable resource, ensuring compatibility with various Android versions.
     *
     * ### Potential Improvements:
     * - Adjust text size dynamically based on the length of the line for better scalability.
     * - Allow customization of colors or fonts through additional parameters.
     */
    private BitmapDescriptor createCustomMarker(String line) {
        // Paint for drawing the text with anti-aliasing
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(50); // Text size
        textPaint.setColor(Color.BLACK); // Text color
        textPaint.setTextAlign(Paint.Align.CENTER); // Center align text

        // Paint for drawing the background rectangle
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE); // Background color

        // Measure the dimensions of the text
        Rect textBounds = new Rect();
        textPaint.getTextBounds(line, 0, line.length(), textBounds);

        int textWidth = textBounds.width() + 20; // Add padding to the width
        int textHeight = textBounds.height() + 20; // Add padding to the height

        // Pin dimensions
        int pinWidth = 124;
        int pinHeight = 212;

        // Calculate the final bitmap dimensions
        int width = Math.max(textWidth, pinWidth); // Ensure the width fits the text or pin
        int height = textHeight + pinHeight; // Combine the text and pin heights

        // Create a bitmap to draw the marker
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw the background rectangle for the text
        canvas.drawRect(0, 0, width, textHeight, backgroundPaint);

        // Draw the line number text at the center
        canvas.drawText(line, width / 2, textHeight - 10, textPaint);

        // Draw the pin icon below the text
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker_icon);
        if (drawable != null) {
            drawable.setBounds((width - pinWidth) / 2, textHeight, (width + pinWidth) / 2, height);
            drawable.draw(canvas);
        }

        // Convert the bitmap into a BitmapDescriptor for use with Google Maps
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Callback triggered when the user's location is clicked on the map.
     * Displays a toast message with the user's current location details.
     *
     * ### Functionality:
     * - Retrieves the current location of the user as a `Location` object.
     * - Displays the location's details (latitude, longitude, and potentially other information) in a toast message.
     *
     * ### Parameters:
     * @param location A `Location` object representing the user's current location.
     *                 - Contains details such as latitude, longitude, accuracy, and more.
     *
     * ### Behavior:
     * - This method is triggered by the Google Maps API when the user clicks on the "My Location" dot on the map.
     * - Shows a short toast message with a string representation of the location.
     *
     * ### Usage:
     * - Provides feedback to the user about their current location.
     * - Can be extended to include additional functionality, such as logging or navigation.
     *
     * ### Example:
     * ```java
     * // Assume the user's location is (52.2297, 21.0122)
     * onMyLocationClick(location);
     * // Toast output: "Obecna lokalizacja:
     * // Location[latitude=52.2297, longitude=21.0122]"
     * ```
     *
     * ### Notes:
     * - Ensure that location permissions are granted and the user's location is enabled on the map.
     * - The toast displays the `toString()` representation of the `Location` object, which may include additional details.
     */
    @Override
    public void onMyLocationClick(@NonNull Location location) {
        // Display the user's current location in a toast message
        Toast.makeText(this, "Obecna lokalizacja:\n" + location, Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback triggered when the "My Location" button on the map is clicked.
     * Displays a toast message to indicate that the location detection process has started.
     *
     * ### Functionality:
     * - Provides feedback to the user when the "My Location" button is clicked.
     * - Allows the map to center on the user's location if default behavior is enabled.
     *
     * ### Returns:
     * @return A boolean indicating whether the event is consumed:
     *         - `false`: Allows the default behavior of the "My Location" button to occur (centering the map on the user's location).
     *         - `true`: Consumes the event and prevents the default behavior.
     *
     * ### Behavior:
     * - When the user clicks the "My Location" button, a toast message saying "Wykrywanie lokalizacji" is displayed.
     * - By returning `false`, the map will automatically center on the user's current location.
     *
     * ### Usage:
     * - Provides visual feedback for the user's action and retains the default behavior of centering the map.
     * - Can be extended to include additional functionality, such as logging or analytics.
     *
     * ### Example:
     * ```java
     * // User clicks the "My Location" button
     * boolean handled = onMyLocationButtonClick();
     * // Toast output: "Wykrywanie lokalizacji"
     * // The map centers on the user's location.
     * ```
     *
     * ### Notes:
     * - Ensure location permissions are granted and the user's location is enabled on the map for the button to function correctly.
     * - Returning `false` ensures that the default behavior of the button is preserved.
     */
    @Override
    public boolean onMyLocationButtonClick() {
        // Display a toast message indicating location detection
        Toast.makeText(this, "Wykrywanie lokalizacji", Toast.LENGTH_SHORT).show();

        // Return false to allow the default behavior (centering the map on the user's location)
        return false;
    }

    /**
     * Callback triggered when the result of a permission request is received.
     * Handles the user's response to the location permission request.
     *
     * ### Functionality:
     * - Checks whether the requested permission (`ACCESS_FINE_LOCATION`) was granted.
     * - Enables the user's location on the map if the permission is granted.
     * - Displays a toast message if the permission is denied.
     *
     * ### Parameters:
     * @param requestCode An integer identifying the permission request. This should match the `LOCATION_PERMISSION_REQUEST_CODE`.
     * @param permissions An array of requested permissions. In this case, it should include `Manifest.permission.ACCESS_FINE_LOCATION`.
     * @param grantResults An array of results corresponding to the requested permissions:
     *                     - `PackageManager.PERMISSION_GRANTED` if the permission was granted.
     *                     - `PackageManager.PERMISSION_DENIED` if the permission was denied.
     *
     * ### Behavior:
     * - If the `requestCode` matches `LOCATION_PERMISSION_REQUEST_CODE`:
     *   - Checks the `grantResults` array to see if the location permission was granted.
     *   - Enables the user's location on the map if the permission was granted.
     *   - Displays a toast message if the permission was denied.
     * - Ignores other `requestCode` values, allowing parent implementations to handle them.
     *
     * ### Usage:
     * - This method is called automatically when the user responds to a permission request initiated by the app.
     *
     * ### Preconditions:
     * - A permission request for `ACCESS_FINE_LOCATION` must have been initiated using `ActivityCompat.requestPermissions`.
     *
     * ### Postconditions:
     * - The user's location is enabled on the map if the permission is granted.
     * - A toast message is displayed if the permission is denied.
     *
     * ### Example:
     * ```java
     * // User grants location permission
     * onRequestPermissionsResult(1, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
     *     new int[]{PackageManager.PERMISSION_GRANTED});
     * // The user's location is enabled on the map.
     * ```
     *
     * ### Notes:
     * - Ensure that the `LOCATION_PERMISSION_REQUEST_CODE` is consistent with the one used in the permission request.
     * - This method checks permissions again using `ContextCompat.checkSelfPermission` for additional safety.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Call the parent implementation
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Handle the result of the location permission request
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, enable user's location on the map
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                // Permission was denied, show a toast message
                Toast.makeText(this, "Uprawnienia nie zostały przyznane", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Lifecycle method called when the activity is being destroyed.
     * Performs cleanup operations to release resources and prevent memory leaks.
     *
     * ### Functionality:
     * - Removes all pending callbacks from the `handler` to stop scheduled tasks.
     * - Cancels and shuts down the `executorService` if it is active, ensuring no background threads remain running.
     * - Calls the parent implementation to handle additional cleanup.
     *
     * ### Behavior:
     * - Ensures that no background tasks or callbacks remain active when the activity is destroyed.
     * - Prevents memory leaks by releasing references to handlers and background executors.
     *
     * ### Usage:
     * - This method is called automatically when the activity is finishing or being destroyed by the system.
     * - Common use cases include closing resources, stopping threads, or canceling tasks.
     *
     * ### Preconditions:
     * - The activity should have initialized the `handler` and `executorService` for background operations.
     *
     * ### Postconditions:
     * - All scheduled tasks and background threads are stopped and cleaned up.
     *
     * ### Example:
     * ```java
     * @Override
     * protected void onDestroy() {
     *     super.onDestroy();
     *     // Cleanup operations to prevent memory leaks
     * }
     * ```
     *
     * ### Notes:
     * - Always ensure that background threads or tasks are properly stopped in `onDestroy`.
     * - If additional resources are allocated during the activity's lifecycle, they should also be released here.
     */
    @Override
    protected void onDestroy() {
        // Call the parent implementation
        super.onDestroy();

        // Remove all pending callbacks from the handler
        if (handler != null) {
            handler.removeCallbacks(runnable); // Remove periodic map updates
            if (mapUpdateRunnable != null) {
                handler.removeCallbacks(mapUpdateRunnable); // Remove delayed map updates
            }
        }

        // Shutdown the executor service to stop background threads
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}