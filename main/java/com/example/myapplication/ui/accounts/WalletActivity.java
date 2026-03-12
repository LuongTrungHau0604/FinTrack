package com.example.myapplication.ui.accounts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.databinding.ActivityWalletBinding;
import com.example.myapplication.data.model.Account;
import com.example.myapplication.data.model.SharedAccountRef; // Import model tham chiếu
import com.example.myapplication.data.datasource.FirebaseDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger; // Dùng để đếm tác vụ bất đồng bộ

public class WalletActivity extends AppCompatActivity implements AccountAdapter.OnAccountInteractionListener {

    private static final String TAG = "WalletActivity";

    private ActivityWalletBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;
    private AccountAdapter accountAdapter;
    // Danh sách cuối cùng để hiển thị, gộp cả cá nhân và được chia sẻ
    private List<Account> displayAccountList = new ArrayList<>();
    // Lưu trữ tạm thời trước khi gộp
    private List<Account> personalAccounts = new ArrayList<>();
    private List<SharedAccountRef> sharedAccountRefs = new ArrayList<>();
    // Map để lấy chi tiết tài khoản được chia sẻ nhanh hơn (Key: sharedAccountId, Value: Account)
    private Map<String, Account> sharedAccountDetailsMap = new HashMap<>();


    private ActivityResultLauncher<Intent> accountActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
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
        loadAllAccountData(); // Load cả 2 loại tài khoản
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarWallet);
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
        binding.recyclerViewAccounts.setLayoutManager(new LinearLayoutManager(this));
        // Khởi tạo adapter với list hiển thị displayAccountList
        accountAdapter = new AccountAdapter(this, displayAccountList, this);
        binding.recyclerViewAccounts.setAdapter(accountAdapter);
    }

    private void setupActivityResultLaunchers() {
        accountActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Returned from Add/Edit Account. Reloading accounts.");
                        loadAllAccountData(); // Tải lại cả 2 loại
                    }
                });
    }

    private void setupListeners() {
        binding.fabAddAccount.setOnClickListener(v -> {
            Log.d(TAG, "FAB Add Account clicked.");
            // Mở màn hình AddAccountActivity để thêm tài khoản MỚI (luôn là cá nhân)
            Intent intent = new Intent(WalletActivity.this, AddAccountActivity.class);
            accountActivityResultLauncher.launch(intent);
        });
    }

    /**
     * Tải dữ liệu tài khoản cá nhân và tham chiếu tài khoản được chia sẻ.
     */
    private void loadAllAccountData() {
        showLoading(true);
        binding.textNoAccounts.setVisibility(View.GONE);

        // Dùng AtomicInteger để theo dõi 2 lời gọi bất đồng bộ
        AtomicInteger tasksRemaining = new AtomicInteger(2);

        // Callback để gộp và cập nhật UI khi cả 2 lời gọi hoàn tất
        Runnable onDataLoadComplete = () -> {
            if (tasksRemaining.decrementAndGet() == 0) {
                Log.d(TAG, "Both personal and shared refs loaded. Merging lists.");
                mergeAndDisplayAccounts();
                showLoading(false);
            }
        };

        // 1. Tải tài khoản cá nhân
        firebaseDAO.getAllAccounts(currentUserId, new FirebaseDAO.OnAccountsRetrievedListener() {
            @Override
            public void onSuccess(List<Account> accounts) {
                Log.d(TAG, "Personal accounts loaded: " + (accounts != null ? accounts.size() : "null"));
                personalAccounts.clear();
                if (accounts != null) {
                    personalAccounts.addAll(accounts);
                }
                onDataLoadComplete.run();
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load personal accounts", e);
                Toast.makeText(WalletActivity.this, "Error loading personal accounts", Toast.LENGTH_SHORT).show();
                personalAccounts.clear(); // Xóa nếu lỗi
                onDataLoadComplete.run(); // Vẫn giảm biến đếm
            }
        });

        // 2. Tải tham chiếu tài khoản được chia sẻ
        firebaseDAO.getSharedAccountReferences(currentUserId, new FirebaseDAO.OnSharedAccountRefsRetrievedListener() {
            @Override
            public void onSuccess(List<SharedAccountRef> refs) {
                Log.d(TAG, "Shared account references loaded: " + (refs != null ? refs.size() : "null"));
                sharedAccountRefs.clear();
                if (refs != null) {
                    sharedAccountRefs.addAll(refs);
                }
                // TODO Optional: Fetch full details for shared accounts here if needed immediately
                // Or fetch them on demand when displaying details / transactions
                onDataLoadComplete.run();
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load shared account references", e);
                Toast.makeText(WalletActivity.this, "Error loading shared accounts", Toast.LENGTH_SHORT).show();
                sharedAccountRefs.clear(); // Xóa nếu lỗi
                onDataLoadComplete.run(); // Vẫn giảm biến đếm
            }
        });
    }

    /**
     * Gộp danh sách tài khoản cá nhân và tài khoản được chia sẻ để hiển thị.
     */
    private void mergeAndDisplayAccounts() {
        displayAccountList.clear();
        displayAccountList.addAll(personalAccounts); // Thêm tài khoản cá nhân trước
        Log.d(TAG, "Merged list size: " + displayAccountList.size() + ". Calling adapter update."); // Log trước khi gọi
        if (accountAdapter != null) {
            accountAdapter.updateData(displayAccountList);
            Log.d(TAG, "Adapter data updated after merge.");
        } else {
            Log.e(TAG, "mergeAndDisplayAccounts: accountAdapter is NULL!");
            // Không thể hiển thị nếu adapter null
            return;
        }
        Log.d(TAG, "Visibility Check: RecyclerView=" + binding.recyclerViewAccounts.getVisibility() +
                " (VISIBLE=" + View.VISIBLE + ", GONE=" + View.GONE + ", INVISIBLE=" + View.INVISIBLE + ")");
        Log.d(TAG, "Visibility Check: EmptyText=" + binding.textNoAccounts.getVisibility());
        Log.d(TAG, "Visibility Check: ProgressBar=" + binding.progressBarWallet.getVisibility());
        // Tạo các đối tượng Account tạm thời từ SharedAccountRef để hiển thị
        // Lưu ý: Số dư và các thông tin khác có thể cần load riêng khi cần
        for (SharedAccountRef ref : sharedAccountRefs) {
            Account sharedAccountPlaceholder = new Account();
            sharedAccountPlaceholder.setFirebaseId(ref.getSharedAccountId()); // ID tài khoản gốc
            sharedAccountPlaceholder.setOwnerId(ref.getOwnerId());          // Owner gốc
            sharedAccountPlaceholder.setName(ref.getAccountName() + " (Shared)"); // Thêm chữ Shared
            // Đặt các giá trị mặc định hoặc placeholder cho các trường khác
            sharedAccountPlaceholder.setCurrentBalance(0); // Cần load riêng số dư nếu muốn hiển thị ở đây
            sharedAccountPlaceholder.setType("shared");    // Đặt type là shared để phân biệt
            sharedAccountPlaceholder.setCurrency("..."); // Lấy từ ref hoặc để trống
            sharedAccountPlaceholder.setIcon("ic_group_wallet"); // Icon chung cho shared
            // ...
            displayAccountList.add(sharedAccountPlaceholder);
        }

        Log.d(TAG, "Merged list size: " + displayAccountList.size());

        if (displayAccountList.isEmpty()) {
            Log.d(TAG, "mergeAndDisplayAccounts: List is empty, showing empty state.");
            showEmptyState("No accounts found. Tap + to add.");
        } else {
            Log.d(TAG, "mergeAndDisplayAccounts: List has data, showing RecyclerView.");
            binding.recyclerViewAccounts.setVisibility(View.VISIBLE); // Hiện RecyclerView
            binding.textNoAccounts.setVisibility(View.GONE);      // Ẩn text rỗng
        }
    }


    private void showEmptyState(String message) {
        Log.d(TAG, "showEmptyState: Displaying message - " + message);
        if(binding != null){
            binding.recyclerViewAccounts.setVisibility(View.GONE);      // Ẩn list
            binding.textNoAccounts.setText(message);
            binding.textNoAccounts.setVisibility(View.VISIBLE);     // Hiện text
            binding.progressBarWallet.setVisibility(View.GONE); // Đảm bảo progress bar cũng ẩn
        }
    }
    // Trong WalletActivity.java
    private void showLoading(boolean isLoading) {
        Log.d(TAG, "showLoading: Setting ProgressBar visibility to " + (isLoading ? "VISIBLE" : "GONE"));
        if (binding != null && binding.progressBarWallet != null) {
            binding.progressBarWallet.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            // Tạm thời chỉ xử lý ProgressBar ở đây.
            // KHÔNG ẩn/hiện RecyclerView hay text empty ở đây nữa.
            // if (isLoading) {
            //     binding.recyclerViewAccounts.setVisibility(View.GONE);
            //     binding.textNoAccounts.setVisibility(View.GONE);
            // }
        } else {
            Log.w(TAG, "Binding or ProgressBar is null in showLoading");
        }
    }

    // --- Implement Interface Methods from AccountAdapter ---

    @Override
    public void onEditAccountClick(Account account, int position) {
        if (account == null || account.getFirebaseId() == null) return;

        // Chỉ cho phép chủ sở hữu chỉnh sửa chi tiết tài khoản
        if (currentUserId.equals(account.getOwnerId())) {
            Log.d(TAG, "Edit account clicked (Owner): " + account.getName());
            Intent intent = new Intent(this, AddAccountActivity.class);
            intent.putExtra("EDIT_MODE", true);
            intent.putExtra("ACCOUNT_ID", account.getFirebaseId()); // Truyền ID tài khoản gốc
            accountActivityResultLauncher.launch(intent);
        } else {
            Log.d(TAG, "Edit clicked for a shared account - View details instead?");
            // Đối với tài khoản được chia sẻ, có thể mở màn hình chi tiết thay vì sửa
            Toast.makeText(this, "Cannot edit details of a shared account.", Toast.LENGTH_SHORT).show();
            // TODO: Implement viewing details for shared accounts if needed
        }
    }

    @Override
    public void onDeleteAccountClick(Account account, int position) {
        if (account == null || account.getFirebaseId() == null) return;

        // Chỉ chủ sở hữu mới được xóa tài khoản gốc
        if (currentUserId.equals(account.getOwnerId())) {
            Log.d(TAG, "Delete account clicked (Owner): " + account.getName());
            // TODO: Thêm kiểm tra xem tài khoản có giao dịch không trước khi xóa!
            showDeleteAccountConfirmationDialog(account, position);
        } else {
            // Người được chia sẻ chỉ có thể "rời khỏi" tài khoản đó
            Log.d(TAG, "Delete clicked for a shared account - Prompt to leave.");
            showLeaveSharedAccountConfirmationDialog(account, position);
        }
    }

    private void showDeleteAccountConfirmationDialog(Account account, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete '" + account.getName() + "'? This will also remove access for everyone it's shared with and cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccountFromDb(account, position))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showLeaveSharedAccountConfirmationDialog(Account account, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Leave Shared Account")
                .setMessage("Are you sure you want to leave the shared account '" + account.getName() + "'? You will lose access to it.")
                .setPositiveButton("Leave", (dialog, which) -> leaveSharedAccount(account, position))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void deleteAccountFromDb(Account account, int position) {
        showLoading(true);
        // TODO: Cần xóa cả các tham chiếu trong sharedAccountRefs của những người được chia sẻ!
        // Đây là lúc Cloud Function sẽ hữu ích hơn để đảm bảo tính nhất quán.
        // Tạm thời chỉ xóa tài khoản gốc:
        firebaseDAO.deleteAccount(account.getOwnerId(), account.getFirebaseId(), new FirebaseDAO.OnAccountDeletedListener() {
            @Override public void onSuccess() {
                Log.d(TAG, "Account deleted from DB: " + account.getFirebaseId());
                // Cập nhật UI cục bộ
                handleAccountDeletionSuccess(account, position);
                Toast.makeText(WalletActivity.this, "Account Deleted", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Failed to delete account", e);
                showLoading(false);
                Toast.makeText(WalletActivity.this, "Error deleting account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void leaveSharedAccount(Account account, int position) {
        showLoading(true);
        // Gọi hàm unshare của DAO, truyền ownerId gốc, accountId gốc và UID của người muốn rời đi
        firebaseDAO.unshareAccountWithUser(account.getOwnerId(), account.getFirebaseId(), currentUserId, new FirebaseDAO.OnShareUpdatedListener() {
            @Override public void onSuccess() {
                Log.d(TAG, "Successfully left shared account: " + account.getFirebaseId());
                handleAccountDeletionSuccess(account, position); // Xóa khỏi list hiển thị cục bộ
                Toast.makeText(WalletActivity.this, "Left shared account", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Failed to leave shared account", e);
                showLoading(false);
                Toast.makeText(WalletActivity.this, "Error leaving account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void handleAccountDeletionSuccess(Account deletedAccount, int position) {
        // Hàm này dùng chung cho cả xóa gốc và rời khỏi shared
        showLoading(false);
        if (position >= 0 && position < displayAccountList.size()) {
            // So sánh ID vì object có thể khác (placeholder vs full object)
            if (displayAccountList.get(position).getFirebaseId().equals(deletedAccount.getFirebaseId())) {
                displayAccountList.remove(position);
                accountAdapter.notifyItemRemoved(position);
                if (displayAccountList.isEmpty()) {
                    showEmptyState("No accounts found. Tap + to add.");
                }
            } else {
                Log.w(TAG, "Position mismatch after deletion/leaving. Reloading.");
                loadAllAccountData();
            }
        } else {
            Log.w(TAG, "Invalid position after deletion/leaving. Reloading.");
            loadAllAccountData();
        }
    }

    // --- DAO Listener Interfaces (Keep for clarity) ---
    // public interface OnAccountsRetrievedListener { ... }
    // public interface OnSharedAccountRefsRetrievedListener { ... }
    // public interface OnAccountDeletedListener { ... }
    // public interface OnShareUpdatedListener { ... }
}