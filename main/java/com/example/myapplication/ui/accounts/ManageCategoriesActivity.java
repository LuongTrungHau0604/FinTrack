package com.example.myapplication.ui.accounts;

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

import com.example.myapplication.data.datasource.FirebaseDAO;
import com.example.myapplication.data.model.Category;
import com.example.myapplication.ui.categories.AddCategoryActivity;
import com.example.myapplication.ui.categories.CategoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.myapplication.databinding.ActivityManageCategoriesBinding;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesActivity extends AppCompatActivity
        implements CategoryAdapter.OnCategoryInteractionListener {

    private static final String TAG = "ManageCategoriesAct"; // Tag riêng cho Activity này

    private ActivityManageCategoriesBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDAO firebaseDAO;
    private String currentUserId;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addCategoryLauncher;
    private ActivityResultLauncher<Intent> editCategoryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageCategoriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate: Activity created."); // Log khi Activity được tạo

        mAuth = FirebaseAuth.getInstance();
        firebaseDAO = new FirebaseDAO();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "onCreate: User is null, finishing activity."); // Log lỗi user null
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();
        Log.i(TAG, "onCreate: User authenticated with UID: " + currentUserId); // Log UID

        setupToolbar();
        setupRecyclerView();
        setupActivityResultLaunchers();
        setupListeners();
        loadCategories(); // Gọi tải dữ liệu
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarManageCategories);
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
        Log.d(TAG, "setupRecyclerView: Initializing RecyclerView and Adapter."); // Log setup RV
        binding.recyclerViewManageCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter(this, categoryList, this);
        binding.recyclerViewManageCategories.setAdapter(categoryAdapter);
        Log.d(TAG, "setupRecyclerView: Adapter set."); // Log đã set adapter
    }

    private void setupActivityResultLaunchers() {

        // --- Launcher cho việc THÊM MỚI category ---
        addCategoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), // Contract chuẩn để khởi chạy Activity và nhận kết quả
                result -> { // Callback xử lý kết quả trả về
                    if (result.getResultCode() == RESULT_OK) {
                        // Nếu Activity thêm mới trả về kết quả OK (thường nghĩa là đã lưu thành công)
                        Log.d(TAG, "Returned from Add Category with RESULT_OK. Reloading categories...");
                        loadCategories(); // Tải lại danh sách để cập nhật
                    } else {
                        // Xử lý trường hợp người dùng nhấn Back hoặc có lỗi khác
                        Log.d(TAG, "Returned from Add Category with result code: " + result.getResultCode());
                    }
                });

        // --- Launcher cho việc CHỈNH SỬA category ---
        editCategoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), // Contract tương tự
                result -> { // Callback xử lý kết quả trả về
                    if (result.getResultCode() == RESULT_OK) {
                        // Nếu Activity chỉnh sửa trả về kết quả OK
                        Log.d(TAG, "Returned from Edit Category with RESULT_OK. Reloading categories...");
                        loadCategories(); // Tải lại danh sách để cập nhật thay đổi
                    } else {
                        Log.d(TAG, "Returned from Edit Category with result code: " + result.getResultCode());
                    }
                });
    }


    private void setupListeners() {
        Log.d(TAG, "setupListeners: Setting up FAB click listener."); // Log setup listener
        binding.fabAddCategory.setOnClickListener(v -> {
            Log.d(TAG, "fabAddCategory clicked. Launching AddCategoryActivity."); // Log khi nhấn FAB
            Intent intent = new Intent(ManageCategoriesActivity.this, AddCategoryActivity.class);
            addCategoryLauncher.launch(intent);
        });
    }


    private void loadCategories() {
        Log.d(TAG, "loadCategories: Attempting to load categories for user: " + currentUserId); // Log bắt đầu load
        showLoading(true);
        binding.textNoCategoriesManage.setVisibility(View.GONE);

        firebaseDAO.getAllCategories(currentUserId, new FirebaseDAO.OnCategoriesRetrievedListener() {
            @Override
            public void onSuccess(List<Category> categories) {
                Log.i(TAG, "loadCategories - onSuccess: Received " + (categories != null ? categories.size() : "null") + " categories."); // Log số lượng nhận được
                showLoading(false);
                if (categories == null || categories.isEmpty()) {
                    Log.d(TAG, "loadCategories - onSuccess: Category list is empty or null."); // Log nếu rỗng
                    showEmptyState("No categories found. Add one!");
                    categoryList.clear();
                } else {
                    Log.d(TAG, "loadCategories - onSuccess: Populating categoryList."); // Log trước khi addAll
                    binding.recyclerViewManageCategories.setVisibility(View.VISIBLE);
                    binding.textNoCategoriesManage.setVisibility(View.GONE);
                    categoryList.clear();
                    categoryList.addAll(categories);
                    Log.d(TAG, "loadCategories - onSuccess: categoryList size after addAll: " + categoryList.size()); // Log size sau khi addAll
                }
                Log.d(TAG, "loadCategories - onSuccess: Calling adapter.updateData()."); // Log trước khi gọi updateData
                categoryAdapter.updateData(categoryList);
                Log.d(TAG, "loadCategories - onSuccess: adapter.updateData() called."); // Log sau khi gọi updateData
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "loadCategories - onFailure: Failed to load categories", e); // Log chi tiết lỗi
                showLoading(false);
                Toast.makeText(ManageCategoriesActivity.this, "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState("Failed to load categories.");
            }
        });
    }

    private void showEmptyState(String message) {
        Log.d(TAG, "showEmptyState: Displaying message - " + message); // Log khi hiển thị trạng thái rỗng
        if(binding != null){
            binding.recyclerViewManageCategories.setVisibility(View.GONE);
            binding.textNoCategoriesManage.setText(message);
            binding.textNoCategoriesManage.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        Log.d(TAG, "showLoading: Setting loading state to " + isLoading); // Log trạng thái loading
        if (binding != null) {
            binding.progressBarManageCategories.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            // Chỉ ẩn/hiện RV và text empty dựa trên isLoading, việc kiểm tra list rỗng sẽ do onSuccess xử lý
            binding.recyclerViewManageCategories.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            binding.textNoCategoriesManage.setVisibility(View.GONE); // Luôn ẩn text empty khi loading
        }
    }

    @Override
    public void onEditCategoryClick(Category category, int position) {
        Log.d(TAG, "onEditCategoryClick: Category '" + category.getName() + "' at position " + position); // Log sự kiện edit
        Intent intent = new Intent(this, AddCategoryActivity.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("CATEGORY_ID", category.getFirebaseId());
        editCategoryLauncher.launch(intent);
    }

    @Override
    public void onDeleteCategoryClick(Category category, int position) {
        Log.d(TAG, "onDeleteCategoryClick: Category '" + category.getName() + "' at position " + position); // Log sự kiện delete
        if (category == null || category.getFirebaseId() == null) {
            Log.e(TAG, "onDeleteCategoryClick: Category or Category ID is null.");
            return;
        }
        showDeleteConfirmationDialog(category, position);
    }

    private void showDeleteConfirmationDialog(Category category, int position) {
        Log.d(TAG, "showDeleteConfirmationDialog: Showing confirmation for '" + category.getName() + "'"); // Log hiển thị dialog xóa
        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete '" + category.getName() + "'? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.d(TAG, "showDeleteConfirmationDialog: Delete confirmed for '" + category.getName() + "'"); // Log xác nhận xóa
                    deleteCategoryFromDb(category, position);
                })
                .setNegativeButton("Cancel", (dialog, which) -> Log.d(TAG, "showDeleteConfirmationDialog: Delete cancelled.")) // Log hủy xóa
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void deleteCategoryFromDb(Category category, int position) {
        Log.d(TAG, "deleteCategoryFromDb: Attempting to delete category: " + category.getFirebaseId()); // Log bắt đầu xóa DB
        showLoading(true);
        firebaseDAO.deleteCategory(currentUserId, category.getFirebaseId(), new FirebaseDAO.OnCategoryDeletedListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "deleteCategoryFromDb - onSuccess: Category deleted from DB."); // Log xóa DB thành công
                handleDeletionSuccess(category, position);
                Toast.makeText(ManageCategoriesActivity.this, "Category Deleted", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "deleteCategoryFromDb - onFailure: Failed to delete category", e); // Log xóa DB thất bại
                showLoading(false);
                Toast.makeText(ManageCategoriesActivity.this, "Error deleting category: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDeletionSuccess(Category deletedCategory, int position) {
        Log.d(TAG, "handleDeletionSuccess: Updating UI after deleting category at position " + position); // Log cập nhật UI sau xóa
        showLoading(false);
        if (position >= 0 && position < categoryList.size() && categoryList.get(position).getFirebaseId().equals(deletedCategory.getFirebaseId())) {
            categoryList.remove(position);
            Log.d(TAG, "handleDeletionSuccess: Removed from local list. Notifying adapter."); // Log xóa khỏi list cục bộ
            categoryAdapter.notifyItemRemoved(position);
            if (categoryList.isEmpty()) {
                Log.d(TAG, "handleDeletionSuccess: List is now empty. Showing empty state."); // Log nếu list rỗng
                showEmptyState("No categories found. Add one!");
            }
        } else {
            Log.w(TAG, "handleDeletionSuccess: Position mismatch or error. Reloading data."); // Log lỗi vị trí
            loadCategories(); // Tải lại nếu có lỗi
        }
    }

    public interface OnCategoryDeletedListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}