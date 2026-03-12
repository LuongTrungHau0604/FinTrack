package com.example.myapplication.ui.loanandebt; // Đảm bảo đúng package

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.databinding.ActivityDebtBinding; // Tạo file binding này
import com.example.myapplication.data.model.Loan; // Import model
import com.example.myapplication.data.datasource.FirebaseDAO; // Import DAO

import java.util.ArrayList;
import java.util.Collections; // Import Collections
import java.util.List;

public class DebtActivity extends AppCompatActivity implements LoanAdapter.OnLoanInteractionListener {

    private static final String TAG = "DebtActivity";

    private ActivityDebtBinding binding; // Binding cho activity_debt.xml
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;
    private LoanAdapter loanAdapter;
    private List<Loan> loanList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditLoanLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDebtBinding.inflate(getLayoutInflater());
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
        setupActivityResultLaunchers();
        setupListeners();
        loadLoans();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarDebt);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
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
        binding.recyclerViewLoans.setLayoutManager(new LinearLayoutManager(this));
        loanAdapter = new LoanAdapter(this, loanList, this); // Pass listener
        binding.recyclerViewLoans.setAdapter(loanAdapter);
    }

    private void setupActivityResultLaunchers() {
        addEditLoanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Returned from Add/Edit Loan. Reloading...");
                        loadLoans(); // Tải lại danh sách
                    }
                });
    }

    private void setupListeners() {
        binding.fabAddLoan.setOnClickListener(v -> {
            Log.d(TAG, "FAB Add Loan clicked.");
            // TODO: Tạo AddEditLoanActivity
            Intent intent = new Intent(DebtActivity.this, AddEditLoanActivity.class); // Activity để thêm/sửa Loan
            addEditLoanLauncher.launch(intent); // Dùng launcher
        });
    }

    private void loadLoans() {
        showLoading(true);
        binding.textNoLoans.setVisibility(View.GONE);

        firebaseDAO.getAllLoans(currentUserId, new FirebaseDAO.OnLoansRetrievedListener() { // Dùng listener mới
            @Override
            public void onSuccess(List<Loan> loans) {
                showLoading(false);
                if (loans == null || loans.isEmpty()) {
                    showEmptyState("No loans or debts recorded.");
                    loanList.clear();
                } else {
                    binding.recyclerViewLoans.setVisibility(View.VISIBLE);
                    binding.textNoLoans.setVisibility(View.GONE);
                    loanList.clear();
                    loanList.addAll(loans);
                    sortLoans(); // Sắp xếp nếu cần
                }
                loanAdapter.updateData(loanList); // Cập nhật adapter
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Log.e(TAG, "Failed to load loans", e);
                Toast.makeText(DebtActivity.this, "Error loading loans: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState("Failed to load loans.");
            }
        });
    }

    private void sortLoans() {
        // Sắp xếp theo ngày đến hạn hoặc ngày tạo, ví dụ:
        try {
            Collections.sort(loanList, (l1, l2) -> {
                // Logic sắp xếp, ví dụ theo ngày tạo (startDate) nếu có
                if(l1.getStartDate() == null && l2.getStartDate() == null) return 0;
                if(l1.getStartDate() == null) return 1; // null về cuối
                if(l2.getStartDate() == null) return -1;// null về cuối
                return l2.getStartDate().compareTo(l1.getStartDate()); // Giảm dần
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sorting loans", e);
        }
    }


    private void showEmptyState(String message) {
        if(binding != null){
            binding.recyclerViewLoans.setVisibility(View.GONE);
            binding.textNoLoans.setText(message);
            binding.textNoLoans.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBarDebt.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.recyclerViewLoans.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            if(isLoading) binding.textNoLoans.setVisibility(View.GONE);
            else if (loanAdapter.getItemCount() == 0) binding.textNoLoans.setVisibility(View.VISIBLE);
            else binding.textNoLoans.setVisibility(View.GONE);
        }
    }

    // --- Implement Interface Methods ---

    @Override
    public void onEditLoanClick(Loan loan, int position) {
        Log.d(TAG, "Edit loan clicked: " + loan.getName());
        if (loan == null || loan.getFirebaseId() == null) return;
        // TODO: Tạo AddEditLoanActivity
        Intent intent = new Intent(this, AddEditLoanActivity.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("LOAN_ID", loan.getFirebaseId());
        addEditLoanLauncher.launch(intent); // Dùng cùng launcher
    }

    @Override
    public void onDeleteLoanClick(Loan loan, int position) {
        Log.d(TAG, "Delete loan clicked: " + loan.getName());
        if (loan == null || loan.getFirebaseId() == null) return;
        // TODO: Quan trọng - Xử lý các khoản trả nợ (repayments) liên quan trước khi xóa khoản vay!
        // Có thể cần xóa subcollection 'repayments' trước hoặc cảnh báo người dùng.
        showDeleteLoanConfirmationDialog(loan, position);
    }

    private void showDeleteLoanConfirmationDialog(Loan loan, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Loan/Debt")
                .setMessage("Are you sure you want to delete '" + loan.getName() + "'? Associated repayment records might also be affected or deleted.")
                .setPositiveButton("Delete", (dialog, which) -> deleteLoanFromDb(loan, position))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteLoanFromDb(Loan loan, int position) {
        showLoading(true);
        // !!! BẠN CẦN THÊM deleteLoan VÀO FirebaseDAO !!!
        firebaseDAO.deleteLoan(currentUserId, loan.getFirebaseId(), new FirebaseDAO.OnLoanDeletedListener() { // Giả sử có interface này
            @Override
            public void onSuccess() {
                Log.d(TAG, "Loan deleted from DB");
                handleLoanDeletionSuccess(loan, position);
                Toast.makeText(DebtActivity.this, "Loan/Debt Deleted", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to delete loan", e);
                showLoading(false);
                Toast.makeText(DebtActivity.this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLoanDeletionSuccess(Loan deletedLoan, int position) {
        showLoading(false);
        if (position >= 0 && position < loanList.size() && loanList.get(position).getFirebaseId().equals(deletedLoan.getFirebaseId())) {
            loanList.remove(position);
            loanAdapter.notifyItemRemoved(position);
            if (loanList.isEmpty()) {
                showEmptyState("No loans or debts recorded.");
            }
        } else {
            Log.w(TAG, "Position mismatch after loan deletion. Reloading data.");
            loadLoans();
        }
    }

    // --- Interfaces giả định cần thêm vào FirebaseDAO ---
    // public interface OnLoansRetrievedListener { void onSuccess(List<Loan> list); void onFailure(Exception e); }
    // public interface OnLoanDeletedListener { void onSuccess(); void onFailure(Exception e); }

}