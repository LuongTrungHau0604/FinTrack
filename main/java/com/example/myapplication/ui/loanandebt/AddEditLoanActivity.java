package com.example.myapplication.ui.loanandebt; // Ensure correct package

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.datasource.FirebaseDAO; // Import DAO
import com.example.myapplication.data.model.Loan; // Import Loan model
import com.example.myapplication.databinding.ActivityAddEditLoanBinding; // Import ViewBinding
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditLoanActivity extends AppCompatActivity {

    private static final String TAG = "AddEditLoanActivity";

    private ActivityAddEditLoanBinding binding; // Use ViewBinding
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;

    private boolean isEditMode = false;
    private String editingLoanId = null;
    private Loan existingLoanData = null;

    private String selectedCurrency = "VND"; // Default currency
    private Calendar selectedStartDateCalendar = Calendar.getInstance();
    private Calendar selectedDueDateCalendar = Calendar.getInstance(); // Use Calendar for dates

    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US); // For saving date strings if needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Keep if needed
        binding = ActivityAddEditLoanBinding.inflate(getLayoutInflater());
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
        setupToolbar();
        setupSpinners();
        setupDatePickers();
        setupListeners();

        if (!isEditMode) {
            updateDateLabel(binding.textViewLoanStartDate, selectedStartDateCalendar); // Set initial start date
            updateDateLabel(binding.textViewLoanDueDate, selectedDueDateCalendar);     // Set initial due date
            binding.editTextLoanCurrentBalance.setText("0.00"); // Initial balance is 0 for new loans
            binding.editTextLoanCurrentBalance.setEnabled(false); // Don't allow editing current balance for new loans
            binding.inputLayoutLoanInitialAmount.setEnabled(true); // Allow editing initial amount for new loans
        } else if (editingLoanId != null) {
            loadLoanForEdit();
        } else {
            Toast.makeText(this, "Error: Loan ID missing for edit.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Apply window insets if using EdgeToEdge
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> { // Replace R.id.main with your root layout ID if needed
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        */
    }

    private void handleIntent() {
        Intent intent = getIntent();
        isEditMode = intent != null && intent.hasExtra("EDIT_MODE") && intent.getBooleanExtra("EDIT_MODE", false);
        if (isEditMode) {
            editingLoanId = intent.getStringExtra("LOAN_ID");
            Log.d(TAG, "Edit Mode enabled for Loan ID: " + editingLoanId);
        } else {
            Log.d(TAG, "Add Mode enabled.");
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarAddEditLoan);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Edit Loan/Debt" : "Add Loan/Debt");
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
        // Currency Spinner
        ArrayAdapter<CharSequence> currencyAdapter = ArrayAdapter.createFromResource(this,
                R.array.currencies_array, android.R.layout.simple_spinner_item);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLoanCurrency.setAdapter(currencyAdapter);
        setSpinnerToValue(binding.spinnerLoanCurrency, currencyAdapter, selectedCurrency); // Set default

        binding.spinnerLoanCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCurrency = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Selected Currency: " + selectedCurrency);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedCurrency = "VND"; }
        });
    }

    private void setSpinnerToValue(Spinner spinner, ArrayAdapter<CharSequence> adapter, String value) {
        if (value == null || adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
        if(adapter.getCount() > 0) spinner.setSelection(0); // Fallback to first
    }

    private void setupDatePickers() {
        binding.textViewLoanStartDate.setOnClickListener(v -> openDatePickerDialog(selectedStartDateCalendar, binding.textViewLoanStartDate));
        binding.textViewLoanDueDate.setOnClickListener(v -> openDatePickerDialog(selectedDueDateCalendar, binding.textViewLoanDueDate));
    }

    private void openDatePickerDialog(Calendar calendar, TextView targetTextView) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel(targetTextView, calendar);
        };
        new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel(TextView textView, Calendar calendar) {
        if(textView != null && calendar != null){
            textView.setText(displayDateFormat.format(calendar.getTime()));
        }
    }


    private void setupListeners() {
        binding.btnSaveLoan.setOnClickListener(v -> saveOrUpdateLoan());
    }

    private void loadLoanForEdit() {
        showLoading(true);
        binding.btnSaveLoan.setEnabled(false);
        // !!! ADD getLoanById to FirebaseDAO !!!
        firebaseDAO.getLoanById(currentUserId, editingLoanId, new FirebaseDAO.OnLoanRetrievedListener() { // Assume this exists
            @Override
            public void onSuccess(Loan loan) {
                showLoading(false);
                binding.btnSaveLoan.setEnabled(true);
                if (loan != null) {
                    existingLoanData = loan;
                    existingLoanData.setFirebaseId(editingLoanId);
                    populateFormForEdit();
                } else {
                    Toast.makeText(AddEditLoanActivity.this, "Loan not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                binding.btnSaveLoan.setEnabled(true);
                Log.e(TAG, "Error loading loan for edit", e);
                Toast.makeText(AddEditLoanActivity.this, "Error loading loan data.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


    private void populateFormForEdit() {
        if (existingLoanData == null || binding == null) return;

        binding.editTextLoanName.setText(existingLoanData.getName());
        binding.editTextEntityName.setText(existingLoanData.getEntityName());
        binding.editTextLoanInitialAmount.setText(String.format(Locale.US, "%.2f", existingLoanData.getInitialAmount()));
        binding.editTextLoanInitialAmount.setEnabled(false); // Don't allow editing initial amount
        binding.editTextLoanCurrentBalance.setText(String.format(Locale.US, "%.2f", existingLoanData.getCurrentBalance()));
        binding.editTextLoanCurrentBalance.setEnabled(true); // Allow editing current balance if needed for corrections
        binding.editTextInterestRate.setText(String.format(Locale.US, "%.2f", existingLoanData.getInterestRate()));
        binding.editTextPaymentDay.setText(String.valueOf(existingLoanData.getPaymentDayOfMonth()));
        binding.editTextLoanNotes.setText(existingLoanData.getNotes());

        // Set currency spinner
        ArrayAdapter<CharSequence> currencyAdapter = (ArrayAdapter<CharSequence>) binding.spinnerLoanCurrency.getAdapter();
        setSpinnerToValue(binding.spinnerLoanCurrency, currencyAdapter, existingLoanData.getCurrency());

        // Set start date
        if (existingLoanData.getStartDate() != null) {
            selectedStartDateCalendar.setTime(existingLoanData.getStartDate().toDate());
        } else {
            selectedStartDateCalendar = Calendar.getInstance(); // Fallback
        }
        updateDateLabel(binding.textViewLoanStartDate, selectedStartDateCalendar);

        // Set due date
        if (existingLoanData.getDueDate() != null) {
            try {
                // Assuming due date is stored as YYYY-MM-DD string
                Date dueDate = firebaseDateFormat.parse(existingLoanData.getDueDate());
                if (dueDate != null) selectedDueDateCalendar.setTime(dueDate);
            } catch (ParseException e) {
                Log.e(TAG,"Error parsing due date string: " + existingLoanData.getDueDate());
                selectedDueDateCalendar = Calendar.getInstance(); // Fallback
            }
        } else {
            selectedDueDateCalendar = Calendar.getInstance(); // Fallback
        }
        updateDateLabel(binding.textViewLoanDueDate, selectedDueDateCalendar);
    }


    private void saveOrUpdateLoan() {
        String loanName = binding.editTextLoanName.getText().toString().trim();
        String entityName = binding.editTextEntityName.getText().toString().trim();
        String initialAmountStr = binding.editTextLoanInitialAmount.getText().toString().trim();
        String currentBalanceStr = binding.editTextLoanCurrentBalance.getText().toString().trim(); // Get current balance if editable
        String interestRateStr = binding.editTextInterestRate.getText().toString().trim();
        String paymentDayStr = binding.editTextPaymentDay.getText().toString().trim();
        String notes = binding.editTextLoanNotes.getText().toString().trim();
        String startDateStr = firebaseDateFormat.format(selectedStartDateCalendar.getTime()); // Use format for consistency if needed
        String dueDateStr = firebaseDateFormat.format(selectedDueDateCalendar.getTime());

        if (!validateInput(loanName, entityName, initialAmountStr, interestRateStr, paymentDayStr)) return;

        double initialAmount = 0.0;
        double currentBalance = 0.0;
        double interestRate = 0.0;
        int paymentDay = 0;

        try {
            initialAmount = Double.parseDouble(initialAmountStr);
            interestRate = Double.parseDouble(interestRateStr);
            // Parse current balance only if it was editable, otherwise use initial for new loan
            if(isEditMode) {
                currentBalance = Double.parseDouble(currentBalanceStr);
            } else {
                currentBalance = initialAmount; // For new loan, current balance starts as initial
            }

            if (!paymentDayStr.isEmpty()) {
                paymentDay = Integer.parseInt(paymentDayStr);
                if (paymentDay < 1 || paymentDay > 31) {
                    binding.inputLayoutPaymentDay.setError("Day must be 1-31");
                    return;
                } else {
                    binding.inputLayoutPaymentDay.setError(null);
                }
            } else {
                binding.inputLayoutPaymentDay.setError(null);
            }
            if (initialAmount < 0) { binding.inputLayoutLoanInitialAmount.setError("Cannot be negative"); return;} else {binding.inputLayoutLoanInitialAmount.setError(null);}
            if (interestRate < 0) { binding.inputLayoutInterestRate.setError("Cannot be negative"); return;} else {binding.inputLayoutInterestRate.setError(null);}

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format.", Toast.LENGTH_SHORT).show();
            // Set error on specific fields if possible
            if (!isValidDouble(initialAmountStr)) binding.inputLayoutLoanInitialAmount.setError("Invalid Number");
            if (!isValidDouble(interestRateStr)) binding.inputLayoutInterestRate.setError("Invalid Number");
            if (isEditMode && !isValidDouble(currentBalanceStr)) binding.inputLayoutLoanCurrentBalance.setError("Invalid Number");
            if (!paymentDayStr.isEmpty() && !isValidInteger(paymentDayStr)) binding.inputLayoutPaymentDay.setError("Invalid Number");
            return;
        }

        showLoading(true);

        Loan loanToSave;
        if (isEditMode) {
            if (existingLoanData == null || editingLoanId == null) { handleSaveFailure("Update error", null); return; }
            loanToSave = existingLoanData;
        } else {
            loanToSave = new Loan();
            loanToSave.setFirebaseId(null); // Ensure null ID for add
        }

        loanToSave.setName(loanName);
        loanToSave.setEntityName(entityName);
        loanToSave.setInitialAmount(initialAmount);
        loanToSave.setCurrentBalance(currentBalance); // Save the (potentially edited) current balance
        loanToSave.setInterestRate(interestRate);
        // Save dates as Timestamps and Strings
        loanToSave.setStartDate(new Timestamp(selectedStartDateCalendar.getTime()));
        loanToSave.setDueDate(dueDateStr); // Keep dueDate as String for consistency with original model? Or change model to Timestamp too?
        loanToSave.setPaymentDayOfMonth(paymentDay);
        loanToSave.setNotes(notes.isEmpty() ? null : notes);
        // Assuming Loan model doesn't store currency directly, or add setCurrency if it does
        // loanToSave.setCurrency(selectedCurrency);


        if (isEditMode) {
            // !!! ADD updateLoan to FirebaseDAO !!!
            firebaseDAO.updateLoan(currentUserId, editingLoanId, loanToSave, new FirebaseDAO.OnLoanUpdatedListener() {
                @Override public void onSuccess() { handleSaveSuccess("Loan Updated!"); }
                @Override public void onFailure(Exception e) { handleSaveFailure("Error updating loan", e); }
            });
        } else {
            firebaseDAO.addLoan(currentUserId, loanToSave, new FirebaseDAO.OnLoanAddedListener() {
                @Override public void onSuccess(String id) { handleSaveSuccess("Loan Saved!"); }
                @Override public void onFailure(Exception e) { handleSaveFailure("Error saving loan", e); }
            });
        }
    }

    private boolean validateInput(String loanName, String entityName, String initialAmountStr, String interestRateStr, String paymentDayStr) {
        boolean isValid = true;
        if (TextUtils.isEmpty(loanName)) { binding.inputLayoutLoanName.setError("Required"); isValid = false; } else { binding.inputLayoutLoanName.setError(null); }
        if (TextUtils.isEmpty(entityName)) { binding.inputLayoutEntityName.setError("Required"); isValid = false; } else { binding.inputLayoutEntityName.setError(null); }
        if (TextUtils.isEmpty(initialAmountStr) && !isEditMode) { binding.inputLayoutLoanInitialAmount.setError("Required for new loan"); isValid = false; } else { binding.inputLayoutLoanInitialAmount.setError(null);} // Initial amount required for new loan only
        if (TextUtils.isEmpty(interestRateStr)) { binding.inputLayoutInterestRate.setError("Required (can be 0)"); isValid = false; } else { binding.inputLayoutInterestRate.setError(null);} // Allow 0 interest

        // Validate payment day format if entered
        if (!paymentDayStr.isEmpty()) {
            try {
                int day = Integer.parseInt(paymentDayStr);
                if (day < 1 || day > 31) {
                    binding.inputLayoutPaymentDay.setError("Day must be 1-31");
                    isValid = false;
                } else {
                    binding.inputLayoutPaymentDay.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.inputLayoutPaymentDay.setError("Invalid day");
                isValid = false;
            }
        } else {
            binding.inputLayoutPaymentDay.setError(null); // Optional field
        }


        // Basic number format check (refined in saveOrUpdateLoan)
        if (!TextUtils.isEmpty(initialAmountStr) && !isValidDouble(initialAmountStr)) { binding.inputLayoutLoanInitialAmount.setError("Invalid Number"); isValid = false; }
        if (!TextUtils.isEmpty(interestRateStr) && !isValidDouble(interestRateStr)) { binding.inputLayoutInterestRate.setError("Invalid Number"); isValid = false; }
        if (isEditMode && !TextUtils.isEmpty(binding.editTextLoanCurrentBalance.getText()) && !isValidDouble(binding.editTextLoanCurrentBalance.getText().toString())) { binding.inputLayoutLoanCurrentBalance.setError("Invalid Number"); isValid = false;} else if (isEditMode) {binding.inputLayoutLoanCurrentBalance.setError(null);}


        return isValid;
    }

    // Helper validation methods
    private boolean isValidDouble(String s) { try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; } }
    private boolean isValidInteger(String s) { try { Integer.parseInt(s); return true; } catch (NumberFormatException e) { return false; } }


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

    private void showLoading(boolean isLoading) {
        if (binding != null && binding.progressBarAddEditLoan != null && binding.btnSaveLoan != null) {
            binding.progressBarAddEditLoan.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSaveLoan.setEnabled(!isLoading);
            // Disable all input fields during loading
            binding.editTextLoanName.setEnabled(!isLoading);
            binding.editTextEntityName.setEnabled(!isLoading);
            // Only disable initial amount if in add mode (it's already disabled in edit mode)
            if(!isEditMode) binding.editTextLoanInitialAmount.setEnabled(!isLoading);
            binding.editTextLoanCurrentBalance.setEnabled(isEditMode && !isLoading); // Only enable current balance in edit mode when not loading
            binding.editTextInterestRate.setEnabled(!isLoading);
            binding.spinnerLoanCurrency.setEnabled(!isLoading);
            binding.textViewLoanStartDate.setEnabled(!isLoading);
            binding.textViewLoanDueDate.setEnabled(!isLoading);
            binding.editTextPaymentDay.setEnabled(!isLoading);
            binding.editTextLoanNotes.setEnabled(!isLoading);
        }
    }

    // --- DAO Interfaces (Ensure these exist in FirebaseDAO) ---
    // public interface OnLoanAddedListener { void onSuccess(String id); void onFailure(Exception e); }
    // public interface OnLoanRetrievedListener { void onSuccess(Loan loan); void onFailure(Exception e); }
    // public interface OnLoanUpdatedListener { void onSuccess(); void onFailure(Exception e); }

}