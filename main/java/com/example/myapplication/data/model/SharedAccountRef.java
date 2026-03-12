package com.example.myapplication.data.model; // Adjust package

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class SharedAccountRef implements FirebaseModelBase { // Implement để dùng chung nếu cần

    private String ownerId;     // UID của chủ sở hữu tài khoản gốc
    private String accountName; // Tên của tài khoản gốc (để hiển thị nhanh)
    // private String icon;     // Optional: Lưu thêm icon
    // private String color;    // Optional: Lưu thêm màu

    private String sharedAccountId; // ID của tài khoản gốc (key của document này)

    public SharedAccountRef() { } // Required empty constructor

    public SharedAccountRef(String ownerId, String accountName) {
        this.ownerId = ownerId;
        this.accountName = accountName;
    }

    // Getters and Setters
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getSharedAccountId() { return sharedAccountId; };
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    // public String getIcon() { return icon; }
    // public void setIcon(String icon) { this.icon = icon; }
    // public String getColor() { return color; }
    // public void setColor(String color) { this.color = color; }

    @Override
    @Exclude
    public String getFirebaseId() { return sharedAccountId; } // Trả về ID tài khoản gốc
    // Setter này để gán ID khi đọc từ Firestore
    public void setSharedAccountId(String sharedAccountId) { this.sharedAccountId = sharedAccountId; }
}