package com.example.myapplication.data.model; // Đặt vào đúng package

import com.google.firebase.Timestamp; // Import Firestore Timestamp
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.Date;

@IgnoreExtraProperties
public class PaymentReminder implements FirebaseModelBase {

    private String name;          // Tên nhắc nhở (vd: Tiền điện tháng 6)
    private double amount;        // Số tiền dự kiến (có thể là 0)
    private String repeatCycle;   // Chu kỳ lặp lại (vd: "monthly", "yearly", "none")
    // private int reminderDay;    // Có thể không cần nếu dùng dueDate
    private String reminderType;  // Loại nhắc nhở (vd: "bill", "debt_payment", "subscription")
    // private String userId;     // Không cần lưu
    private Timestamp dueDate;       // Ngày đến hạn thanh toán (DÙNG TIMESTAMP)
    private boolean isPaid;       // Trạng thái đã thanh toán hay chưa
    private String notes;         // Ghi chú thêm (tùy chọn)
    private String relatedLoanId; // ID của khoản vay liên quan (nếu type là debt_payment)
    private String relatedAccountId; // ID tài khoản dự kiến thanh toán (tùy chọn)


    private String firebaseId;    // ID document trên Firestore

    // Constructor mặc định BẮT BUỘC
    public PaymentReminder() {
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getRepeatCycle() {
        return repeatCycle;
    }

    public void setRepeatCycle(String repeatCycle) {
        this.repeatCycle = repeatCycle;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    @Exclude
    public Date getDueDateAsDate() {
        return (dueDate != null) ? dueDate.toDate() : null;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRelatedLoanId() {
        return relatedLoanId;
    }

    public void setRelatedLoanId(String relatedLoanId) {
        this.relatedLoanId = relatedLoanId;
    }

    public String getRelatedAccountId() {
        return relatedAccountId;
    }

    public void setRelatedAccountId(String relatedAccountId) {
        this.relatedAccountId = relatedAccountId;
    }

    @Override // Thêm @Override nếu implement FirebaseModelBase
    @Exclude
    public String getFirebaseId() {
        return firebaseId;
    }
    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

}