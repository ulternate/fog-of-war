package com.danielcswain.fogofwar.Data;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * Object for location data storage and retrieval.
 */
public class LocationObject {

    private int id;
    private long datetime;
    private double latitude;
    private double longitude;

    /**
     * Constructor for a new LocationObject from the database.
     * @param id: int, the ID of the LocationObject in the database.
     * @param datetime: long, time in milliseconds.
     * @param latitude: double, Latitude of the location.
     * @param longitude: double, Longitude of the location.
     */
    public LocationObject(int id, long datetime, double latitude, double longitude) {
        this.id = id;
        this.datetime = datetime;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Constructor for a new LocationObject from a LatLng object. Used to save a new location object
     * retrieved from the location services.
     * @param latLng: LatLng object.
     */
    public LocationObject(LatLng latLng) {
        this.datetime = System.currentTimeMillis();
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
    }

    public int getId() {
        return id;
    }

    public long getDatetime() {
        return datetime;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Get the LatLng representation of a LocationObject.
     * @return a LatLng object.
     */
    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    /**
     * Get the Location representation of a LocationObject.
     * @return a Location object (used to calculate distance between points).
     */
    public Location getLocation() {
        Location location = new Location(String.valueOf(datetime));
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    @Override
    public String toString() {
        return datetime + ": (Lat: " + latitude + ", Long: " + longitude + ")";
    }
}
