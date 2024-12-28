package pl.creativesstudio.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents the response structure for data fetched from Warsaw's public transport API.
 * This class maps the JSON response to a Java object for easier data handling.
 */
public class ApiResponse {

    /**
     * A list of `Bus` objects containing the real-time data for buses or trams.
     * This field is mapped from the `result` key in the API's JSON response.
     */
    @SerializedName("result")
    private List<Bus> result;

    /**
     * Retrieves the list of buses or trams from the API response.
     *
     * @return A `List<Bus>` containing the real-time data.
     *         - Each `Bus` object represents a single bus or tram with its respective details.
     */
    public List<Bus> getResult() {
        return result;
    }

    /**
     * Sets the list of buses or trams in the API response.
     *
     * @param result A `List<Bus>` to set as the API response data.
     *               - This method is typically used when parsing or modifying the response data.
     */
    public void setResult(List<Bus> result) {
        this.result = result;
    }
}