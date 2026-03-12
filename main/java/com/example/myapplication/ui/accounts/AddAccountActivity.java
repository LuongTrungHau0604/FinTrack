package com.example.myapplication.ui.accounts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.data.datasource.FirebaseDAO;
import com.example.myapplication.data.model.Account;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.databinding.ActivityAddAccountBinding;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddAccountActivity extends AppCompatActivity {

    private static final String TAG = "AddAccountActivity";
    private List<String> sharedUserIds = new ArrayList<>(); // Lưu ID người đang được chia sẻ

    private ActivityAddAccountBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;

    private String selectedAccountType = "";
    private String selectedCurrency = "VND";
    private String selectedIconName = "ic_account_balance_wallet";
    private String selectedColorHex = "#4CAF50";
    private boolean includeInTotal = true;

    // Edit Mode Variables
    private boolean isEditMode = false;
    private String editingAccountId = null;
    private Account existingAccountData = null; // Holds the original account data when editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firebaseDAO = new FirebaseDAO(); // Uses Firestore

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        handleIntent(); // Check for edit mode and get ID
        setupToolbar();
        setupSpinners();
        setupListeners();

        // Load existing data if in edit mode, otherwise set defaults
        if (!isEditMode) {
            updateColorView(selectedColorHex);
            updateIconView(selectedIconName);
            binding.switchIncludeInTotal.setChecked(includeInTotal);
            setSpinnerToValue(binding.spinnerCurrency, (ArrayAdapter<CharSequence>) binding.spinnerCurrency.getAdapter(), selectedCurrency); // Set default currency
            // Default account type will be the first item
            if(binding.spinnerAccountType.getAdapter().getCount() > 0) {
                String[] accountTypeValues = getResources().getStringArray(R.array.account_type_values_array);
                if(accountTypeValues.length > 0) selectedAccountType = accountTypeValues[0];
            }

        } else if (editingAccountId != null) {
            loadAccountForEdit(); // Load data if editing
        } else {
            Toast.makeText(this, "Error: Account ID missing for edit.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EDIT_MODE") && intent.getBooleanExtra("EDIT_MODE", false)) {
            isEditMode = true;
            editingAccountId = intent.getStringExtra("ACCOUNT_ID"); // Get the ID to edit
            Log.d(TAG, "Edit Mode enabled for Account ID: " + editingAccountId);
        } else {
            isEditMode = false;
            Log.d(TAG, "Add Mode enabled.");
        }
    }


    private void setupToolbar() {
        setSupportActionBar(binding.toolbarAddAccount);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Edit Account" : "Add New Account"); // Change title based on mode
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

    private void setupSpinners() {
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.account_types_array, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAccountType.setAdapter(typeAdapter);
        binding.spinnerAccountType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] accountTypeValues = getResources().getStringArray(R.array.account_type_values_array);
                if (position >= 0 && position < accountTypeValues.length) {
                    selectedAccountType = accountTypeValues[position];
                    Log.d(TAG, "Selected Account Type: " + selectedAccountType);
                    // Show/hide Link Bank button based on type
                    binding.btnLinkBank.setVisibility("bank".equalsIgnoreCase(selectedAccountType) ? View.VISIBLE : View.GONE);
                } else {
                    selectedAccountType = "";
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { selectedAccountType = "";}
        });

        ArrayAdapter<CharSequence> currencyAdapter = ArrayAdapter.createFromResource(this,
                R.array.currencies_array, android.R.layout.simple_spinner_item);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCurrency.setAdapter(currencyAdapter);
        // Don't set default selection here if in edit mode yet
        // setSpinnerToValue(binding.spinnerCurrency, currencyAdapter, selectedCurrency);

        binding.spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCurrency = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Selected Currency: " + selectedCurrency);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { selectedCurrency = "VND"; }
        });
    }

    private void setSpinnerToValue(Spinner spinner, ArrayAdapter<CharSequence> adapter, String value) {
        if (value == null || adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return; // Exit once found
            }
        }
        Log.w(TAG, "Value '" + value + "' not found in spinner adapter.");
        if(adapter.getCount() > 0) spinner.setSelection(0); // Select first item if value not found
    }


    private void setupListeners() {
        binding.switchIncludeInTotal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            includeInTotal = isChecked;
            Log.d(TAG, "Include in Total: " + includeInTotal);
        });
        binding.btnSelectAccountIcon.setOnClickListener(v -> {
            Toast.makeText(this, "Implement Icon Selection", Toast.LENGTH_SHORT).show();
            selectedIconName = "ic_bank";
            updateIconView(selectedIconName);
        });
        binding.btnManageSharing.setOnClickListener(v -> showShareDialog());
        binding.btnSelectAccountColor.setOnClickListener(v -> openColorPickerDialog());
        binding.btnLinkBank.setOnClickListener(v -> Toast.makeText(this, "Implement Bank Linking", Toast.LENGTH_SHORT).show());
        binding.btnSaveAccount.setOnClickListener(v -> saveOrUpdateAccount()); // Call new save method
    }

    private void loadAccountForEdit() {
        showLoading(true);
        // !!! ADD getAccountById to FirebaseDAO !!!
        firebaseDAO.getAccountById(currentUserId, editingAccountId, new FirebaseDAO.OnAccountRetrievedListener() { // Assume this exists
            @Override
            public void onSuccess(Account account) {
                showLoading(false);
                if (account != null) {
                    existingAccountData = account;
                    existingAccountData.setFirebaseId(editingAccountId); // Ensure ID is set
                    populateFormForEdit();
                } else {
                    Log.e(TAG, "Account not found for ID: " + editingAccountId);
                    Toast.makeText(AddAccountActivity.this, "Account not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Log.e(TAG, "Error loading account for edit", e);
                Toast.makeText(AddAccountActivity.this, "Error loading account data.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


    private void populateFormForEdit() {
        if (existingAccountData == null) return;

        binding.editTextAccountName.setText(existingAccountData.getName());
        binding.editTextInitialBalance.setText(String.format(Locale.US, "%.2f", existingAccountData.getCurrentBalance())); // Display current balance as "initial" in edit mode
        binding.switchIncludeInTotal.setChecked(existingAccountData.isIncludeInTotal());
        updateIconView(existingAccountData.getIcon() != null ? existingAccountData.getIcon() : "ic_account_balance_wallet");
        updateColorView(existingAccountData.getColor() != null ? existingAccountData.getColor() : "#4CAF50");

        // Select Account Type in Spinner
        String[] accountTypeValues = getResources().getStringArray(R.array.account_type_values_array);
        String currentType = existingAccountData.getType();
        selectedAccountType = ""; // Reset before setting
        for (int i = 0; i < accountTypeValues.length; i++) {
            if (accountTypeValues[i].equalsIgnoreCase(currentType)) {
                binding.spinnerAccountType.setSelection(i);
                selectedAccountType = accountTypeValues[i]; // Store the technical value
                break;
            }
        }


        // Show/hide link bank button based on loaded type
        binding.btnLinkBank.setVisibility("bank".equalsIgnoreCase(selectedAccountType) ? View.VISIBLE : View.GONE);


        // Select Currency in Spinner
        ArrayAdapter<CharSequence> currencyAdapter = (ArrayAdapter<CharSequence>) binding.spinnerCurrency.getAdapter();
        selectedCurrency = existingAccountData.getCurrency() != null ? existingAccountData.getCurrency() : "VND"; // Set selectedCurrency first
        setSpinnerToValue(binding.spinnerCurrency, currencyAdapter, selectedCurrency); // Then update spinner

        if (currentUserId.equals(existingAccountData.getOwnerId())) {
            binding.sectionShare.setVisibility(View.VISIBLE); // Hiện layout chia sẻ
            sharedUserIds.clear();
            if (existingAccountData.getSharedWithUids() != null) {
                sharedUserIds.addAll(existingAccountData.getSharedWithUids());
            }
            updateSharedUserDisplay(); // Cập nhật hiển thị danh sách người được chia sẻ
        } else {
            binding.sectionShare.setVisibility(View.GONE); // Ẩn nếu không phải chủ sở hữu
        }
    }

    private void showShareDialog() {
        if (!isEditMode || !currentUserId.equals(existingAccountData.getOwnerId())) {
            Toast.makeText(this, "Only the owner can manage sharing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Xây dựng Dialog phức tạp hơn với danh sách người dùng và nút thêm/xóa
        // Ví dụ đơn giản bằng AlertDialog để thêm email
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share Account");

        final EditText inputEmail = new EditText(this);
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setHint("Enter email to share with");
        builder.setView(inputEmail);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String emailToShare = inputEmail.getText().toString().trim();
            if (!TextUtils.isEmpty(emailToShare) && android.util.Patterns.EMAIL_ADDRESS.matcher(emailToShare).matches()) {
                // --- TODO: TÌM UID TỪ EMAIL (CẦN CLOUD FUNCTION) ---
                findUidByEmailAndShare(emailToShare);
            } else {
                Toast.makeText(this, "Please enter a valid email.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // TODO: Thêm phần hiển thị danh sách người đã chia sẻ và nút xóa họ

        builder.show();
    }


    private void findUidByEmailAndShare(String email) {
        Log.d(TAG, "Attempting to find UID for email: " + email);
        showLoading(true);

        firebaseDAO.findUserByEmail(email, new FirebaseDAO.OnUserFoundListener() {
            @Override
            public void onSuccess(String foundUid) {
                Log.d(TAG, "Found UID: " + foundUid);
                // Kiểm tra tự chia sẻ, đã chia sẻ chưa (như cũ)
                if (foundUid.equals(currentUserId)) { /* ... */ showLoading(false); return; }
                if (sharedUserIds.contains(foundUid)) { /* ... */ showLoading(false); return; }

                // Gọi hàm share của DAO (như cũ)
                firebaseDAO.shareAccountWithUser(currentUserId, editingAccountId, foundUid, existingAccountData.getName(), new FirebaseDAO.OnShareUpdatedListener() {
                    @Override public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(AddAccountActivity.this, "Account shared!", Toast.LENGTH_SHORT).show();
                        sharedUserIds.add(foundUid);
                        updateSharedUserDisplay();
                    }
                    @Override public void onFailure(Exception e) {
                        handleSaveFailure("Failed to update sharing info", e); // Dùng hàm xử lý lỗi chung
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to find user by email", e);
                showLoading(false);
                // Hiển thị lỗi cụ thể hơn nếu có thể (ví dụ: từ exception message)
                String message = e != null && e.getMessage() != null ? e.getMessage() : "User not found or error occurred.";
                // Đặc biệt kiểm tra lỗi PERMISSION_DENIED
                if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    message = "Permission denied to search users. Check Firestore rules.";
                }
                Toast.makeText(AddAccountActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void updateSharedUserDisplay() {
        // TODO: Cập nhật UI (ví dụ: một TextView hoặc RecyclerView) để hiển thị danh sách sharedUserIds
        // Ví dụ đơn giản:
        binding.textSharedWithInfo.setText("Shared with: " + TextUtils.join(", ", sharedUserIds));
        binding.textSharedWithInfo.setVisibility(sharedUserIds.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // --- HÀM MỚI: Xử lý hủy chia sẻ (cần UI để chọn người hủy) ---
    private void unshareWithSelectedUser(String userToUnshareUid) {
        if (!isEditMode || !currentUserId.equals(existingAccountData.getOwnerId())) return;
        showLoading(true);
        firebaseDAO.unshareAccountWithUser(currentUserId, editingAccountId, userToUnshareUid, new FirebaseDAO.OnShareUpdatedListener() {
            @Override public void onSuccess() {
                showLoading(false);
                Toast.makeText(AddAccountActivity.this, "Sharing removed.", Toast.LENGTH_SHORT).show();
                sharedUserIds.remove(userToUnshareUid);
                updateSharedUserDisplay();
            }
            @Override public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(AddAccountActivity.this, "Failed to remove sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void openColorPickerDialog() {
        Toast.makeText(this, "Implement Color Picker Dialog", Toast.LENGTH_SHORT).show();
        selectedColorHex = "#2196F3";
        updateColorView(selectedColorHex);
    }

    private void updateIconView(String iconName) {
        selectedIconName = iconName;
        if (binding == null || binding.imageViewSelectedAccountIcon == null) return;
        try {
            int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            binding.imageViewSelectedAccountIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_account_balance_wallet);
        } catch (Exception e) {
            Log.e(TAG, "Error setting icon resource: " + iconName, e);
            binding.imageViewSelectedAccountIcon.setImageResource(R.drawable.ic_account_balance_wallet);
        }
    }

    private void updateColorView(String colorHex) {
        selectedColorHex = colorHex;
        if (binding == null || binding.viewSelectedAccountColor == null) return;
        try {
            binding.viewSelectedAccountColor.setBackgroundColor(Color.parseColor(colorHex));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid color hex: " + colorHex, e);
            binding.viewSelectedAccountColor.setBackgroundColor(Color.GRAY);
        }
    }

    private void saveOrUpdateAccount() { // Renamed from saveAccount
        String accountName = binding.editTextAccountName.getText().toString().trim();
        String balanceStr = binding.editTextInitialBalance.getText().toString().trim(); // Use this for balance in both modes

        if (TextUtils.isEmpty(accountName)) {
            binding.inputLayoutAccountName.setError("Account Name is required.");
            return;
        } else {
            binding.inputLayoutAccountName.setError(null);
        }

        if (TextUtils.isEmpty(selectedAccountType)) {
            Toast.makeText(this, "Please select an account type.", Toast.LENGTH_SHORT).show();
            return;
        }

        double balance = 0.0;
        if (!TextUtils.isEmpty(balanceStr)) {
            try {
                balance = Double.parseDouble(balanceStr);
                // Allow 0 balance, but maybe not negative for initial in add mode?
                // if (!isEditMode && balance < 0) {
                //    binding.inputLayoutInitialBalance.setError("Initial balance cannot be negative.");
                //    return;
                // }
                binding.inputLayoutInitialBalance.setError(null);
            } catch (NumberFormatException e) {
                binding.inputLayoutInitialBalance.setError("Invalid number format.");
                return;
            }
        } else {
            // Treat empty as 0
            binding.inputLayoutInitialBalance.setError(null);
        }


        showLoading(true);

        Account accountToSave;
        if (isEditMode) {
            if (existingAccountData == null || editingAccountId == null) {
                handleSaveFailure("Error updating: Missing data", new IllegalStateException("Missing edit data"));
                return;
            }
            accountToSave = existingAccountData; // Update the existing object
            Log.d(TAG, "Preparing to UPDATE account ID: " + editingAccountId);
        } else {
            accountToSave = new Account(); // Create a new object for adding
            accountToSave.setCreatedAt(Timestamp.now()); // Set creation time only for new accounts
            Log.d(TAG, "Preparing to ADD new account.");
        }

        // Update fields from the form for both Add and Edit
        accountToSave.setName(accountName);
        accountToSave.setType(selectedAccountType);
        accountToSave.setCurrency(selectedCurrency);
        accountToSave.setCurrentBalance(balance); // Update currentBalance from the form field
        accountToSave.setIcon(selectedIconName);
        accountToSave.setColor(selectedColorHex);
        accountToSave.setIncludeInTotal(includeInTotal);
        // createdAt is only set for new accounts


        if (isEditMode) {
            // !!! ADD updateAccount to FirebaseDAO !!!
            firebaseDAO.updateAccount(currentUserId, editingAccountId, accountToSave, new FirebaseDAO.OnAccountUpdatedListener() { // Assume this exists
                @Override public void onSuccess() { handleSaveSuccess("Account Updated!"); }
                @Override public void onFailure(Exception e) { handleSaveFailure("Error updating account", e); }
            });
        } else {
            firebaseDAO.addAccount(currentUserId, accountToSave, new FirebaseDAO.OnAccountAddedListener() {
                @Override public void onSuccess(String id) { handleSaveSuccess("Account Saved!"); }
                @Override public void onFailure(Exception e) { handleSaveFailure("Error saving account", e); }
            });
        }
    }

    private void handleSaveSuccess(String message) {
        showLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK); // Notify previous activity of success
        finish();
    }

    private void handleSaveFailure(String logMessage, Exception e) {
        Log.e(TAG, logMessage, e);
        showLoading(false);
        Toast.makeText(this, logMessage + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
    }


    private void showLoading(boolean isLoading) {
        if (binding != null && binding.progressBarAddAccount != null && binding.btnSaveAccount != null) {
            binding.progressBarAddAccount.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSaveAccount.setEnabled(!isLoading);
            binding.editTextAccountName.setEnabled(!isLoading);
            binding.spinnerAccountType.setEnabled(!isLoading);
            binding.editTextInitialBalance.setEnabled(!isLoading);
            binding.spinnerCurrency.setEnabled(!isLoading);
            binding.switchIncludeInTotal.setEnabled(!isLoading);
            binding.btnSelectAccountIcon.setEnabled(!isLoading);
            binding.btnSelectAccountColor.setEnabled(!isLoading);
            binding.btnLinkBank.setEnabled(!isLoading);
        } else {
            Log.w(TAG, "Binding or its views are null in showLoading");
        }
    }

    // Interfaces needed for DAO calls (DAO should define these)
    public interface OnAccountRetrievedListener { void onSuccess(Account account); void onFailure(Exception e); }
    public interface OnAccountUpdatedListener { void onSuccess(); void onFailure(Exception e); }

}