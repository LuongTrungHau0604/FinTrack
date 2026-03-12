package com.example.myapplication.data.model; // Đặt vào đúng package

import com.google.firebase.Timestamp; // Import Firestore Timestamp
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.Date;

@IgnoreExtraProperties
public class Repayment implements FirebaseModelBase {

    // private int loanId;      // Không cần lưu loanId nếu đây là subcollection của Loan
    private double amountPaid;    // Số tiền đã trả
    private Timestamp date;       // Ngày trả (dùng Timestamp)
    private String notes;         // Ghi chú (tùy chọn)

    private String firebaseId;    // ID của document repayment trên Firestore

    // Constructor mặc định BẮT BUỘC
    public Repayment() {
    }

    // Getters and Setters

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    @Exclude
    public Date getDateAsDate() {
        return (date != null) ? date.toDate() : null;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    @Exclude
    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }



}