package com.example.myapplication.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.Date;

@IgnoreExtraProperties
public class Transaction {
    private String accountType; // "personal" or "shared"
    private String type;
    private double amount;
    private String currency;
    private String description;
    private String date;
    private Timestamp timestamp;
    private String categoryId;
    private String accountId;
    private String note;
    private String firebaseId;

    public Transaction() {
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    @Exclude
    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    @Exclude
    public Date getTimestampAsDate() {
        return (timestamp != null) ? timestamp.toDate() : null;
    }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
}