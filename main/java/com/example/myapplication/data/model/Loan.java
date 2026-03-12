package com.example.myapplication.data.model; // Đặt vào đúng package model của bạn

import com.google.firebase.Timestamp; // Import Firestore Timestamp
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@IgnoreExtraProperties
public class Loan implements FirebaseModelBase { // Implement interface để dùng trong DAO helper

    private double initialAmount; // Số tiền vay ban đầu (đổi tên từ amount cho rõ)
    private double interestRate;  // Lãi suất (đổi tên từ interest)
    private String dueDate;       // Ngày đến hạn (giữ dạng String YYYY-MM-DD)
    private double currentBalance;// Số tiền còn lại (đổi tên từ remaining và dùng chung như Account)
    // private String userId;     // Không cần lưu userId bên trong document nếu đã lưu dưới dạng /users/{userId}/loans
    private Timestamp startDate;     // Thêm ngày bắt đầu vay (quan trọng)
    private String entityName;    // Tên người/tổ chức cho vay/vay (Thêm)
    private String name;          // Thêm tên/mô tả cho khoản vay (vd: Vay mua xe)
    private String notes;         // Thêm ghi chú (tùy chọn)
    private int paymentDayOfMonth; // Ngày trả hàng tháng (nếu có)

    private String firebaseId;    // ID của document trên Firestore
    private String currency; // *** ADDED CURRENCY FIELD ***


    // Constructor mặc định BẮT BUỘC
    public Loan() {
    }

    // Getters and Setters

    public double getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(double initialAmount) {
        this.initialAmount = initialAmount;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public String getDueDate() {
        return dueDate;
    }
    @Exclude
    public Date getDueDateAsDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return dueDate != null ? sdf.parse(dueDate) : null;
        } catch (ParseException e) {
            return null;
        }
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    // Đổi tên hàm setRemaining thành setCurrentBalance cho nhất quán
    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    @Exclude
    public Date getStartDateAsDate() {
        return (startDate != null) ? startDate.toDate() : null;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getPaymentDayOfMonth() {
        return paymentDayOfMonth;
    }

    public void setPaymentDayOfMonth(int paymentDayOfMonth) {
        this.paymentDayOfMonth = paymentDayOfMonth;
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }



    public String getType() {
        return "loan"; // Hoặc bất kỳ loại nào bạn muốn sử dụng để xác định đối tượng này
    }

    @Override // Từ FirebaseModelBase
    @Exclude
    public String getFirebaseId() {
        return firebaseId;
    }

    @Exclude
    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }



}