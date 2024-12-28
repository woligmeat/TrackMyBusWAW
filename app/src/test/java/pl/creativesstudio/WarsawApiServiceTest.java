package pl.creativesstudio;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.creativesstudio.api.WarsawApiService;
import pl.creativesstudio.models.ApiResponse;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Unit tests for the `WarsawApiService` interface.
 * Verifies the correct behavior of API calls made using Retrofit.
 */
class WarsawApiServiceTest {

    /**
     * Instance of the `WarsawApiService` interface.
     */
    private WarsawApiService warsawApiService;

    /**
     * Mocked Retrofit `Call` object for simulating API responses.
     */
    private Call<ApiResponse> mockCall;

    /**
     * Sets up the test environment before each test.
     * - Initializes a Retrofit instance for creating a `WarsawApiService` instance.
     * - Creates a mock `Call<ApiResponse>` object for use in tests.
     */
    @BeforeEach
    void setUp() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.um.warszawa.pl/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        warsawApiService = retrofit.create(WarsawApiService.class);
        mockCall = mock(Call.class);
    }

    /**
     * Tests the `getBuses` method with only the required parameters.
     * Verifies that:
     * - The service returns a non-null `Call<ApiResponse>` object.
     * - The method is invoked with the correct parameters.
     */
    @Test
    void testGetBusesWithRequiredParameters() {
        WarsawApiService service = mock(WarsawApiService.class);
        String resourceId = "exampleResourceId";
        String apiKey = "exampleApiKey";
        int type = 1;

        // Simulate the API call
        when(service.getBuses(resourceId, apiKey, type, null, null)).thenReturn(mockCall);

        // Call the method and verify the results
        Call<ApiResponse> call = service.getBuses(resourceId, apiKey, type, null, null);
        assertNotNull(call, "The service call should return a Call<ApiResponse> instance.");
        verify(service).getBuses(resourceId, apiKey, type, null, null);
    }

    /**
     * Tests the `getBuses` method with all parameters provided.
     * Verifies that:
     * - The service returns a non-null `Call<ApiResponse>` object.
     * - The method is invoked with the correct parameters.
     */
    @Test
    void testGetBusesWithAllParameters() {
        WarsawApiService service = mock(WarsawApiService.class);
        String resourceId = "exampleResourceId";
        String apiKey = "exampleApiKey";
        int type = 1;
        String line = "123";
        String brigade = "A1";

        // Simulate the API call
        when(service.getBuses(resourceId, apiKey, type, line, brigade)).thenReturn(mockCall);

        // Call the method and verify the results
        Call<ApiResponse> call = service.getBuses(resourceId, apiKey, type, line, brigade);
        assertNotNull(call, "The service call should return a Call<ApiResponse> instance.");
        verify(service).getBuses(resourceId, apiKey, type, line, brigade);
    }
}