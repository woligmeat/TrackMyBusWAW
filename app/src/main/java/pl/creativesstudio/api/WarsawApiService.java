package pl.creativesstudio.api;

import pl.creativesstudio.models.ApiResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WarsawApiService {
    @GET("api/action/busestrams_get/")
    Call<ApiResponse> getBuses(
            @Query("resource_id") String resourceId,
            @Query("apikey") String apiKey,
            @Query("type") int type,
            @Query("line") String line,      // Opcjonalny
            @Query("brigade") String brigade // Opcjonalny
    );
}
