package pl.creativesstudio.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiResponse {
    @SerializedName("result")
    private List<Bus> result;

    public List<Bus> getResult() { return result; }
    public void setResult(List<Bus> result) { this.result = result; }
}
