package com.example.myapplication.ui.home; // Đảm bảo đúng package

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.ui.transactions.AllTransactionsActivity;
import com.example.myapplication.ui.accounts.ManageCategoriesActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

// Import ViewBinding cho fragment
import com.example.myapplication.databinding.FragmentAddOptionsBottomSheetBinding;

public class AddOptionsBottomSheetFragment extends BottomSheetDialogFragment {

    // Tag để hiển thị Fragment
    public static final String TAG = "AddOptionsBottomSheet";

    private FragmentAddOptionsBottomSheetBinding binding; // Sử dụng ViewBinding

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout bằng ViewBinding
        binding = FragmentAddOptionsBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Gắn sự kiện click cho từng tùy chọn
        binding.textManageTransactions.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AllTransactionsActivity.class));
            dismiss(); // Đóng Bottom Sheet sau khi chọn
        });

        binding.textManageCategories.setOnClickListener(v -> {
            // TODO: Tạo AddCategoryActivity
            startActivity(new Intent(getActivity(), ManageCategoriesActivity.class)); // Điều hướng đến Activity tương ứng
            Toast.makeText(getActivity(), "Add Category Clicked (Implement Activity)", Toast.LENGTH_SHORT).show();
            dismiss();
        });


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Tránh memory leak với ViewBinding trong Fragment
    }
}