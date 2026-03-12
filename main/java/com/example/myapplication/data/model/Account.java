package com.example.myapplication.data.model; // Adjust package

import com.google.firebase.Timestamp;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.ArrayList; // Import ArrayList
import java.util.Date;
import java.util.List; // Import List

@IgnoreExtraProperties
public class Account implements FirebaseModelBase {

    private String name;
    private String type;
    private String currency;
    private double currentBalance;
    private String icon;
    private String color;
    private boolean includeInTotal;
    private Timestamp createdAt;
    private String ownerId; // <<< THÊM: UID của chủ sở hữu gốc
    private List<String> sharedWithUids; // <<< THÊM: Danh sách UID người được chia sẻ (có thể null)

    private String firebaseId; // ID của document này

    public Account() {
        // Firestore cần constructor rỗng
        this.sharedWithUids = new ArrayList<>(); // Khởi tạo list rỗng để tránh NullPointerException
    }

    // --- Getters and Setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isIncludeInTotal() { return includeInTotal; }
    public void setIncludeInTotal(boolean includeInTotal) { this.includeInTotal = includeInTotal; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getOwnerId() { return ownerId; } // <<< THÊM
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; } // <<< THÊM

    public List<String> getSharedWithUids() { return sharedWithUids; } // <<< THÊM
    public void setSharedWithUids(List<String> sharedWithUids) { this.sharedWithUids = sharedWithUids; } // <<< THÊM

    @Override
    @Exclude
    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    @Exclude
    public Date getCreatedAtAsDate() {
        return (createdAt != null) ? createdAt.toDate() : null;
    }

    // Helper để kiểm tra xem có phải tài khoản được chia sẻ không (không phải chủ sở hữu)
    @Exclude
    public boolean isSharedAccount(String currentUserId) {
        return ownerId != null && !ownerId.equals(currentUserId);
    }
}