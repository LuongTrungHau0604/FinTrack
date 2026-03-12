package com.example.myapplication.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.Date;

@IgnoreExtraProperties
public class Profile {

    private String displayName;
    private Timestamp createdAt; // Changed from long

    public Profile() {
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Timestamp getCreatedAt() { return createdAt; } // Changed return type
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; } // Changed parameter type

    @Exclude
    public Date getCreatedAtAsDate() {
        return (createdAt != null) ? createdAt.toDate() : null;
    }
}