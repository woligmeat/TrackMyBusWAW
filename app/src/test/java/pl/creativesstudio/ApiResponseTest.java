package pl.creativesstudio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import pl.creativesstudio.models.ApiResponse;
import pl.creativesstudio.models.Bus;

/**
 * Unit tests for the `ApiResponse` class.
 * Verifies the behavior of the `ApiResponse` class, including:
 * - Initial state of the `result` field.
 * - Setting and retrieving the list of buses.
 * - Handling overwrites of the `result` field.
 */
class ApiResponseTest {

    /**
     * Instance of the `ApiResponse` class used for testing.
     */
    private ApiResponse apiResponse;

    /**
     * List of `Bus` objects used as test data.
     */
    private List<Bus> busList;

    /**
     * Sets up the test environment before each test.
     * - Initializes the `ApiResponse` instance.
     * - Creates a list of `Bus` objects with predefined values.
     */
    @BeforeEach
    void setUp() {
        apiResponse = new ApiResponse();
        busList = new ArrayList<>();

        Bus bus1 = new Bus();
        bus1.setLines("Bus1");
        bus1.setTime("10:00");
        busList.add(bus1);

        Bus bus2 = new Bus();
        bus2.setLines("Bus2");
        bus2.setTime("11:00");
        busList.add(bus2);
    }

    /**
     * Tests the `getResult` method when the `result` field is null.
     * Verifies that:
     * - The initial value of `result` is `null`.
     */
    @Test
    void testGetResultWhenListIsNull() {
        assertNull(apiResponse.getResult(), "The initial value of the result field should be null.");
    }

    /**
     * Tests the `setResult` method.
     * Verifies that:
     * - The `result` field is updated correctly.
     * - The `result` field is not null after setting a list.
     * - The size of the `result` list matches the input list.
     */
    @Test
    void testSetResult() {
        apiResponse.setResult(busList);
        assertNotNull(apiResponse.getResult(), "The result field should not be null after setting a list.");
        assertEquals(2, apiResponse.getResult().size(), "The result list should contain 2 elements.");
    }

    /**
     * Tests the `getResult` method.
     * Verifies that:
     * - The method returns the list previously set with `setResult`.
     * - The returned list matches the input list.
     */
    @Test
    void testGetResultReturnsCorrectList() {
        apiResponse.setResult(busList);
        List<Bus> result = apiResponse.getResult();

        assertNotNull(result, "The result should not be null.");
        assertEquals(busList, result, "The returned list should match the set list.");
    }

    /**
     * Tests that calling `setResult` overwrites the previous value of the `result` field.
     * Verifies that:
     * - The size of the `result` list reflects the new value.
     * - The data in the `result` list matches the new value.
     */
    @Test
    void testSetResultOverwritesPreviousValue() {
        List<Bus> newBusList = new ArrayList<>();
        Bus newBus = new Bus();
        newBus.setLines("Bus3");
        newBus.setTime("12:00");
        newBusList.add(newBus);

        apiResponse.setResult(busList);
        apiResponse.setResult(newBusList);

        assertEquals(1, apiResponse.getResult().size(), "The result list should contain one element after overwriting.");
        assertEquals("Bus3", apiResponse.getResult().get(0).getLines(), "The bus line name should be 'Bus3'.");
    }
}