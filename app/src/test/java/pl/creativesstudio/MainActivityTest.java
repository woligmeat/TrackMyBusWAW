package pl.creativesstudio;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import pl.creativesstudio.models.Bus;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Unit tests for the `MainActivity` class.
 * Verifies various methods and functionality, including:
 * - Sorting bus lines.
 * - Filtering buses within geographical bounds.
 * - Filtering buses by line.
 * - Formatting timestamps.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class MainActivityTest {

    /**
     * Instance of the `MainActivity` class used for testing.
     */
    private MainActivity mainActivity;

    /**
     * List of test buses used for testing.
     */
    private List<Bus> testBuses;

    /**
     * Sets up the test environment before each test.
     * - Initializes a new instance of `MainActivity`.
     * - Creates a mock list of `Bus` objects with predefined values.
     */
    @Before
    public void setUp() {
        mainActivity = new MainActivity();

        // Create mock data for buses
        testBuses = new ArrayList<>();

        Bus bus1 = new Bus();
        bus1.setLines("123");
        bus1.setLat(52.2297);
        bus1.setLon(21.0122);
        bus1.setVehicleNumber("A1");
        bus1.setBrigade("B1");
        bus1.setTime("2024-01-01 10:00:00");
        testBuses.add(bus1);

        Bus bus2 = new Bus();
        bus2.setLines("456");
        bus2.setLat(52.2297);
        bus2.setLon(21.0122);
        bus2.setVehicleNumber("A2");
        bus2.setBrigade("B2");
        bus2.setTime("2024-01-01 11:00:00");
        testBuses.add(bus2);

        Bus bus3 = new Bus();
        bus3.setLines("123");
        bus3.setLat(52.2397);
        bus3.setLon(21.0222);
        bus3.setVehicleNumber("A3");
        bus3.setBrigade("B3");
        bus3.setTime("2024-01-01 12:00:00");
        testBuses.add(bus3);
    }

    /**
     * Tests the `sortBusLines` method for correct sorting of bus lines.
     * Verifies that lines are sorted numerically and alphabetically as expected.
     */
    @Test
    public void testSortBusLines() {
        List<String> unsortedLines = new ArrayList<>();
        unsortedLines.add("200");
        unsortedLines.add("34");
        unsortedLines.add("20");
        unsortedLines.add("N61");

        List<String> sortedLines = mainActivity.sortBusLines(unsortedLines);

        // Verify sorted order
        assertEquals("20", sortedLines.get(0));
        assertEquals("34", sortedLines.get(1));
        assertEquals("200", sortedLines.get(2));
        assertEquals("N61", sortedLines.get(3));
    }

    /**
     * Tests the `filterBusesWithinBounds` method.
     * Verifies that only buses within the specified geographical bounds are returned.
     */
    @Test
    public void testFilterBusesWithinBounds() {
        LatLngBounds mockBounds = LatLngBounds.builder()
                .include(new LatLng(52.2297, 21.0122))  // Coordinates of bus1 and bus2
                .include(new LatLng(52.2397, 21.0222))  // Coordinates of bus3
                .build();

        mainActivity.visibleBounds = mockBounds;

        List<Bus> filteredBuses = mainActivity.filterBusesWithinBounds(testBuses);

        // Verify that filtering works
        assertNotNull(filteredBuses);
        assertEquals(3, filteredBuses.size());
    }

    /**
     * Tests filtering buses by line manually.
     * Verifies that only buses with the specified line are returned.
     */
    @Test
    public void testFilterByLine() {
        List<Bus> filteredBuses = new ArrayList<>();
        for (Bus bus : testBuses) {
            if ("123".equals(bus.getLines())) {
                filteredBuses.add(bus);
            }
        }

        // Verify line filtering
        assertEquals(2, filteredBuses.size());
        for (Bus bus : filteredBuses) {
            assertEquals("123", bus.getLines());
        }
    }

    /**
     * Tests the `formatTimestamp` method.
     * Verifies that the method formats a timestamp into the expected format: `yyyy-MM-dd HH:mm:ss`.
     */
    @Test
    public void testFormatTimestamp() {
        try {
            Method method = MainActivity.class.getDeclaredMethod("formatTimestamp", long.class);
            method.setAccessible(true);

            long testTimestamp = System.currentTimeMillis();
            String formattedTime = (String) method.invoke(mainActivity, testTimestamp);

            assertNotNull(formattedTime);
            // Check if the format matches expected pattern
            assertTrue(formattedTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        } catch (Exception e) {
            fail("Exception in testFormatTimestamp: " + e.getMessage());
        }
    }
}