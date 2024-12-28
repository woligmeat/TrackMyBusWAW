package pl.creativesstudio.models;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single bus or tram's real-time data retrieved from Warsaw's public transport API.
 * This class maps JSON fields to Java object properties for easier access and manipulation.
 */
public class Bus {

    /**
     * The bus or tram's line identifier (e.g., "105", "A1").
     * Mapped from the JSON key `Lines`.
     */
    @SerializedName("Lines")
    private String lines;

    /**
     * The longitude of the bus or tram's current location.
     * Mapped from the JSON key `Lon`.
     */
    @SerializedName("Lon")
    private double lon;

    /**
     * The latitude of the bus or tram's current location.
     * Mapped from the JSON key `Lat`.
     */
    @SerializedName("Lat")
    private double lat;

    /**
     * The timestamp of the bus or tram's last reported location.
     * Mapped from the JSON key `Time`.
     * Format: `yyyy-MM-dd HH:mm:ss` (e.g., "2024-12-27 14:30:00").
     */
    @SerializedName("Time")
    private String time;

    /**
     * The unique identifier for the bus or tram.
     * Mapped from the JSON key `VehicleNumber`.
     */
    @SerializedName("VehicleNumber")
    private String vehicleNumber;

    /**
     * The brigade number assigned to the bus or tram.
     * Mapped from the JSON key `Brigade`.
     */
    @SerializedName("Brigade")
    private String brigade;

    // Getters and Setters

    /**
     * Retrieves the line identifier of the bus or tram.
     *
     * @return A `String` representing the line (e.g., "105").
     */
    public String getLines() {
        return lines;
    }

    /**
     * Sets the line identifier of the bus or tram.
     *
     * @param lines A `String` representing the line to set.
     */
    public void setLines(String lines) {
        this.lines = lines;
    }

    /**
     * Retrieves the longitude of the bus or tram's location.
     *
     * @return A `double` representing the longitude.
     */
    public double getLon() {
        return lon;
    }

    /**
     * Sets the longitude of the bus or tram's location.
     *
     * @param lon A `double` representing the longitude to set.
     */
    public void setLon(double lon) {
        this.lon = lon;
    }

    /**
     * Retrieves the latitude of the bus or tram's location.
     *
     * @return A `double` representing the latitude.
     */
    public double getLat() {
        return lat;
    }

    /**
     * Sets the latitude of the bus or tram's location.
     *
     * @param lat A `double` representing the latitude to set.
     */
    public void setLat(double lat) {
        this.lat = lat;
    }

    /**
     * Retrieves the timestamp of the bus or tram's last reported location.
     *
     * @return A `String` representing the timestamp.
     */
    public String getTime() {
        return time;
    }

    /**
     * Sets the timestamp of the bus or tram's last reported location.
     *
     * @param time A `String` representing the timestamp to set.
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * Retrieves the unique vehicle number of the bus or tram.
     *
     * @return A `String` representing the vehicle number.
     */
    public String getVehicleNumber() {
        return vehicleNumber;
    }

    /**
     * Sets the unique vehicle number of the bus or tram.
     *
     * @param vehicleNumber A `String` representing the vehicle number to set.
     */
    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    /**
     * Retrieves the brigade number assigned to the bus or tram.
     *
     * @return A `String` representing the brigade number.
     */
    public String getBrigade() {
        return brigade;
    }

    /**
     * Sets the brigade number assigned to the bus or tram.
     *
     * @param brigade A `String` representing the brigade number to set.
     */
    public void setBrigade(String brigade) {
        this.brigade = brigade;
    }
}