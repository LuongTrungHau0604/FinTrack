package com.example.myapplication.ui.accounts;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Import ImageButton
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Account;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private static final String TAG = "AccountAdapter";
    private List<Account> accountList;
    private Context context;
    private NumberFormat defaultCurrencyFormatter;
    private OnAccountInteractionListener interactionListener; // Chỉ giữ lại listener này

    // Interface để Activity xử lý click Edit/Delete
    public interface OnAccountInteractionListener {
        void onEditAccountClick(Account account, int position);
        void onDeleteAccountClick(Account account, int position);
    }

    // Constructor chính, BẮT BUỘC nhận listener
    public AccountAdapter(Context context, List<Account> accountList, OnAccountInteractionListener listener) {
        this.context = context;
        this.accountList = (accountList != null) ? accountList : new ArrayList<>();
        this.defaultCurrencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        this.interactionListener = listener; // Luôn lưu listener
    }

    // Bỏ constructor thứ hai đi nếu bạn luôn muốn có listener cho Edit/Delete

    public void updateData(List<Account> newAccounts) {
        Log.d(TAG, "Adapter updateData: Received " + (newAccounts != null ? newAccounts.size() : "null") + " accounts.");
        this.accountList = (newAccounts != null) ? new ArrayList<>(newAccounts) : new ArrayList<>();
        Log.d(TAG, "Adapter updateData: this.accountList size AFTER assignment: " + this.accountList.size()); // QUAN TRỌNG
        notifyDataSetChanged();
        Log.d(TAG, "Adapter updateData: notifyDataSetChanged() called.");
    }


    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called for viewType: " + viewType); // Log khi tạo VH
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder called for position: " + position); // Log khi bind
        Account account = accountList.get(position);
        // Kiểm tra null cơ bản cho các view chính trước khi bind
        if (holder.txtAccountName != null && holder.txtAccountBalance != null && holder.imgAccountIcon != null) {
            holder.bind(account, context, defaultCurrencyFormatter);
        } else {
            Log.e(TAG, "ViewHolder core views are null at position: " + position + ". Check ViewHolder constructor and layout IDs.");
            return; // Không bind nếu view bị null
        }

        // Gắn Listener cho nút Edit và Delete
        // Luôn kiểm tra interactionListener và các nút không null
        if (interactionListener != null) {
            if (holder.btnEdit != null) {
                holder.btnEdit.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        interactionListener.onEditAccountClick(accountList.get(currentPosition), currentPosition);
                    }
                });
            } else {
                Log.w(TAG, "btnEdit is null in ViewHolder at position " + position + ". Check layout ID R.id.btn_edit_account.");
            }

            if (holder.btnDelete != null) {
                holder.btnDelete.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        interactionListener.onDeleteAccountClick(accountList.get(currentPosition), currentPosition);
                    }
                });
            } else {
                Log.w(TAG, "btnDelete is null in ViewHolder at position " + position + ". Check layout ID R.id.btn_delete_account.");
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = accountList != null ? accountList.size() : 0;
        Log.d("AccountAdapter", "getItemCount: Returning count = " + count); // DÙNG Log.d
        return count;
    }

    // --- ViewHolder đã sửa ---
    static class AccountViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAccountIcon;
        TextView txtAccountName, txtAccountType, txtAccountBalance;
        ImageButton btnEdit, btnDelete; // Khai báo các nút

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            // Tìm tất cả các View bằng ID
            imgAccountIcon = itemView.findViewById(R.id.img_account_icon);
            txtAccountName = itemView.findViewById(R.id.txt_account_name);
            txtAccountType = itemView.findViewById(R.id.txt_account_type);
            txtAccountBalance = itemView.findViewById(R.id.txt_account_balance);
            // !!! THÊM findViewById CHO CÁC NÚT !!!
            btnEdit = itemView.findViewById(R.id.btn_edit_account);
            btnDelete = itemView.findViewById(R.id.btn_delete_account);

            // Log kiểm tra (tùy chọn)
            if (btnEdit == null) Log.e("AccountViewHolder", "btnEdit (R.id.btn_edit_account) not found!");
            if (btnDelete == null) Log.e("AccountViewHolder", "btnDelete (R.id.btn_delete_account) not found!");
        }

        // --- Phương thức bind() ---
        public void bind(Account account, Context context, NumberFormat defaultFormatter) {
            if (account == null) {
                Log.e("AccountViewHolderBind", "Attempting to bind null account");
                // Set default/empty state for views
                if(txtAccountName != null) txtAccountName.setText("Error");
                if(txtAccountType != null) txtAccountType.setText("");
                if(txtAccountBalance != null) txtAccountBalance.setText("");
                if(imgAccountIcon != null) imgAccountIcon.setImageResource(R.drawable.ic_account_balance_wallet);
                return;
            }

            // Bind data to views (with null checks for safety)
            if(txtAccountName != null) txtAccountName.setText(account.getName() != null ? account.getName() : "N/A");

            if (txtAccountType != null) {
                txtAccountType.setText(formatAccountType(account.getType()));
            }

            if(txtAccountBalance != null) {
                try {
                    NumberFormat specificFormatter = NumberFormat.getCurrencyInstance();
                    Currency accountCurrency = null;
                    String currencyCode = account.getCurrency();

                    if (currencyCode != null && !currencyCode.isEmpty()) {
                        try {
                            accountCurrency = Currency.getInstance(currencyCode);
                            specificFormatter.setCurrency(accountCurrency);
                        } catch (IllegalArgumentException e) {
                            specificFormatter = defaultFormatter; // Fallback
                        }
                    } else {
                        specificFormatter = defaultFormatter; // Fallback
                    }
                    if(accountCurrency != null) {
                        specificFormatter.setMinimumFractionDigits(accountCurrency.getDefaultFractionDigits());
                        specificFormatter.setMaximumFractionDigits(accountCurrency.getDefaultFractionDigits());
                    }

                    txtAccountBalance.setText(specificFormatter.format(account.getCurrentBalance()));
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting currency for " + account.getName(), e);
                    txtAccountBalance.setText(String.format(Locale.US, "%.2f", account.getCurrentBalance())); // Basic fallback
                }
            }

            if(imgAccountIcon != null){
                String iconName = (account.getIcon() != null && !account.getIcon().isEmpty()) ? account.getIcon() : "ic_account_balance_wallet";
                try {
                    int iconResId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
                    imgAccountIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_account_balance_wallet);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting icon for " + account.getName(), e);
                    imgAccountIcon.setImageResource(R.drawable.ic_account_balance_wallet);
                }
            }
        }

        // --- Hàm helper formatAccountType giữ nguyên ---
        private String formatAccountType(String type) {
            if (type == null) return "";
            String formattedType = type.toLowerCase(Locale.ROOT);
            switch (formattedType) {
                case "cash": return "Cash";
                case "bank": return "Bank Account";
                case "e_wallet": return "E-Wallet";
                case "savings": return "Savings";
                default:
                    if(type.length() > 0) { return Character.toUpperCase(type.charAt(0)) + type.substring(1); }
                    return type;
            }
        }
    }
}