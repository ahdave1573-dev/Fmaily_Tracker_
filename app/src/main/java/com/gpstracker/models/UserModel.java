package com.gpstracker.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class UserModel {
    private String userId;
    private String name;
    private String email;
    private Object status;      // "onl ine" અથવા "offline"
    private Object batteryLevel;
    private String profileImageUrl;
    // Map of groupId -> sharingStatus (true = sharing, false = not sharing)
    private Map<String, Object> joinedGroups = new HashMap<>();
    private Map<String, Object> sharingStatus = new HashMap<>();
    // Map of groupIds created by this user
    private Map<String, Object> createdGroups = new HashMap<>();

    public UserModel() {
    }

    public UserModel(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.status = "offline";
        this.batteryLevel = "0%";
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() {
        return status != null ? String.valueOf(status) : "offline";
    }
    public void setStatus(Object status) { this.status = status; }

    public String getBatteryLevel() {
        return batteryLevel != null ? String.valueOf(batteryLevel) : "0%";
    }
    public void setBatteryLevel(Object batteryLevel) { this.batteryLevel = batteryLevel; }

    public Map<String, Object> getJoinedGroups() { return joinedGroups; }
    public void setJoinedGroups(Map<String, Object> joinedGroups) { this.joinedGroups = joinedGroups; }

    public Map<String, Object> getCreatedGroups() { return createdGroups; }
    public void setCreatedGroups(Map<String, Object> createdGroups) { this.createdGroups = createdGroups; }

    public Map<String, Object> getSharingStatus() { return sharingStatus; }
    public void setSharingStatus(Map<String, Object> sharingStatus) { this.sharingStatus = sharingStatus; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}