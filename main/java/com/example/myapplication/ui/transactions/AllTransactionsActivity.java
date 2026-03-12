package com.example.myapplication.ui.transactions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.data.datasource.FirebaseDAO;
import com.example.myapplication.data.model.Account;
import com.example.myapplication.data.model.Category;
import com.example.myapplication.data.model.FirebaseModelBase;
import com.example.myapplication.data.model.Transaction;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.databinding.ActivityAllTransactionsBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AllTransactionsActivity extends AppCompatActivity
        implements TransactionAdapter.OnTransactionInteractionListener {

    private static final String TAG = "AllTransactionsActivity";

    private ActivityAllTransactionsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;

    private TransactionAdapter transactionAdapter;
    private List<Transaction> allTransactionsList = new ArrayList<>();
    private Map<String, Category> categoryMap = new HashMap<>(); // Needed for the Transaction Adapter
    private List<Category> categoriesForSpinner = new ArrayList<>(); // Separate list for Spinner
    private List<Account> accountsForSpinner = new ArrayList<>();   // Separate list for Spinner

    private ArrayAdapter<String> categorySpinnerAdapter;
    private ArrayAdapter<String> accountSpinnerAdapter;

    private Category selectedCategory = null;
    private Account selectedAccount = null;
    private Calendar selectedDateCalendar = Calendar.getInstance();
    private boolean isEditMode = false;
    private String editingTransactionId = null;
    private Transaction editingTransactionData = null;

    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // Launcher to potentially reload data if Add/Edit was successful in another Activity
    // (We might not need it if doing everything inline, but good practice)
    private ActivityResultLauncher<Intent> crudTransactionLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllTransactionsBinding.inflate(getLayoutInflater());
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
        initializeFormSpinners(); // Initialize spinners in the form
        setupFormListeners();     // Setup listeners for the form elements
        setupActivityLaunchers(); // Setup launchers
        loadInitialData();        // Load Categories/Accounts first
    }

    // --- Toolbar and Back Button ---
    private void setupToolbar() {
        setSupportActionBar(binding.toolbarAllTransactions);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (isFormVisible()) {
                showForm(false); // Hide form if visible
            } else {
                finish(); // Otherwise, close activity
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isFormVisible()) {
            showForm(false); // Hide form on back press
        } else {
            super.onBackPressed(); // Default back behavior
        }
    }

    // --- RecyclerView Setup ---
    private void setupRecyclerView() {
        binding.recyclerViewAllTransactions.setLayoutManager(new LinearLayoutManager(this));
        // Pass 'this' as the listener for edit/delete clicks
        transactionAdapter = new TransactionAdapter(this, allTransactionsList, categoryMap, this);
        binding.recyclerViewAllTransactions.setAdapter(transactionAdapter);
    }

    // --- Form Spinners Setup ---
    private void initializeFormSpinners() {
        // Category Spinner in the Add/Edit Form
        categorySpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categorySpinnerAdapter); // Target the form's spinner
        binding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < categoriesForSpinner.size()) {
                    selectedCategory = categoriesForSpinner.get(position);
                } else {
                    selectedCategory = null;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedCategory = null; }
        });

        // Account Spinner in the Add/Edit Form
        accountSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        accountSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAccount.setAdapter(accountSpinnerAdapter); // Target the form's spinner
        binding.spinnerAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < accountsForSpinner.size()) {
                    selectedAccount = accountsForSpinner.get(position);
                    String currencyCode = selectedAccount.getCurrency() != null ? selectedAccount.getCurrency() : "VND";
                    binding.inputLayoutAmount.setPrefixText(currencyCode + " ");
                } else {
                    selectedAccount = null;
                    binding.inputLayoutAmount.setPrefixText("VND ");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                selectedAccount = null;
                binding.inputLayoutAmount.setPrefixText("VND ");
            }
        });
    }

    // --- Form Listeners Setup ---
    private void setupFormListeners() {
        // FAB to show the Add form
        binding.fabAddTransactionAll.setOnClickListener(v -> {
            isEditMode = false;
            editingTransactionId = null;
            editingTransactionData = null;
            binding.formTitle.setText("Add Transaction");
            clearForm(); // Clear form for adding new
            updateDateLabel(); // Show current date
            showForm(true);
        });

        // Cancel button within the form
        binding.btnCancelForm.setOnClickListener(v -> showForm(false));

        // Save button within the form
        binding.btnSaveForm.setOnClickListener(v -> saveOrUpdateTransaction());

        // Date TextView click to open DatePicker
        binding.textViewDate.setOnClickListener(v -> openDatePickerDialog());
    }

    // --- Activity Result Launcher Setup ---
    private void setupActivityLaunchers() {
        // This might be useful if you navigate away and need to refresh
        // For inline editing, it might not be strictly necessary unless AddTransactionActivity is reused
        crudTransactionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Returned from another Activity with RESULT_OK. Reloading data.");
                        loadInitialData(); // Reload data on successful return
                    }
                });
    }


    // --- Data Loading ---
    private void loadInitialData() {
        showListLoading(true);
        int tasksToComplete = 3; // Categories, Accounts, Transactions
        int[] tasksCompleted = {0};

        Runnable checkCompletion = () -> {
            tasksCompleted[0]++;
            if (tasksCompleted[0] >= tasksToComplete) {
                showListLoading(false);
                // Ensure empty state is correct after all loads
                if(allTransactionsList.isEmpty()){
                    showEmptyState("No transactions recorded yet.");
                }
            }
        };

        // 1. Load Categories (for spinner and map)
        firebaseDAO.getAllCategories(currentUserId, new FirebaseDAO.OnCategoriesRetrievedListener() {
            @Override public void onSuccess(List<Category> categories) {
                categoryMap.clear();
                categoriesForSpinner.clear();
                if (categories != null) {
                    categoriesForSpinner.addAll(categories);
                    for (Category category : categories) {
                        if (category.getFirebaseId() != null) {
                            categoryMap.put(category.getFirebaseId(), category);
                        }
                    }
                }
                List<String> names = categoriesForSpinner.stream().map(Category::getName).collect(Collectors.toList());
                categorySpinnerAdapter.clear(); categorySpinnerAdapter.addAll(names);
                categorySpinnerAdapter.notifyDataSetChanged();
                checkCompletion.run();
            }
            @Override public void onFailure(Exception e) { Log.e(TAG,"Cat load fail",e); checkCompletion.run(); }
        });

        // 2. Load Accounts (for spinner)
        firebaseDAO.getAllAccounts(currentUserId, new FirebaseDAO.OnAccountsRetrievedListener() {
            @Override public void onSuccess(List<Account> accounts) {
                accountsForSpinner.clear();
                if (accounts != null) {
                    accountsForSpinner.addAll(accounts);
                }
                List<String> names = accountsForSpinner.stream().map(Account::getName).collect(Collectors.toList());
                accountSpinnerAdapter.clear(); accountSpinnerAdapter.addAll(names);
                accountSpinnerAdapter.notifyDataSetChanged();
                checkCompletion.run();
            }
            @Override public void onFailure(Exception e) { Log.e(TAG,"Acc load fail",e); checkCompletion.run(); }
        });

        // 3. Load Transactions (for RecyclerView)
        fetchTransactions(checkCompletion); // Pass the completion checker
    }

    private void showEmptyState(String s) {
        binding.textNoTransactionsAll.setText(s);
        binding.textNoTransactionsAll.setVisibility(View.VISIBLE);
    }

    private void fetchTransactions(Runnable onComplete) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "fetchTransactions: Cannot fetch, currentUserId is null or empty.");
            if(onComplete != null) onComplete.run(); // Notify completion even if we can't fetch
            showListLoading(false); // Ensure loading is hidden
            showEmptyState("Error: User not identified."); // Show error state
            return;
        }

        Log.d(TAG, "fetchTransactions: Fetching transactions for user: " + currentUserId);

        // Use OnTransactionsRetrievedListener (plural 's') which returns a List
        firebaseDAO.getAllTransactions(currentUserId, new FirebaseDAO.OnTransactionsRetrievedListener() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                // Log the raw result size
                Log.d(TAG, "fetchTransactions - onSuccess: Received " + (transactions != null ? transactions.size() : "null") + " transactions from DAO.");

                allTransactionsList.clear(); // Clear the local list first

                if (transactions != null && !transactions.isEmpty()) {
                    allTransactionsList.addAll(transactions); // Add new data
                    Log.d(TAG, "fetchTransactions - onSuccess: Populated local list. Size: " + allTransactionsList.size());
                    sortTransactions(); // Sort the populated list
                    binding.recyclerViewAllTransactions.setVisibility(View.VISIBLE); // Show RecyclerView
                    binding.textNoTransactionsAll.setVisibility(View.GONE); // Hide empty text
                } else {
                    Log.d(TAG, "fetchTransactions - onSuccess: Transaction list is null or empty.");
                    showEmptyState("No transactions recorded yet."); // Show empty state
                }

                // Update adapter regardless of whether the list has items or is empty
                if(transactionAdapter != null) {
                    transactionAdapter.updateData(allTransactionsList, categoryMap);
                    Log.d(TAG, "fetchTransactions - onSuccess: Adapter updated.");
                } else {
                    Log.e(TAG, "fetchTransactions - onSuccess: transactionAdapter is null!");
                }

                // Notify completion after updating adapter
                if(onComplete != null) onComplete.run();
                // Ensure loading is hidden AFTER processing
                showListLoading(false);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "fetchTransactions - onFailure: Failed to load transactions", e);
                Toast.makeText(AllTransactionsActivity.this, "Error loading transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                allTransactionsList.clear(); // Clear list on failure
                if(transactionAdapter != null) {
                    transactionAdapter.updateData(allTransactionsList, categoryMap); // Update adapter with empty list
                }
                if(onComplete != null) onComplete.run(); // Notify completion even on failure
                showListLoading(false); // Ensure loading is hidden
                showEmptyState("Failed to load transactions."); // Show error state
            }
        });
    }


    private void sortTransactions() {
        try {
            Collections.sort(allTransactionsList, (t1, t2) -> {
                Timestamp ts1 = t1.getTimestamp(); Timestamp ts2 = t2.getTimestamp();
                if (ts1 == null && ts2 == null) return 0;
                if (ts1 == null) return 1;
                if (ts2 == null) return -1;
                return ts2.compareTo(ts1); // Descending
            });
        } catch (Exception e) { Log.e(TAG, "Error sorting transactions", e); }
    }

    // --- Interface Implementation (Adapter Clicks) ---
    @Override
    public void onEditClick(Transaction transaction, int position) {
        if (transaction == null || transaction.getFirebaseId() == null) {
            Toast.makeText(this, "Cannot edit this item.", Toast.LENGTH_SHORT).show();
            return;
        }
        isEditMode = true;
        editingTransactionId = transaction.getFirebaseId();
        editingTransactionData = transaction; // Keep original data for comparison/update
        binding.formTitle.setText("Edit Transaction");
        populateFormForEdit(); // Populate form with this transaction's data
        showForm(true);        // Show the form
    }

    @Override
    public void onDeleteClick(Transaction transaction, int position) {
        if (transaction == null || transaction.getFirebaseId() == null) return;
        showDeleteConfirmationDialog(transaction, position);
    }

    // --- Form Visibility and State ---
    private boolean isFormVisible() {
        return binding.cardAddEditForm.getVisibility() == View.VISIBLE;
    }

    private void showForm(boolean show) {
        binding.cardAddEditForm.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewAllTransactions.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.fabAddTransactionAll.setVisibility(show ? View.GONE : View.VISIBLE);

        // Show/Hide empty text correctly based on list state when hiding form
        if (!show) {
            isEditMode = false;
            editingTransactionId = null;
            editingTransactionData = null;
            if (allTransactionsList.isEmpty()) {
                showEmptyState("No transactions recorded yet.");
            } else {
                binding.textNoTransactionsAll.setVisibility(View.GONE);
            }
        } else {
            binding.textNoTransactionsAll.setVisibility(View.GONE); // Hide empty text when form is shown
        }
    }

    private void clearForm() {
        binding.editTextAmount.setText("");
        binding.editTextDescription.setText("");
        binding.editTextNote.setText("");
        binding.radioExpense.setChecked(true);
        if (!categoriesForSpinner.isEmpty()) binding.spinnerCategory.setSelection(0); else binding.spinnerCategory.setAdapter(null); // Reset adapter if empty
        if (!accountsForSpinner.isEmpty()) binding.spinnerAccount.setSelection(0); else binding.spinnerAccount.setAdapter(null);
        selectedCategory = categoriesForSpinner.isEmpty() ? null : categoriesForSpinner.get(0);
        selectedAccount = accountsForSpinner.isEmpty() ? null : accountsForSpinner.get(0);
        selectedDateCalendar = Calendar.getInstance();
        updateDateLabel(); // Update to current date
        binding.inputLayoutAmount.setError(null);
        binding.inputLayoutDescription.setError(null);
        String defaultCurrency = (accountsForSpinner.isEmpty() || accountsForSpinner.get(0).getCurrency() == null) ? "VND" : accountsForSpinner.get(0).getCurrency();
        binding.inputLayoutAmount.setPrefixText(defaultCurrency + " ");
    }


    private void populateFormForEdit() {
        if (editingTransactionData == null) return;

        binding.editTextAmount.setText(String.format(Locale.US, "%.2f", editingTransactionData.getAmount()));
        binding.editTextDescription.setText(editingTransactionData.getDescription());
        binding.editTextNote.setText(editingTransactionData.getNote() != null ? editingTransactionData.getNote() : "");

        binding.radioIncome.setChecked("income".equalsIgnoreCase(editingTransactionData.getType()));
        binding.radioExpense.setChecked("expense".equalsIgnoreCase(editingTransactionData.getType()));


        if (editingTransactionData.getCategoryId() != null) {
            selectSpinnerItemById(binding.spinnerCategory, categoriesForSpinner, editingTransactionData.getCategoryId(), categorySpinnerAdapter);
        } else if (!categoriesForSpinner.isEmpty()) {
            binding.spinnerCategory.setSelection(0); // Default to first if no category was saved
        }


        if (editingTransactionData.getAccountId() != null) {
            selectSpinnerItemById(binding.spinnerAccount, accountsForSpinner, editingTransactionData.getAccountId(), accountSpinnerAdapter);
            // Update currency prefix based on loaded account
            Account loadedAcc = accountsForSpinner.stream().filter(a -> editingTransactionData.getAccountId().equals(a.getFirebaseId())).findFirst().orElse(null);
            String currencyCode = (loadedAcc != null && loadedAcc.getCurrency() != null) ? loadedAcc.getCurrency() : "VND";
            binding.inputLayoutAmount.setPrefixText(currencyCode + " ");
        } else if (!accountsForSpinner.isEmpty()) {
            binding.spinnerAccount.setSelection(0);
            String defaultCurrency = (accountsForSpinner.get(0).getCurrency() != null) ? accountsForSpinner.get(0).getCurrency() : "VND";
            binding.inputLayoutAmount.setPrefixText(defaultCurrency + " ");
        }

        if (editingTransactionData.getTimestamp() != null) {
            selectedDateCalendar.setTime(editingTransactionData.getTimestamp().toDate());
        } else {
            selectedDateCalendar = Calendar.getInstance(); // Fallback to current date if timestamp is missing
        }
        updateDateLabel();
    }

    // Helper to select spinner item based on Firebase ID
    private <T extends FirebaseModelBase> void selectSpinnerItemById(Spinner spinner, List<T> itemList, String targetId, ArrayAdapter<String> adapter) {
        if (targetId == null || itemList == null || adapter == null) return;
        for (int i = 0; i < itemList.size(); i++) {
            if (targetId.equals(itemList.get(i).getFirebaseId())) {
                spinner.setSelection(i);
                // Update selected variable (important!)
                if (itemList.get(i) instanceof Category) {
                    selectedCategory = (Category) itemList.get(i);
                } else if (itemList.get(i) instanceof Account) {
                    selectedAccount = (Account) itemList.get(i);
                }
                return; // Found it
            }
        }
        // If ID not found in list (e.g., category/account was deleted), select first item
        if (!itemList.isEmpty()) {
            spinner.setSelection(0);
            if (itemList.get(0) instanceof Category) selectedCategory = (Category) itemList.get(0);
            else if (itemList.get(0) instanceof Account) selectedAccount = (Account) itemList.get(0);
        }
    }

    // Base class or interface for models having getFirebaseId()
    // Make sure Account and Category implement FirebaseModelBase or have getFirebaseId()


    // --- Date Picker ---
    private void openDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            selectedDateCalendar.set(Calendar.YEAR, year);
            selectedDateCalendar.set(Calendar.MONTH, month);
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, day);
            updateDateLabel();
        };
        new DatePickerDialog(this, dateSetListener, selectedDateCalendar.get(Calendar.YEAR), selectedDateCalendar.get(Calendar.MONTH), selectedDateCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        binding.textViewDate.setText(displayDateFormat.format(selectedDateCalendar.getTime()));
    }

    // --- Save/Update Logic ---
    private void saveOrUpdateTransaction() {
        String amountStr = binding.editTextAmount.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();
        String note = binding.editTextNote.getText().toString().trim();
        int selectedTypeId = binding.radioGroupType.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedTypeId);
        String transactionType = (selectedRadioButton != null && selectedRadioButton.getId() == R.id.radio_income) ? "income" : "expense";

        if (!validateInput(amountStr, description)) {
            return;
        }
        double amount = 0.0;
        try {
            amount = Double.parseDouble(amountStr);
            if(amount <= 0) throw new NumberFormatException();
            binding.inputLayoutAmount.setError(null);
        } catch (NumberFormatException e) {
            binding.inputLayoutAmount.setError("Invalid positive amount.");
            return;
        }

        showFormLoading(true);

        Transaction transactionToSave;
        if (isEditMode) {
            if (editingTransactionData == null || editingTransactionId == null) {
                handleSaveFailure("Error updating: Missing data", new IllegalStateException("Missing edit data"));
                return;
            }
            transactionToSave = editingTransactionData;
        } else {
            transactionToSave = new Transaction();
        }

        transactionToSave.setType(transactionType);
        transactionToSave.setAmount(amount);
        transactionToSave.setCurrency(selectedAccount != null ? selectedAccount.getCurrency() : "VND");
        transactionToSave.setDescription(description);
        transactionToSave.setDate(firebaseDateFormat.format(selectedDateCalendar.getTime()));
        transactionToSave.setTimestamp(new Timestamp(selectedDateCalendar.getTime())); // Use Firestore Timestamp
        transactionToSave.setCategoryId(selectedCategory != null ? selectedCategory.getFirebaseId() : null);
        transactionToSave.setAccountId(selectedAccount != null ? selectedAccount.getFirebaseId() : null);
        transactionToSave.setNote(note.isEmpty() ? null : note);

        if (isEditMode) {
            firebaseDAO.updateTransaction(currentUserId, editingTransactionId, transactionToSave, new FirebaseDAO.OnTransactionUpdatedListener() {
                @Override public void onSuccess() { handleSaveSuccess("Transaction Updated!"); }
                @Override public void onFailure(Exception e) { handleSaveFailure("Error updating transaction", e); }
            });
        } else {
            firebaseDAO.addTransaction(currentUserId, transactionToSave, new FirebaseDAO.OnTransactionAddedListener() {
                @Override public void onSuccess(String id) { handleSaveSuccess("Transaction Saved!"); }
                @Override public void onFailure(Exception e) { handleSaveFailure("Error saving transaction", e); }
            });
        }
    }


    private boolean validateInput(String amountStr, String description) {
        boolean isValid = true;
        if (TextUtils.isEmpty(amountStr)) { binding.inputLayoutAmount.setError("Amount required"); isValid = false;}
        else { try { double a=Double.parseDouble(amountStr); if(a<=0){binding.inputLayoutAmount.setError("Positive amount required"); isValid=false;} else {binding.inputLayoutAmount.setError(null);}}catch(NumberFormatException e){binding.inputLayoutAmount.setError("Invalid number"); isValid=false;}}
        if (TextUtils.isEmpty(description)) { binding.inputLayoutDescription.setError("Description required"); isValid = false;} else {binding.inputLayoutDescription.setError(null);}
        if (selectedCategory == null && !categoriesForSpinner.isEmpty()) { Toast.makeText(this,"Select category", Toast.LENGTH_SHORT).show(); isValid = false; } // Check only if list has items
        if (selectedAccount == null && !accountsForSpinner.isEmpty()) { Toast.makeText(this,"Select account", Toast.LENGTH_SHORT).show(); isValid = false; } // Check only if list has items
        return isValid;
    }

    // --- Delete Logic ---
    private void showDeleteConfirmationDialog(Transaction transaction, int position) {
        if (transaction == null || transaction.getFirebaseId() == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Delete '" + transaction.getDescription()+"'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTransactionFromDb(transaction, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTransactionFromDb(Transaction transaction, int position) {
        showListLoading(true);
        firebaseDAO.deleteTransaction(currentUserId, transaction.getFirebaseId(), new FirebaseDAO.OnTransactionDeletedListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Transaction deleted from DB. Updating balance...");
                handleDeletionSuccess(transaction, position); // Cập nhật RecyclerView trước

                // --- GỌI HÀM CẬP NHẬT SỐ DƯ CHO XÓA ---
                // Xác định xem giao dịch bị xóa là income hay không
                boolean wasIncome = "income".equalsIgnoreCase(transaction.getType());
                // Gọi hàm updateBalanceAfterAddOrDelete từ AddTransactionActivity (hoặc tạo hàm tương tự ở đây)
                // Truyền isAddition = false
                updateBalanceAfterDeletion(transaction.getAccountId(), transaction.getAmount(), wasIncome);

                Toast.makeText(AllTransactionsActivity.this,"Transaction Deleted",Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Exception e) {
                handleDeletionFailure(e); // Hàm này đã có
            }
        });
    }


    private void updateBalanceAfterDeletion(String accountId, double transactionAmount, boolean wasIncome) {
        if (accountId == null) {
            Log.e(TAG,"Cannot update balance after delete: Account ID null");
            Toast.makeText(this, "Could not update balance: Account ID missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Tương tự như hàm trong AddTransactionActivity nhưng isAddition luôn là false
        firebaseDAO.getAccountById(currentUserId, accountId, new FirebaseDAO.OnAccountRetrievedListener() {
            @Override
            public void onSuccess(Account currentAccountData) {
                if (currentAccountData == null) {
                    Log.e(TAG,"Cannot update balance after delete: Account "+accountId+" not found");
                    Toast.makeText(AllTransactionsActivity.this, "Could not update balance: Account not found.", Toast.LENGTH_SHORT).show();
                    return;
                }
                double currentBalance = currentAccountData.getCurrentBalance();
                // Hoàn tác: Trừ Income, Cộng Expense
                double amountChange = wasIncome ? -transactionAmount : transactionAmount;
                double newBalance = currentBalance + amountChange;
                Log.d(TAG, "Updating balance after DELETION for acc " + accountId + ": " + currentBalance + " -> " + newBalance);

                firebaseDAO.updateAccountBalance(currentUserId, accountId, newBalance, new FirebaseDAO.OnAccountUpdatedListener() {
                    @Override public void onSuccess() { Log.d(TAG, "Account balance updated successfully after deletion."); }
                    @Override public void onFailure(Exception e) {
                        Log.e(TAG, "Balance update failed after deletion", e);
                        Toast.makeText(AllTransactionsActivity.this, "Balance update failed after delete.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG,"Balance error: Fetching account failed for deletion update.", e);
                Toast.makeText(AllTransactionsActivity.this, "Could not update balance: Failed fetching account.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void handleDeletionSuccess(Transaction deletedTransaction, int position) {
        showListLoading(false);
        if (position >= 0 && position < allTransactionsList.size() && allTransactionsList.get(position).getFirebaseId().equals(deletedTransaction.getFirebaseId())) {
            allTransactionsList.remove(position);
            transactionAdapter.notifyItemRemoved(position);
            if (allTransactionsList.isEmpty()) { showEmptyState("No transactions yet."); }
            // TODO: Trigger balance update
        } else {
            Log.w(TAG, "Delete position mismatch. Reloading.");
            loadInitialData(); // Reload if mismatch
        }
    }

    private void handleDeletionFailure(Exception e) {
        Log.e(TAG, "Failed to delete transaction", e);
        showListLoading(false);
        Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }


    // --- Success/Failure Handlers ---
    private void handleSaveSuccess(String message) {
        showFormLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        showForm(false);
        loadInitialData(); // Reload list data
        // TODO: Trigger balance update
    }

    private void handleSaveFailure(String logMessage, Exception e) {
        Log.e(TAG, logMessage, e);
        showFormLoading(false);
        Toast.makeText(this, logMessage + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    // --- Loading Indicators ---
    private void showListLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBarAllTransactions.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.recyclerViewAllTransactions.setVisibility(isLoading ? View.GONE : (allTransactionsList.isEmpty() && !isLoading ? View.GONE : View.VISIBLE) ); // Hide if empty after load
            binding.textNoTransactionsAll.setVisibility(isLoading ? View.GONE : (allTransactionsList.isEmpty() ? View.VISIBLE : View.GONE)); // Show if empty after load
        }
    }

    private void showFormLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBarSaveTransaction.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSaveForm.setEnabled(!isLoading);
            binding.btnCancelForm.setEnabled(!isLoading);
            binding.editTextAmount.setEnabled(!isLoading);
            binding.editTextDescription.setEnabled(!isLoading);
            binding.spinnerCategory.setEnabled(!isLoading);
            binding.spinnerAccount.setEnabled(!isLoading);
            binding.textViewDate.setEnabled(!isLoading);
            binding.editTextNote.setEnabled(!isLoading);
            if (binding.radioGroupType != null) {
                for (int i = 0; i < binding.radioGroupType.getChildCount(); i++) {
                    binding.radioGroupType.getChildAt(i).setEnabled(!isLoading);
                }
            }
        }
    }

    // Trong AllTransactionsActivity -> deleteTransactionFromDb -> onSuccess


    // --- Hàm mới trong AllTransactionsActivity ---
    private void updateDeletedTransactionBalance(String accountId, double amount, boolean needsToAddBack) { // needsToAddBack = !wasIncome
        if(currentUserId == null || accountId == null) return;
        firebaseDAO.getAccountById(currentUserId, accountId, new FirebaseDAO.OnAccountRetrievedListener() {
            @Override public void onSuccess(Account account) {
                if(account != null) {
                    double current = account.getCurrentBalance();
                    double newBalance = current + (needsToAddBack ? amount : -amount); // Cộng lại expense, trừ đi income đã xóa
                    firebaseDAO.updateAccountBalance(currentUserId, accountId, newBalance, new FirebaseDAO.OnAccountUpdatedListener() {
                        @Override public void onSuccess() { Log.d(TAG, "Balance updated after deletion."); }
                        @Override public void onFailure(Exception e) { Log.e(TAG, "Failed to update balance after deletion.", e); }
                    });
                }
            }
            @Override public void onFailure(Exception e) { Log.e(TAG, "Failed to get account for balance update after deletion.", e); }
        });
    }

    // --- DAO Interfaces (Keep for clarity, implementation is in FirebaseDAO) ---

    // --- Helper Interface for Spinner Selection ---
    // Ensure Account and Category implement this or have getFirebaseId()
}