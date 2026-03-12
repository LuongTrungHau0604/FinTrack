package com.example.myapplication.ui.categories;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private static final String TAG = "CategoryAdapter"; // Tag riêng cho Adapter
    private List<Category> categoryList;
    private Context context;
    private OnCategoryInteractionListener interactionListener;

    public interface OnCategoryInteractionListener {
        void onEditCategoryClick(Category category, int position);
        void onDeleteCategoryClick(Category category, int position);
    }

    public CategoryAdapter(Context context, List<Category> categoryList, OnCategoryInteractionListener listener) {
        this.context = context;
        this.categoryList = (categoryList != null) ? categoryList : new ArrayList<>();
        this.interactionListener = listener;
        Log.d(TAG, "Adapter created. Initial list size: " + this.categoryList.size()); // Log khi tạo Adapter
    }

    public void updateData(List<Category> newCategories) {
        Log.d(TAG, "updateData: Received new data with size: " + (newCategories != null ? newCategories.size() : "null"));

        // --- THAY ĐỔI CÁCH CẬP NHẬT ---
        if (newCategories != null) {
            // Tạo một bản sao của danh sách mới để đảm bảo an toàn tham chiếu
            this.categoryList = new ArrayList<>(newCategories);
        } else {
            // Nếu danh sách mới là null, đặt danh sách nội bộ thành rỗng
            this.categoryList = new ArrayList<>();
        }
        // --- KẾT THÚC THAY ĐỔI ---

        Log.d(TAG, "updateData: Internal list size AFTER assignment: " + this.categoryList.size()); // Kiểm tra size sau khi gán
        Log.d(TAG, "updateData: Calling notifyDataSetChanged().");
        notifyDataSetChanged();
        Log.d(TAG, "updateData: notifyDataSetChanged() called.");
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: Inflating layout for item view."); // Log khi tạo ViewHolder
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_category_manage, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        Log.d(TAG, "onBindViewHolder: Binding data for position " + position + ", Category: " + (category != null ? category.getName() : "null")); // Log khi bind
        holder.bind(category, context);

        if (interactionListener != null) {
            if (holder.btnEdit != null) {
                holder.btnEdit.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        Log.d(TAG, "onBindViewHolder: Edit button clicked for position " + currentPosition); // Log click edit
                        interactionListener.onEditCategoryClick(categoryList.get(currentPosition), currentPosition);
                    }
                });
            }
            if (holder.btnDelete != null) {
                holder.btnDelete.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        Log.d(TAG, "onBindViewHolder: Delete button clicked for position " + currentPosition); // Log click delete
                        interactionListener.onDeleteCategoryClick(categoryList.get(currentPosition), currentPosition);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = categoryList.size();
        Log.v(TAG, "getItemCount: Returning count = " + count); // Log số lượng item (Verbose)
        return count;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtName;
        ImageButton btnEdit, btnDelete;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_category_manage_icon);
            txtName = itemView.findViewById(R.id.txt_category_manage_name);
            btnEdit = itemView.findViewById(R.id.btn_edit_category);
            btnDelete = itemView.findViewById(R.id.btn_delete_category);
            Log.v("CategoryViewHolder", "ViewHolder created. Icon is " + (imgIcon == null ? "NULL" : "OK") + ", Name is " + (txtName == null ? "NULL" : "OK")); // Log tạo ViewHolder (Verbose)
        }

        public void bind(Category category, Context context) {
            if (category == null) {
                Log.w("CategoryViewHolderBind", "Attempting to bind null category."); // Log nếu category null
                if (txtName != null) txtName.setText("Error");
                if (imgIcon != null) imgIcon.setImageResource(R.drawable.ic_placeholder_category);
                return;
            }
            Log.v("CategoryViewHolderBind", "Binding category: " + category.getName()); // Log tên category đang bind (Verbose)

            if(txtName != null) txtName.setText(category.getName() != null ? category.getName() : "N/A");

            if (imgIcon != null) {
                String iconName = (category.getIcon() != null && !category.getIcon().isEmpty()) ? category.getIcon() : "ic_placeholder_category";
                try {
                    int iconResId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
                    imgIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_placeholder_category);
                } catch (Exception e) {
                    Log.e("CategoryViewHolderBind", "Error setting icon for category " + category.getName(), e);
                    imgIcon.setImageResource(R.drawable.ic_placeholder_category);
                }
            }
        }
    }
}