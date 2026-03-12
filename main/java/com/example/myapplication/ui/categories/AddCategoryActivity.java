package com.example.myapplication.ui.categories;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent; // Import Intent
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.data.datasource.FirebaseDAO;
import com.example.myapplication.data.model.Category;
import com.google.android.material.chip.Chip; // Import Chip
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.databinding.ActivityAddCategoryBinding;

public class AddCategoryActivity extends AppCompatActivity {

    private static final String TAG = "AddCategoryActivity";

    private ActivityAddCategoryBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;

    private String selectedCategoryType = "expense";
    private String selectedIconName = "ic_placeholder_category";
    private String selectedColorHex = "#CCCCCC";

    // Variables for Edit mode
    private boolean isEditMode = false;
    private String editingCategoryId = null;
    private Category existingCategoryData = null; // To hold the category being edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddCategoryBinding.inflate(getLayoutInflater());
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

        handleIntent(); // Check if it's Edit mode and get data
        setupToolbar();
        setupListeners();

        // Set initial UI state (color/icon) or load existing data if in edit mode
        if (!isEditMode) {
            updateColorView(selectedColorHex);
            updateIconView(selectedIconName);
        } else if (editingCategoryId != null) {
            // Load existing category data if ID is present
            loadCategoryForEdit();
        } else {
            // Handle error: Edit mode without ID
            Toast.makeText(this, "Error: Category ID missing for edit.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EDIT_MODE") && intent.getBooleanExtra("EDIT_MODE", false)) {
            isEditMode = true;
            editingCategoryId = intent.getStringExtra("CATEGORY_ID");
            Log.d(TAG, "Edit Mode enabled for Category ID: " + editingCategoryId);
        } else {
            isEditMode = false;
            Log.d(TAG, "Add Mode enabled.");
        }
    }


    private void setupToolbar() {
        setSupportActionBar(binding.toolbarAddCategory);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Edit Category" : "Add New Category");
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


    private void setupListeners() {
        binding.chipGroupCategoryType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                selectedCategoryType = (checkedId == R.id.chip_income) ? "income" : "expense";
                Log.d(TAG, "Selected Category Type: " + selectedCategoryType);
            } else {
                selectedCategoryType = "expense";
            }
        });

        binding.btnSelectIcon.setOnClickListener(v -> {
            Toast.makeText(this, "Implement Icon Selection", Toast.LENGTH_SHORT).show();
            selectedIconName = "ic_food_drink";
            updateIconView(selectedIconName);
        });

        binding.btnSelectColor.setOnClickListener(v -> {
            openColorPickerDialog();
        });

        binding.btnSaveCategory.setOnClickListener(v -> saveCategory());
    }

    private void loadCategoryForEdit() {
        if (editingCategoryId == null || currentUserId == null) return;
        showLoading(true);
        firebaseDAO.getCategoryById(currentUserId, editingCategoryId, new FirebaseDAO.OnCategoryRetrievedListener() {
            @Override
            public void onSuccess(Category category) {
                showLoading(false);
                if (category != null) {
                    existingCategoryData = category;
                    populateFormForEdit();
                } else {
                    Toast.makeText(AddCategoryActivity.this, "Failed to load category data.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Category not found for ID: " + editingCategoryId);
                    finish(); // Close if category not found
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Log.e(TAG, "Error loading category for edit", e);
                Toast.makeText(AddCategoryActivity.this, "Error loading category: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish(); // Close on error
            }
        });
    }


    private void populateFormForEdit() {
        if (existingCategoryData == null) return;

        binding.editTextCategoryName.setText(existingCategoryData.getName());

        if ("income".equalsIgnoreCase(existingCategoryData.getType())) {
            binding.chipIncome.setChecked(true);
            selectedCategoryType = "income";
        } else {
            binding.chipExpense.setChecked(true);
            selectedCategoryType = "expense";
        }

        updateIconView(existingCategoryData.getIcon() != null ? existingCategoryData.getIcon() : "ic_placeholder_category");
        updateColorView(existingCategoryData.getColor() != null ? existingCategoryData.getColor() : "#CCCCCC");
    }



    private void openColorPickerDialog() {
        Toast.makeText(this, "Implement Color Picker Dialog", Toast.LENGTH_SHORT).show();
        selectedColorHex = "#FF5722";
        updateColorView(selectedColorHex);
    }

    private void updateIconView(String iconName) {
        selectedIconName = iconName;
        if (binding == null || binding.imageViewSelectedIcon == null) return;
        try {
            int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            binding.imageViewSelectedIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_placeholder_category);
        } catch (Exception e) {
            Log.e(TAG, "Error setting icon resource: " + iconName, e);
            binding.imageViewSelectedIcon.setImageResource(R.drawable.ic_placeholder_category);
        }
    }

    private void updateColorView(String colorHex) {
        selectedColorHex = colorHex;
        if (binding == null || binding.viewSelectedColor == null) return;
        try {
            binding.viewSelectedColor.setBackgroundColor(Color.parseColor(colorHex));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid color hex: " + colorHex, e);
            binding.viewSelectedColor.setBackgroundColor(Color.GRAY);
        }
    }


    private void saveCategory() {
        String categoryName = binding.editTextCategoryName.getText().toString().trim();

        if (TextUtils.isEmpty(categoryName)) {
            binding.inputLayoutCategoryName.setError("Category Name is required.");
            return;
        } else {
            binding.inputLayoutCategoryName.setError(null);
        }

        showLoading(true);

        Category categoryToSave;
        if (isEditMode) {
            if (existingCategoryData == null || editingCategoryId == null || editingCategoryId.isEmpty()) {
                Log.e(TAG, "Error: Cannot save in Edit mode. Missing existing data or ID.");
                Toast.makeText(this, "Error saving changes. Please reload.", Toast.LENGTH_SHORT).show();
                showLoading(false);
                return;
            }
            categoryToSave = existingCategoryData;
            Log.d(TAG, "saveCategory: Preparing to UPDATE category with ID: " + editingCategoryId);
        } else {
            categoryToSave = new Category();
            categoryToSave.setCustom(true);
            Log.d(TAG, "saveCategory: Preparing to ADD new category.");
        }

        categoryToSave.setName(categoryName);
        categoryToSave.setType(selectedCategoryType);
        categoryToSave.setIcon(selectedIconName);
        categoryToSave.setColor(selectedColorHex);
        // categoryToSave.setParentCategoryId(null); // Add logic if needed

        if (isEditMode) {
            firebaseDAO.updateCategory(currentUserId, editingCategoryId, categoryToSave, new FirebaseDAO.OnCategoryUpdatedListener() {
                @Override
                public void onSuccess() {
                    handleSaveSuccess("Category Updated!");
                }
                @Override
                public void onFailure(Exception e) {
                    handleSaveFailure("Error updating category", e);
                }
            });
        } else {
            firebaseDAO.addCategory(currentUserId, categoryToSave, new FirebaseDAO.OnCategoryAddedListener() {
                @Override
                public void onSuccess(String categoryId) {
                    handleSaveSuccess("Category Saved!");
                }
                @Override
                public void onFailure(Exception e) {
                    handleSaveFailure("Error saving category", e);
                }
            });
        }
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
        Toast.makeText(this, logMessage + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void showLoading(boolean isLoading) {
        if (binding != null && binding.progressBarAddCategory != null && binding.btnSaveCategory != null) {
            binding.progressBarAddCategory.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSaveCategory.setEnabled(!isLoading);
            binding.editTextCategoryName.setEnabled(!isLoading);
            if (binding.chipGroupCategoryType != null) {
                for (int i = 0; i < binding.chipGroupCategoryType.getChildCount(); i++) {
                    Chip chip = (Chip) binding.chipGroupCategoryType.getChildAt(i);
                    chip.setEnabled(!isLoading);
                }
            }
            if(binding.btnSelectIcon != null) binding.btnSelectIcon.setEnabled(!isLoading);
            if(binding.btnSelectColor != null) binding.btnSelectColor.setEnabled(!isLoading);
        } else {
            Log.w(TAG, "Binding or views are null in showLoading");
        }
    }
}