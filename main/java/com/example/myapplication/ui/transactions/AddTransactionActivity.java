package com.example.myapplication.ui.transactions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.databinding.ActivityAddTransactionBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AddTransactionActivity extends AppCompatActivity implements FirebaseModelBase {

    private static final String TAG = "AddTransactionActivity";

    private ActivityAddTransactionBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;

    private List<Category> categoryList = new ArrayList<>();
    private List<Account> accountList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> accountAdapter;

    private Category selectedCategory = null;
    private Account selectedAccount = null;
    private Calendar selectedDateCalendar = Calendar.getInstance();
    private boolean isEditMode = false;
    private String editingTransactionId = null;
    private Transaction existingTransactionData = null;

    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    public String getFirebaseId() {
        return isEditMode ? editingTransactionId : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
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

        handleIntent();
        setupHeader();
        initializeSpinners();
        setupDatePicker();
        setupListeners();
        loadInitialDataAndExistingTransaction();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        isEditMode = intent != null && intent.hasExtra("EDIT_MODE") && intent.getBooleanExtra("EDIT_MODE", false);
        if (isEditMode) {
            editingTransactionId = intent.getStringExtra("TRANSACTION_ID");
            Log.d(TAG, "Edit Mode enabled for Transaction ID: " + editingTransactionId);
            if (editingTransactionId == null) {
                Toast.makeText(this, "Error: Missing Transaction ID for editing.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Log.d(TAG, "Add Mode enabled.");
            updateDateLabel();
        }
    }

    private void setupHeader() {
        binding.textViewTitle.setText(isEditMode ? "Edit Transaction" : "Add Transaction");
        binding.buttonBack.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeSpinners() {
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);
        binding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = (position >= 0 && position < categoryList.size()) ? categoryList.get(position) : null;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedCategory = null; }
        });

        accountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAccount.setAdapter(accountAdapter);
        binding.spinnerAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < accountList.size()) {
                    selectedAccount = accountList.get(position);
                    String currencyCode = selectedAccount.getCurrency() != null ? selectedAccount.getCurrency() : "VND";
                    if (binding.inputLayoutAmount != null) binding.inputLayoutAmount.setPrefixText(currencyCode + " ");
                } else {
                    selectedAccount = null;
                    if (binding.inputLayoutAmount != null) binding.inputLayoutAmount.setPrefixText("VND ");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                selectedAccount = null;
                if (binding.inputLayoutAmount != null) binding.inputLayoutAmount.setPrefixText("VND ");
            }
        });
    }

    private void setupDatePicker() {
        if (binding.textViewDate != null) {
            binding.textViewDate.setOnClickListener(v -> openDatePickerDialog());
        }
    }

    private void openDatePickerDialog() {
        new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            selectedDateCalendar.set(Calendar.YEAR, year);
            selectedDateCalendar.set(Calendar.MONTH, monthOfYear);
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        },
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        if (binding != null && binding.textViewDate != null) {
            binding.textViewDate.setText(displayDateFormat.format(selectedDateCalendar.getTime()));
        }
    }

    private void setupListeners() {
        if (binding.btnSaveTransaction != null) {
            binding.btnSaveTransaction.setOnClickListener(v -> saveOrUpdateTransaction());
        }
    }

    private void loadInitialDataAndExistingTransaction() {
        showLoading(true);
        if (binding.btnSaveTransaction != null) binding.btnSaveTransaction.setEnabled(false);

        int tasksToComplete = isEditMode ? 3 : 2;
        int[] tasksCompleted = {0};

        Runnable checkCompletion = () -> {
            tasksCompleted[0]++;
            if (tasksCompleted[0] >= tasksToComplete) {
                showLoading(false);
                if (binding.btnSaveTransaction != null) binding.btnSaveTransaction.setEnabled(true);
                if (isEditMode) {
                    if (existingTransactionData != null) {
                        populateFormForEdit();
                    } else {
                        Toast.makeText(this, "Error loading transaction details.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }
        };

        firebaseDAO.getAllCategories(currentUserId, new FirebaseDAO.OnCategoriesRetrievedListener() {
            @Override
            public void onSuccess(List<Category> categories) {
                categoryList.clear();
                if (categories != null) categoryList.addAll(categories);
                List<String> names = categoryList.stream().map(Category::getName).collect(Collectors.toList());
                if (categoryAdapter != null) {
                    categoryAdapter.clear(); categoryAdapter.addAll(names); categoryAdapter.notifyDataSetChanged();
                }
                checkCompletion.run();
            }
            @Override public void onFailure(Exception e) { Log.e(TAG, "Failed category load", e); checkCompletion.run(); }
        });

        firebaseDAO.getAllAccounts(currentUserId, new FirebaseDAO.OnAccountsRetrievedListener() {
            @Override
            public void onSuccess(List<Account> accounts) {
                accountList.clear();
                if (accounts != null) accountList.addAll(accounts);
                List<String> names = accountList.stream().map(Account::getName).collect(Collectors.toList());
                if (accountAdapter != null) {
                    accountAdapter.clear(); accountAdapter.addAll(names); accountAdapter.notifyDataSetChanged();
                }
                checkCompletion.run();
            }
            @Override public void onFailure(Exception e) { Log.e(TAG, "Failed account load", e); checkCompletion.run(); }
        });

        if (isEditMode && editingTransactionId != null) {
            firebaseDAO.getTransactionById(currentUserId, editingTransactionId, new FirebaseDAO.OnTransactionRetrievedListener() {
                @Override
                public void onSuccess(Transaction transaction) {
                    existingTransactionData = (Transaction) transaction; // Can be null if not found
                    if (existingTransactionData != null) {
                        existingTransactionData.setFirebaseId(editingTransactionId);
                    } else {
                        Log.e(TAG, "Transaction not found for ID: " + editingTransactionId);
                        Toast.makeText(AddTransactionActivity.this, "Transaction not found.", Toast.LENGTH_SHORT).show();
                    }
                    checkCompletion.run();
                }


                @Override public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to load transaction for edit", e);
                    Toast.makeText(AddTransactionActivity.this, "Error loading transaction details.", Toast.LENGTH_SHORT).show();
                    checkCompletion.run();
                }
            });
        }
    }

    private void populateFormForEdit() {
        if (existingTransactionData == null || binding == null) return;

        binding.editTextAmount.setText(String.format(Locale.US, "%.2f", existingTransactionData.getAmount()));
        binding.editTextDescription.setText(existingTransactionData.getDescription());
        binding.editTextNote.setText(existingTransactionData.getNote() != null ? existingTransactionData.getNote() : "");

        binding.radioIncome.setChecked("income".equalsIgnoreCase(existingTransactionData.getType()));
        binding.radioExpense.setChecked("expense".equalsIgnoreCase(existingTransactionData.getType()));

        selectSpinnerItemById(binding.spinnerCategory, categoryList, existingTransactionData.getCategoryId());
        selectSpinnerItemById(binding.spinnerAccount, accountList, existingTransactionData.getAccountId());

        Account loadedAcc = accountList.stream()
                .filter(a -> existingTransactionData.getAccountId() != null && a != null && existingTransactionData.getAccountId().equals(a.getFirebaseId()))
                .findFirst().orElse(accountList.isEmpty() ? null : accountList.get(0)); // Fallback to first account if available
        String currencyCode = (loadedAcc != null && loadedAcc.getCurrency() != null) ? loadedAcc.getCurrency() : "VND";
        if(binding.inputLayoutAmount != null) binding.inputLayoutAmount.setPrefixText(currencyCode + " ");

        if (existingTransactionData.getTimestamp() != null) {
            selectedDateCalendar.setTime(existingTransactionData.getTimestamp().toDate());
        } else {
            selectedDateCalendar = Calendar.getInstance();
        }
        updateDateLabel();
    }

    private <T extends FirebaseModelBase> void selectSpinnerItemById(Spinner spinner, List<T> itemList, String targetId) {
        ArrayAdapter<String> adapter = null;
        if (spinner == null || itemList == null || adapter == null)
            return; // Added adapter null check just in case
        adapter = (ArrayAdapter<String>) spinner.getAdapter();
        if (adapter == null) return;

        int selectionIndex = AdapterView.INVALID_POSITION; // Default to no selection

        if (targetId != null) {
            for (int i = 0; i < itemList.size(); i++) {
                T item = itemList.get(i);
                if (item != null && targetId.equals(item.getFirebaseId())) {
                    selectionIndex = i;
                    if (item instanceof Category) selectedCategory = (Category) item;
                    else if (item instanceof Account) selectedAccount = (Account) item;
                    break; // Found it
                }
            }
        }

        // If not found by ID, or targetId was null, try selecting the first item if list is not empty
        if (selectionIndex == AdapterView.INVALID_POSITION && !itemList.isEmpty() && itemList.get(0) != null) {
            selectionIndex = 0;
            if (itemList.get(0) instanceof Category) selectedCategory = (Category) itemList.get(0);
            else if (itemList.get(0) instanceof Account)
                selectedAccount = (Account) itemList.get(0);
        }

        // Set selection only if a valid index was found
        if (selectionIndex != AdapterView.INVALID_POSITION) {
            spinner.setSelection(selectionIndex, false); // Use false to prevent immediate listener trigger if possible
        } else {
            spinner.setSelection(AdapterView.INVALID_POSITION); // Explicitly set no selection
            // Reset selected object if nothing is selected in the spinner
            if (adapter == categoryAdapter) selectedCategory = null;
            else if (adapter == accountAdapter) selectedAccount = null;
        }
    }


    private void saveOrUpdateTransaction() {
        String amountStr = binding.editTextAmount.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();
        String note = binding.editTextNote.getText().toString().trim();
        int selectedTypeId = binding.radioGroupType.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedTypeId);
        final String transactionType = (selectedRadioButton != null && selectedRadioButton.getId() == R.id.radio_income) ? "income" : "expense";

        if (!validateInput(amountStr, description)) return;

        // Sửa lỗi ở đây - Parse giá trị amount từ chuỗi người dùng nhập vào
        final double amount = Double.parseDouble(amountStr);

        if (selectedAccount == null) return;
        if (selectedCategory == null) return;
        final String accountId = selectedAccount.getFirebaseId();
        final String categoryId = selectedCategory.getFirebaseId();
        final boolean isNowIncome = transactionType.equals("income");


        final Transaction transactionToSave;
        double oldAmount = 0;
        boolean wasIncome = false;
        String oldAccountId = null;

        if (isEditMode) {
            // ... (Lấy existingTransactionData như cũ) ...
            oldAmount = existingTransactionData.getAmount();
            wasIncome = "income".equalsIgnoreCase(existingTransactionData.getType());
            oldAccountId = existingTransactionData.getAccountId();
            transactionToSave = existingTransactionData; // Update object cũ
        } else {
            transactionToSave = new Transaction(); // Tạo object mới
        }

        // --- Cập nhật transactionToSave bằng dữ liệu form (như cũ) ---
        transactionToSave.setType(transactionType);
        transactionToSave.setAmount(amount);
        // ... các trường khác ...
        transactionToSave.setAccountId(accountId); // Đảm bảo accountId mới được gán
        transactionToSave.setCategoryId(categoryId);
        // ...

        if (isEditMode) {
            // --- UPDATE ---
            // Lưu giá trị cũ và mới vào biến final để dùng trong callback
            final String finalOldAccountId = oldAccountId;
            final double finalOldAmount = oldAmount;
            final boolean finalWasIncome = wasIncome;
            final String finalNewAccountId = transactionToSave.getAccountId();
            final double finalNewAmount = transactionToSave.getAmount();
            final boolean finalIsNowIncome = isNowIncome; // Dùng biến đã xác định ở trên

            firebaseDAO.updateTransaction(currentUserId, editingTransactionId, transactionToSave, new FirebaseDAO.OnTransactionUpdatedListener() {
                @Override public void onSuccess() {
                    Log.d(TAG, "DB: Transaction Updated. Now updating balance(s)...");
                    // Gọi hàm cập nhật số dư cho Edit
                    updateBalanceAfterEdit(finalOldAccountId, finalNewAccountId, finalOldAmount, finalWasIncome, finalNewAmount, finalIsNowIncome);
                }
                @Override public void onFailure(Exception e) { handleSaveFailure("Error updating transaction", e); }
            });
        } else {
            // --- ADD ---
            final String finalAccountId = transactionToSave.getAccountId();
            final double finalAmount = transactionToSave.getAmount();
            final boolean finalIsIncome = isNowIncome; // Dùng biến đã xác định

            firebaseDAO.addTransaction(currentUserId, transactionToSave, new FirebaseDAO.OnTransactionAddedListener() {
                @Override public void onSuccess(String id) {
                    Log.d(TAG, "DB: Transaction Added. Now updating balance...");
                    // Gọi hàm cập nhật số dư cho Add (isAddition = true)
                    updateBalanceAfterAddOrDelete(finalAccountId, finalAmount, finalIsIncome, true);
                }
                @Override public void onFailure(Exception e) { handleSaveFailure("Error saving transaction", e); }
            });
        }
    }

    /**
     * Cập nhật số dư tài khoản sau khi THÊM hoặc XÓA giao dịch.
     * Sửa lại logic amountChange.
     */
    private void updateBalanceAfterAddOrDelete(String accountId, double transactionAmount, boolean wasIncome, boolean isAddition) { // Đổi tên isIncome thành wasIncome cho rõ nghĩa (trạng thái của giao dịch đang xử lý)
        if (accountId == null) { handleSaveFailure("Balance error: Account ID null", null); return; }

        firebaseDAO.getAccountById(currentUserId, accountId, new FirebaseDAO.OnAccountRetrievedListener() {
            @Override
            public void onSuccess(Account currentAccountData) {
                if (currentAccountData == null) { handleSaveFailure("Balance error: Account not found", null); return; }

                double currentBalance = currentAccountData.getCurrentBalance();
                double amountChange;

                // --- LOGIC TÍNH TOÁN CHÍNH XÁC ---
                if (isAddition) { // Đang THÊM transaction
                    amountChange = wasIncome ? transactionAmount : -transactionAmount; // Income thì cộng, Expense thì trừ
                    Log.d(TAG, "Calculating balance change for ADDING: " + (wasIncome ? "Income +" : "Expense -") + transactionAmount);
                } else { // Đang XÓA transaction (được gọi từ AllTransactionsActivity)
                    amountChange = wasIncome ? -transactionAmount : transactionAmount; // Income thì trừ (hoàn tác cộng), Expense thì cộng (hoàn tác trừ)
                    Log.d(TAG, "Calculating balance change for DELETING: " + (wasIncome ? "Income -" : "Expense +") + transactionAmount);
                }
                // --- KẾT THÚC LOGIC TÍNH TOÁN ---

                double newBalance = currentBalance + amountChange;
                Log.i(TAG, "Updating balance (Add/Delete="+isAddition+") acc " + accountId + ": " + currentBalance + " -> " + newBalance);

                firebaseDAO.updateAccountBalance(currentUserId, accountId, newBalance, new FirebaseDAO.OnAccountUpdatedListener() {
                    @Override public void onSuccess() {
                        Log.d(TAG, "Account balance updated successfully after " + (isAddition ? "add/edit" : "delete") + ".");
                        // Chỉ gọi handleSaveSuccess nếu là thao tác từ Activity này (Add/Edit)
                        if (isAddition || isEditMode) { // isAddition sẽ true khi add, isEditMode sẽ true khi edit
                            handleSaveSuccess(isEditMode ? "Transaction Updated!" : "Transaction Saved!");
                        }
                        // Nếu isAddition=false, nghĩa là hàm này được gọi từ luồng Xóa, không cần làm gì thêm ở đây.
                    }
                    @Override public void onFailure(Exception e) {
                        Log.e(TAG, "Balance update failed after " + (isAddition ? "add/edit" : "delete"), e);
                        if (isAddition || isEditMode) {
                            // Nếu lỗi khi đang thêm/sửa, vẫn nên báo giao dịch đã xử lý nhưng số dư lỗi
                            Toast.makeText(AddTransactionActivity.this, "Tx saved/updated, but balance update failed.", Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK); // Vẫn nên reload list
                            finish();
                        } else {
                            // Nếu lỗi khi đang xóa (gọi từ Activity khác), chỉ cần log/toast ở Activity đó
                            // Không cần finish() ở đây.
                        }
                    }
                });
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG,"Balance error: Fetching account failed for updateBalanceAfterAddOrDelete", e);
                if (isAddition || isEditMode) {
                    handleSaveFailure("Balance error: Fetching account failed", e);
                } else {
                    Toast.makeText(AddTransactionActivity.this, "Could not update balance: Failed fetching account.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void updateBalanceAfterEdit(String oldAccountId, String newAccountId, double oldAmount, boolean wasIncome, double newAmount, boolean isNowIncome) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) firestoreTransaction -> {
            DocumentReference oldAccountRef = null; Account oldAccountData = null; double balanceAfterRevert = 0;
            DocumentReference newAccountRef = null; Account newAccountData = null; double finalNewBalance = 0; // Khởi tạo

            // --- Bước 1: Đọc tài khoản CŨ và tính số dư sau khi HOÀN TÁC ---
            if (oldAccountId != null) {
                oldAccountRef = firebaseDAO.getUserSubCollectionRef(currentUserId, FirebaseDAO.ACCOUNTS_COLLECTION).document(oldAccountId);
                DocumentSnapshot oldAccountSnap = firestoreTransaction.get(oldAccountRef);
                if (!oldAccountSnap.exists()) throw new FirebaseFirestoreException("Old account " + oldAccountId + " not found!", FirebaseFirestoreException.Code.ABORTED);
                oldAccountData = oldAccountSnap.toObject(Account.class);
                if(oldAccountData == null) throw new FirebaseFirestoreException("Parse old account failed", FirebaseFirestoreException.Code.ABORTED);
                double balanceBeforeOld = oldAccountData.getCurrentBalance();
                // Hoàn tác: Nếu TRƯỚC ĐÓ là Income thì TRỪ đi, nếu là Expense thì CỘNG lại
                double oldAmountChange = wasIncome ? -oldAmount : oldAmount;
                balanceAfterRevert = balanceBeforeOld + oldAmountChange;
                Log.d(TAG, "Edit Balance Step 1: Reverting old account " + oldAccountId + ": " + balanceBeforeOld + " + (" + oldAmountChange + ") = " + balanceAfterRevert);
            }

            // --- Bước 2: Đọc tài khoản MỚI và tính số dư cuối cùng ---
            if (newAccountId != null) {
                newAccountRef = firebaseDAO.getUserSubCollectionRef(currentUserId, FirebaseDAO.ACCOUNTS_COLLECTION).document(newAccountId);
                // Nếu sửa trên cùng một tài khoản
                if(oldAccountId != null && oldAccountId.equals(newAccountId)) {
                    double balanceBeforeNewApply = balanceAfterRevert; // Bắt đầu từ số dư đã hoàn tác
                    // Áp dụng mới: Nếu BÂY GIỜ là Income thì CỘNG, là Expense thì TRỪ
                    double newAmountChange = isNowIncome ? newAmount : -newAmount;
                    finalNewBalance = balanceBeforeNewApply + newAmountChange;
                    Log.d(TAG, "Edit Balance Step 2 (Same Acc): Applying new amount: " + balanceBeforeNewApply + " + (" + newAmountChange + ") = " + finalNewBalance);
                    // Chỉ cần cập nhật 1 lần cho tài khoản này
                    firestoreTransaction.update(newAccountRef, "currentBalance", finalNewBalance);

                } else { // Nếu sửa và chuyển sang tài khoản khác
                    DocumentSnapshot newAccountSnap = firestoreTransaction.get(newAccountRef);
                    if (!newAccountSnap.exists()) throw new FirebaseFirestoreException("New account " + newAccountId + " not found!", FirebaseFirestoreException.Code.ABORTED);
                    newAccountData = newAccountSnap.toObject(Account.class);
                    if(newAccountData == null) throw new FirebaseFirestoreException("Parse new acc failed", FirebaseFirestoreException.Code.ABORTED);

                    // Áp dụng thay đổi mới vào tài khoản mới
                    double balanceBeforeNewApply = newAccountData.getCurrentBalance();
                    // Áp dụng mới: Nếu BÂY GIỜ là Income thì CỘNG, là Expense thì TRỪ
                    double newAmountChange = isNowIncome ? newAmount : -newAmount;
                    finalNewBalance = balanceBeforeNewApply + newAmountChange;
                    Log.d(TAG, "Edit Balance Step 2 (Diff Acc): Applying new amount: " + newAccountId + ": " + balanceBeforeNewApply + " + (" + newAmountChange + ") = " + finalNewBalance);
                    firestoreTransaction.update(newAccountRef, "currentBalance", finalNewBalance); // Cập nhật tài khoản mới

                    // --- Bước 3: Cập nhật tài khoản CŨ (nếu nó khác tài khoản mới và đã tồn tại) ---
                    if (oldAccountRef != null) { // Đã đọc ở bước 1
                        Log.d(TAG, "Edit Balance Step 3 (Diff Acc): Updating old account " + oldAccountId + " balance to reverted value: " + balanceAfterRevert);
                        firestoreTransaction.update(oldAccountRef, "currentBalance", balanceAfterRevert); // Cập nhật số dư đã hoàn tác
                    }
                }
            } else {
                throw new FirebaseFirestoreException("New account ID is null during update", FirebaseFirestoreException.Code.ABORTED);
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Account balance(s) updated successfully via Firestore transaction after edit.");
            handleSaveSuccess("Transaction Updated!");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore transaction for balance update failed after edit", e);
            Toast.makeText(AddTransactionActivity.this, "Tx updated, but balance update failed: "+e.getMessage(), Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        });
    }




    private boolean validateInput(String amountStr, String description) {
        boolean isValid = true;
        if (TextUtils.isEmpty(amountStr)) { binding.inputLayoutAmount.setError("Required"); isValid = false;}
        else { try { double a=Double.parseDouble(amountStr); if(a<=0){binding.inputLayoutAmount.setError("Positive"); isValid=false;} else {binding.inputLayoutAmount.setError(null);}}catch(NumberFormatException e){binding.inputLayoutAmount.setError("Invalid"); isValid=false;}}
        if (TextUtils.isEmpty(description)) { binding.inputLayoutDescription.setError("Required"); isValid = false;} else {binding.inputLayoutDescription.setError(null);}
        if (selectedCategory == null && !categoryList.isEmpty()) { Toast.makeText(this,"Select category", Toast.LENGTH_SHORT).show(); isValid = false; }
        if (selectedAccount == null && !accountList.isEmpty()) { Toast.makeText(this,"Select account", Toast.LENGTH_SHORT).show(); isValid = false; }
        return isValid;
    }

    private void showLoading(boolean isLoading) {
        if(binding != null && binding.progressBarAddTransaction != null && binding.btnSaveTransaction != null){
            binding.progressBarAddTransaction.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSaveTransaction.setEnabled(!isLoading);
            binding.editTextAmount.setEnabled(!isLoading);
            binding.editTextDescription.setEnabled(!isLoading);
            binding.editTextNote.setEnabled(!isLoading);
            binding.spinnerAccount.setEnabled(!isLoading);
            binding.spinnerCategory.setEnabled(!isLoading);
            binding.textViewDate.setEnabled(!isLoading);
            if(binding.radioGroupType != null) { for (int i=0; i < binding.radioGroupType.getChildCount(); i++){ binding.radioGroupType.getChildAt(i).setEnabled(!isLoading);}}
        } else { Log.w(TAG, "Binding or views null in showLoading"); }
    }

    private void handleSaveSuccess(String message) {
        showLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void handleSaveFailure(String logMessage, Exception e) {
        Log.e(TAG, logMessage, e);
        showLoading(false);
        Toast.makeText(this, logMessage + (e != null ? ": " + e.getMessage() : ""), Toast.LENGTH_LONG).show();
    }

    // Keep DAO listener interfaces for clarity, implementation is in FirebaseDAO
    // public interface OnAccountsRetrievedListener { void onSuccess(List<Account> a); void onFailure(Exception e); } // Defined in DAO
    // public interface OnTransactionRetrievedListener { void onSuccess(Transaction t); void onFailure(Exception e); } // Defined in DAO
}