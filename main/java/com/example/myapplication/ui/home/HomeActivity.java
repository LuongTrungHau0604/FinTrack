package com.example.myapplication.ui.home;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.myapplication.ui.chat.ChatActivity;
import com.example.myapplication.ui.loanandebt.DebtActivity;
import com.example.myapplication.ui.notifications.NotificationActivity;
import com.example.myapplication.ui.profile.ProfileActivity;
import com.example.myapplication.ui.transactions.AllTransactionsActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.transactions.TransactionAdapter;
import com.example.myapplication.ui.accounts.WalletActivity;
import com.example.myapplication.data.datasource.FirebaseDAO;
import com.example.myapplication.data.model.Category;
import com.example.myapplication.data.model.Transaction;
import com.example.myapplication.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.myapplication.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int MAX_RECENT_TRANSACTIONS = 5;

    private ActivityHomeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;

    private TransactionAdapter transactionAdapter;
    private List<Transaction> allTransactions = new ArrayList<>();
    private Map<String, Category> categoryMap = new HashMap<>();

    private NumberFormat currencyFormatter;
    private final SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firebaseDAO = new FirebaseDAO();

        currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        setupRecyclerView();
        setupListeners(); // Renamed from setupClickListeners for clarity
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserAuthentication();
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLoginActivity();
        } else {
            currentUserId = currentUser.getUid();
            Log.d(TAG, "User logged in: " + currentUserId);
            updateGreeting(); // Update greeting here
            loadUserData();
        }
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerView() {
        binding.recyclerViewRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
        // --- SỬA LẠI DÒNG NÀY ---
        // Khởi tạo adapter với danh sách và map rỗng ban đầu
        // Truyền null cho listener vì HomeActivity có thể không cần xử lý edit/delete trực tiếp
        transactionAdapter = new TransactionAdapter(this, new ArrayList<>(), new HashMap<>(), null); // Thêm null vào cuối
        // --- KẾT THÚC SỬA ĐỔI ---
        binding.recyclerViewRecentTransactions.setAdapter(transactionAdapter);
        binding.recyclerViewRecentTransactions.setNestedScrollingEnabled(false);
    }

    private void setupListeners() {
        // See All Transactions Click
        binding.seeAllTransactions.setOnClickListener(v -> {
            Log.d(TAG, "See All transactions clicked.");
            Intent intent = new Intent(HomeActivity.this, AllTransactionsActivity.class);
            startActivity(intent);
        });
        binding.fabChat.setOnClickListener(v -> {
            Log.d(TAG, "FAB Chat clicked.");
            Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
            startActivity(intent);
                });
        // Bottom Navigation Selection
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                // Already home or refresh
                return true;
            } else if (itemId == R.id.navigation_debt) {
                Intent intent = new Intent(HomeActivity.this, DebtActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_add) {
                AddOptionsBottomSheetFragment bottomSheetFragment = new AddOptionsBottomSheetFragment();
                bottomSheetFragment.show(getSupportFragmentManager(), AddOptionsBottomSheetFragment.TAG);
                return false; // Don't keep "Add" selected
            } else if (itemId == R.id.navigation_wallet) {
                Intent intent = new Intent(HomeActivity.this, WalletActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            else {
                return false;
            }
        });

        ImageView notificationIcon = findViewById(R.id.notification_icon);
        notificationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, NotificationActivity.class);
                startActivity(intent);
            }
        });

        // Set default selected item AFTER setting the listener
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
    }

    private void updateGreeting() {
        FirebaseUser user = mAuth.getCurrentUser();
        String greetingName = (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : "User";
        binding.greeting.setText(getString(R.string.greeting_format, greetingName)); // Use string resource
    }


    private void loadUserData() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "Cannot load user data: User ID is null or empty.");
            goToLoginActivity();
            return;
        }
        Log.d(TAG, "Loading data for user: " + currentUserId);
        // showLoading(true); // Consider adding loading indicators

        firebaseDAO.getAllCategories(currentUserId, new FirebaseDAO.OnCategoriesRetrievedListener() {
            @Override
            public void onSuccess(List<Category> categories) {
                Log.d(TAG, "Categories loaded: " + categories.size());
                categoryMap.clear();
                for (Category category : categories) {
                    if (category.getFirebaseId() != null) {
                        categoryMap.put(category.getFirebaseId(), category);
                    }
                }
                loadTransactions(); // Load transactions after categories
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load categories", e);
                Toast.makeText(HomeActivity.this, "Failed to load categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // hideLoading(false);
            }
        });
    }

    private void loadTransactions() {
        if (currentUserId == null) return;

        firebaseDAO.getAllTransactions(currentUserId, new FirebaseDAO.OnTransactionsRetrievedListener(){
            @Override
            public void onSuccess(List<Transaction> transactions) {
                Log.d(TAG, "Transactions loaded: " + transactions.size());
                allTransactions.clear();
                allTransactions.addAll(transactions);
                calculateTotalsAndUpdateUI();
                // hideLoading(false);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load transactions", e);
                Toast.makeText(HomeActivity.this, "Failed to load transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // hideLoading(false);
            }
        });
    }

    private void calculateTotalsAndUpdateUI() {
        double totalIncome = 0.0;
        double totalExpenses = 0.0;

        for (Transaction transaction : allTransactions) {
            if (transaction.getTimestamp() != null) { // Ensure timestamp exists for potential logic
                if ("income".equalsIgnoreCase(transaction.getType())) {
                    totalIncome += transaction.getAmount();
                } else if ("expense".equalsIgnoreCase(transaction.getType())) {
                    totalExpenses += transaction.getAmount();
                }
            }
        }
        updateTotalsUI(totalIncome, totalExpenses);
        updateRecentTransactionsUI();
    }

    private void updateTotalsUI(double income, double expenses) {
        double balance = income - expenses;
        // Ensure TextView IDs match your layout file exactly
        binding.icomeText.setText(currencyFormatter.format(income));
        binding.expensesText.setText(currencyFormatter.format(expenses));
        binding.totalBalance.setText(currencyFormatter.format(balance));
    }

    private void updateRecentTransactionsUI() {
        try {
            // Sort by Firestore Timestamp (more reliable)
            Collections.sort(allTransactions, (t1, t2) -> {
                com.google.firebase.Timestamp ts1 = t1.getTimestamp();
                com.google.firebase.Timestamp ts2 = t2.getTimestamp();
                if (ts1 == null && ts2 == null) return 0;
                if (ts1 == null) return 1; // nulls last
                if (ts2 == null) return -1; // nulls last
                return ts2.compareTo(ts1); // Descending order (newest first)
            });

        } catch (Exception e) {
            Log.e(TAG, "Error sorting transactions by timestamp", e);
        }

        List<Transaction> recentTransactions = allTransactions.stream()
                .limit(MAX_RECENT_TRANSACTIONS)
                .collect(Collectors.toList());

        if (transactionAdapter != null) {
            transactionAdapter.updateData(recentTransactions, categoryMap);
        } else {
            Log.w(TAG, "TransactionAdapter is null when trying to update UI.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "HomeActivity onDestroy");
        // No listeners to remove here as we use single-fetch in DAO
    }
}