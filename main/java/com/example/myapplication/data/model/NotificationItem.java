package com.example.myapplication.data.model;

import com.google.firebase.Timestamp;

public class NotificationItem {
    private String id;
    private String title;
    private String description;
    private String type; // "loan", "debt", "transaction"
    private Timestamp timestamp;
    private boolean isImportant;

    public NotificationItem() {
        // Empty constructor needed for Firebase
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getIsImportant() {
        return isImportant;
    }

    public void setIsImportant(boolean important) {
        isImportant = important;
    }
}