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

class WarsawApiServiceTest {

    private WarsawApiService warsawApiService;
    private Call<ApiResponse> mockCall;

    @BeforeEach
    void setUp() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.um.warszawa.pl/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        warsawApiService = retrofit.create(WarsawApiService.class);
        mockCall = mock(Call.class);
    }

    @Test
    void testGetBusesWithRequiredParameters() {
        WarsawApiService service = mock(WarsawApiService.class);
        String resourceId = "exampleResourceId";
        String apiKey = "exampleApiKey";
        int type = 1;

        when(service.getBuses(resourceId, apiKey, type, null, null)).thenReturn(mockCall);
        Call<ApiResponse> call = service.getBuses(resourceId, apiKey, type, null, null);

        assertNotNull(call, "Wywołanie uslugi powinno zwrocic Call<ApiResponse>");
        verify(service).getBuses(resourceId, apiKey, type, null, null);
    }

    @Test
    void testGetBusesWithAllParameters() {
        WarsawApiService service = mock(WarsawApiService.class);
        String resourceId = "exampleResourceId";
        String apiKey = "exampleApiKey";
        int type = 1;
        String line = "123";
        String brigade = "A1";

        when(service.getBuses(resourceId, apiKey, type, line, brigade)).thenReturn(mockCall);
        Call<ApiResponse> call = service.getBuses(resourceId, apiKey, type, line, brigade);

        assertNotNull(call, "Wywołanie uslugi powinno zwrocic Call<ApiResponse>");
        verify(service).getBuses(resourceId, apiKey, type, line, brigade);
    }
}
