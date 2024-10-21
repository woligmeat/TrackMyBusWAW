package pl.creativesstudio.models;

import com.google.gson.annotations.SerializedName;

public class Bus {

    @SerializedName("Lines")
    private String lines;

    @SerializedName("Lon")
    private double lon;

    @SerializedName("Lat")
    private double lat;

    @SerializedName("Time")
    private String time;

    @SerializedName("VehicleNumber")
    private String vehicleNumber;

    @SerializedName("Brigade")
    private String brigade;

    // Getters and Setters
    public String getLines() { return lines; }
    public void setLines(String lines) { this.lines = lines; }
    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public String getBrigade() { return brigade; }
    public void setBrigade(String brigade) { this.brigade = brigade; }
}
