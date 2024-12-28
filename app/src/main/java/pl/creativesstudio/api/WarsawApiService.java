package pl.creativesstudio.api;

import pl.creativesstudio.models.ApiResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface for interacting with Warsaw's public transport API.
 * Provides methods for fetching real-time bus and tram data.
 */
public interface WarsawApiService {

    /**
     * Fetches real-time data for buses or trams from Warsaw's public transport API.
     *
     * ### Endpoint:
     * - `GET /api/action/busestrams_get/`
     *
     * ### Parameters:
     * @param resourceId The unique resource identifier for the data endpoint.
     *                   - Required for API requests.
     * @param apiKey The API key used to authenticate requests.
     *               - Required for API requests.
     * @param type The type of vehicle to fetch:
     *             - `1` for buses.
     *             - `2` for trams.
     * @param line (Optional) The specific line to filter results by (e.g., "105").
     *             - If not provided, data for all lines is returned.
     * @param brigade (Optional) The specific brigade to filter results by.
     *                - If not provided, data for all brigades is returned.
     *
     * ### Returns:
     * @return A `Call<ApiResponse>` object representing the asynchronous HTTP request.
     *         - On success: Contains the real-time data for the requested vehicles.
     *         - On failure: Contains error details.
     *
     * ### Behavior:
     * - Sends a `GET` request to the specified API endpoint with the provided query parameters.
     * - Returns data in the form of an `ApiResponse` object.
     *
     * ### Example Usage:
     * ```java
     * WarsawApiService apiService = retrofit.create(WarsawApiService.class);
     * Call<ApiResponse> call = apiService.getBuses("resource_id_value", "api_key_value", 1, "105", null);
     * call.enqueue(new Callback<ApiResponse>() {
     *     @Override
     *     public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
     *         if (response.isSuccessful()) {
     *             ApiResponse apiResponse = response.body();
     *             // Process the bus data
     *         } else {
     *             // Handle API errors
     *         }
     *     }
     *
     *     @Override
     *     public void onFailure(Call<ApiResponse> call, Throwable t) {
     *         // Handle network errors
     *     }
     * });
     * ```
     *
     * ### Notes:
     * - The `line` and `brigade` parameters are optional; omit them to fetch data for all lines and brigades.
     * - The API key and resource ID must be valid for the request to succeed.
     */
    @GET("api/action/busestrams_get/")
    Call<ApiResponse> getBuses(
            @Query("resource_id") String resourceId,
            @Query("apikey") String apiKey,
            @Query("type") int type,
            @Query("line") String line,      // Optional
            @Query("brigade") String brigade // Optional
    );
}
