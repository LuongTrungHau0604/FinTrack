package com.example.myapplication.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {
    private String uid;
    private String displayName;
    private String email;
    private String photoUrl;
    private String phoneNumber;
    private String location;
    private boolean darkModeEnabled;
    private boolean notificationsEnabled;
    private String defaultCurrency;

    // For Firestore deserialization
    public UserProfile() {
        // Default values
        this.darkModeEnabled = false;
        this.notificationsEnabled = true;
        this.defaultCurrency = "USD";
    }

    public UserProfile(String uid, String displayName, String email) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.darkModeEnabled = false;
        this.notificationsEnabled = true;
        this.defaultCurrency = "USD";
    }

    // Getters and setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @PropertyName("darkModeEnabled")
    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    @PropertyName("darkModeEnabled")
    public void setDarkModeEnabled(boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    @PropertyName("notificationsEnabled")
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    @PropertyName("notificationsEnabled")
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    // Helper method to convert to Map for Firestore updates
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);

        if (displayName != null) result.put("displayName", displayName);
        if (email != null) result.put("email", email);
        if (photoUrl != null) result.put("photoUrl", photoUrl);
        if (phoneNumber != null) result.put("phoneNumber", phoneNumber);
        if (location != null) result.put("location", location);
        result.put("darkModeEnabled", darkModeEnabled);
        result.put("notificationsEnabled", notificationsEnabled);
        if (defaultCurrency != null) result.put("defaultCurrency", defaultCurrency);

        return result;
    }
}