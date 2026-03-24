package com.gpstracker.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class LocationModel {

    public String userId;
    public String name;
    public double latitude;
    public double longitude;
    public long timestamp;
    public Object status;
    public Object batteryLevel;
    public float speed;
    public String address;
    public String profileImageUrl;
    public float distance; // <--- નવું ફિલ્ડ ઉમેર્યું

    // ૧. ડિફોલ્ટ કન્સ્ટ્રક્ટર (Firebase માટે ફરજિયાત)
    public LocationModel() {
    }

    // ૨. પેરામીટરાઈઝ્ડ કન્સ્ટ્રક્ટર (Distance સાથે)
    public LocationModel(String userId, String name, double latitude, double longitude,
                         long timestamp, Object status, Object batteryLevel,
                         float speed, String address, float distance) {
        this.userId = userId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.speed = speed;
        this.address = address;
        this.distance = distance; // <--- અહીં એડ કર્યું
    }

    // ૩. Getters & Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() {
        return status != null ? String.valueOf(status) : "offline";
    }
    public void setStatus(Object status) { this.status = status; }

    public String getBatteryLevel() {
        String bat = batteryLevel != null ? String.valueOf(batteryLevel) : "0";
        if (!bat.contains("%")) bat += "%";
        return bat;
    }
    public void setBatteryLevel(Object batteryLevel) { this.batteryLevel = batteryLevel; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    // Distance માટે Getter અને Setter
    public float getDistance() { return distance; }
    public void setDistance(float distance) { this.distance = distance; }
}