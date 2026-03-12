package com.example.myapplication.ui.notifications;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.data.datasource.FirebaseDAO;
import com.example.myapplication.data.model.Loan;
import com.example.myapplication.data.model.NotificationItem;
import com.example.myapplication.data.model.Transaction;
import com.example.myapplication.databinding.ActivityNotificationBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    private ActivityNotificationBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;
    private NotificationAdapter notificationAdapter;
    private List<NotificationItem> notificationItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firebaseDAO = new FirebaseDAO();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        setupToolbar();
        setupRecyclerView();
        loadNotifications();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarNotifications);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(this, notificationItems);
        binding.recyclerViewNotifications.setAdapter(notificationAdapter);
    }

    private void loadNotifications() {
        showLoading(true);
        binding.textNoNotifications.setVisibility(View.GONE);

        // Load khoản vay/nợ sắp đến hạn
        firebaseDAO.getAllLoans(currentUserId, new FirebaseDAO.OnLoansRetrievedListener() {
            @Override
            public void onSuccess(List<Loan> loans) {
                processLoans(loans);
                // Sau khi xử lý loans, tiếp tục tải transactions
                loadRecentTransactions();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load loans", e);
                // Vẫn tiếp tục tải transactions dù loans có lỗi
                loadRecentTransactions();
            }
        });
    }

    private void loadRecentTransactions() {
        firebaseDAO.getAllTransactions(currentUserId, new FirebaseDAO.OnTransactionsRetrievedListener() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                processTransactions(transactions);
                finishLoading();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load transactions", e);
                finishLoading();
            }
        });
    }

    private void processLoans(List<Loan> loans) {
        if (loans == null || loans.isEmpty()) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();

        // Kiểm tra 7 ngày tới cho các khoản vay/nợ sắp đến hạn
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        Date nextWeek = calendar.getTime();

        for (Loan loan : loans) {
            // Sửa phần này: Chuyển đổi String dueDate thành Date
            Date dueDate = null;
            try {
                if (loan.getDueDate() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    dueDate = sdf.parse(loan.getDueDate());
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + loan.getDueDate(), e);
                continue;
            }

            if (dueDate != null && dueDate.after(today) && dueDate.before(nextWeek)) {
                // Tạo thông báo cho khoản vay/nợ sắp đến hạn
                NotificationItem item = new NotificationItem();
                item.setId(loan.getFirebaseId());
                item.setTitle(loan.getName() + " - Due Soon");

                // Tính số ngày còn lại
                long diffInMillies = dueDate.getTime() - today.getTime();
                int diffInDays = (int) (diffInMillies / (1000 * 60 * 60 * 24));

                item.setDescription("Due in " + diffInDays + " days. Amount: " + loan.getCurrentBalance() +
                        " " + (loan.getCurrency() != null ? loan.getCurrency() : "VND"));
                item.setType("loan"); // Sửa đoạn này nếu loan.getType() gặp lỗi

                // Sửa đoạn này: Tạo Timestamp từ Date
                Timestamp timestamp = new Timestamp(dueDate);
                item.setTimestamp(timestamp);

                item.setIsImportant(true);

                notificationItems.add(item);
            }
        }
    }

    private void processTransactions(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        // Lấy 10 giao dịch gần nhất để thông báo
        int count = 0;
        for (Transaction transaction : transactions) {
            if (count >= 10) break; // Giới hạn 10 thông báo giao dịch

            NotificationItem item = new NotificationItem();
            item.setId(transaction.getFirebaseId());
            item.setTitle(transaction.getType().equals("income") ? "Income Transaction" : "Expense Transaction");
            item.setDescription(transaction.getDescription() + " - Amount: " + transaction.getAmount());
            item.setType("transaction");
            item.setTimestamp(transaction.getTimestamp());
            item.setIsImportant(false);

            notificationItems.add(item);
            count++;
        }
    }

    private void finishLoading() {
        // Sắp xếp thông báo theo thời gian gần nhất
        notificationItems.sort((item1, item2) -> {
            if (item1.getTimestamp() == null && item2.getTimestamp() == null) return 0;
            if (item1.getTimestamp() == null) return 1;
            if (item2.getTimestamp() == null) return -1;
            // Sắp xếp giảm dần (mới nhất lên đầu)
            return item2.getTimestamp().compareTo(item1.getTimestamp());
        });

        notificationAdapter.notifyDataSetChanged();

        showLoading(false);

        if (notificationItems.isEmpty()) {
            showEmptyState("No notifications available.");
        }
    }

    private void showEmptyState(String message) {
        if (binding != null) {
            binding.recyclerViewNotifications.setVisibility(View.GONE);
            binding.textNoNotifications.setText(message);
            binding.textNoNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBarNotifications.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.recyclerViewNotifications.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            if (isLoading) {
                binding.textNoNotifications.setVisibility(View.GONE);
            } else if (notificationItems.isEmpty()) {
                binding.textNoNotifications.setVisibility(View.VISIBLE);
            } else {
                binding.textNoNotifications.setVisibility(View.GONE);
            }
        }
    }
}