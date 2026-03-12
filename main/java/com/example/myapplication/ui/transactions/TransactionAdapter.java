package com.example.myapplication.ui.transactions;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Category;
import com.example.myapplication.data.model.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;
    private Map<String, Category> categoryMap;
    private Context context;
    private NumberFormat currencyFormatter;
    private OnTransactionInteractionListener interactionListener;

    public interface OnTransactionInteractionListener {
        void onEditClick(Transaction transaction, int position);
        void onDeleteClick(Transaction transaction, int position);
        // Optional: Keep these if you need click/long click on the whole item row
        // void onItemClick(Transaction transaction, int position);
        // void onItemLongClick(Transaction transaction, int position);
    }

    public TransactionAdapter(Context context, List<Transaction> transactionList, Map<String, Category> categoryMap, OnTransactionInteractionListener listener) {
        this.context = context;
        this.transactionList = (transactionList != null) ? transactionList : new ArrayList<>();
        this.categoryMap = (categoryMap != null) ? categoryMap : new HashMap<>();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault()); // Default formatter
        this.interactionListener = listener;
    }

    public void updateData(List<Transaction> newTransactions, Map<String, Category> newCategoryMap) {
        Log.d(TAG, "TAdapter updateData: Received " + (newTransactions != null ? newTransactions.size() : "null") + " transactions");
        this.transactionList = (newTransactions != null) ? new ArrayList<>(newTransactions) : new ArrayList<>();
        this.categoryMap = (newCategoryMap != null) ? new HashMap<>(newCategoryMap) : new HashMap<>(); // Cũng nên tạo map mới
        Log.d(TAG, "TAdapter updateData: Internal list size now: " + this.transactionList.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_transaction_home, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        // Ensure view holder views are not null before binding
        if (holder.txtDescription != null && holder.txtCategoryName != null && holder.txtAmount != null) {
            holder.bind(transaction, categoryMap, context, currencyFormatter); // Pass default formatter
        } else {
            Log.e("TransactionAdapter", "ViewHolder views are null at position: " + position);
        }


        if (interactionListener != null) {
            if (holder.btnEdit != null) {
                holder.btnEdit.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        interactionListener.onEditClick(transactionList.get(currentPosition), currentPosition);
                    }
                });
            } else {
                Log.e("TransactionAdapter", "btnEdit is null at position: " + position);
            }

            if (holder.btnDelete != null) {
                holder.btnDelete.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        interactionListener.onDeleteClick(transactionList.get(currentPosition), currentPosition);
                    }
                });
            } else {
                Log.e("TransactionAdapter", "btnDelete is null at position: " + position);
            }
            // Optional: Whole item click
            /*
            holder.itemView.setOnClickListener(v -> {
                 int currentPosition = holder.getAdapterPosition();
                 if (currentPosition != RecyclerView.NO_POSITION && interactionListener::onItemClick != null) { // Check if method exists
                    interactionListener.onItemClick(transactionList.get(currentPosition), currentPosition);
                 }
            });
            holder.itemView.setOnLongClickListener(v -> {
                 int currentPosition = holder.getAdapterPosition();
                 if (currentPosition != RecyclerView.NO_POSITION && interactionListener::onItemLongClick != null) {
                    interactionListener.onItemLongClick(transactionList.get(currentPosition), currentPosition);
                    return true;
                 }
                 return false;
            });
             */
        }
    }

    @Override
    public int getItemCount() {
        int count = transactionList.size();
        Log.v("TransactionAdapter", "getItemCount: Returning " + count);
        return count;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCategoryIcon;
        TextView txtDescription, txtCategoryName, txtAmount, txtTransactionDate;
        ImageButton btnEdit, btnDelete;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCategoryIcon = itemView.findViewById(R.id.img_category_icon);
            txtDescription = itemView.findViewById(R.id.txt_transaction_description);
            txtCategoryName = itemView.findViewById(R.id.txt_transaction_category);
            txtAmount = itemView.findViewById(R.id.txt_transaction_amount); // Sửa thành txt_transaction_amount
            txtTransactionDate = itemView.findViewById(R.id.txt_transaction_date);
            btnEdit = itemView.findViewById(R.id.btn_edit_transaction);
            btnDelete = itemView.findViewById(R.id.btn_delete_transaction);

            // Check if views are found immediately (optional debug)
            if (txtAmount == null) {
                Log.e("ViewHolder", "txtAmount (R.id.txt_account_balance) is NULL!");
            }
            if (imgCategoryIcon == null) {
                Log.e("ViewHolder", "imgCategoryIcon (R.id.img_category_icon) is NULL!");
            }
            if (txtTransactionDate == null) {
                Log.e("ViewHolder", "txtTransactionDate (R.id.txt_transaction_date) is NULL!");
            }
            // Add checks for other views if needed
        }

        public void bind(Transaction transaction, Map<String, Category> categoryMap, Context context, NumberFormat defaultFormatter) {
            if (transaction == null) {
                Log.e("ViewHolderBind", "Attempting to bind a null transaction object.");
                // Optionally clear views or show placeholder text
                if(txtDescription!=null) txtDescription.setText("");
                if(txtCategoryName!=null) txtCategoryName.setText("");
                if(txtAmount!=null) txtAmount.setText("");
                if(txtTransactionDate!=null) txtTransactionDate.setText("");
                if(imgCategoryIcon!=null) imgCategoryIcon.setImageResource(R.drawable.ic_placeholder_category);
                return; // Exit early if transaction is null
            }

            if(txtDescription!=null) txtDescription.setText(transaction.getDescription() != null ? transaction.getDescription() : "");

            String categoryName = "Uncategorized";
            String categoryIconName = "ic_placeholder_category";
            if (categoryMap != null && transaction.getCategoryId() != null && categoryMap.containsKey(transaction.getCategoryId())) {
                Category category = categoryMap.get(transaction.getCategoryId());
                if (category != null) {
                    if (category.getName() != null) categoryName = category.getName();
                    if (category.getIcon() != null && !category.getIcon().isEmpty()) categoryIconName = category.getIcon();
                }
            }
            if(txtCategoryName!=null) txtCategoryName.setText(categoryName);

            if (imgCategoryIcon != null) {
                try {
                    int iconResId = context.getResources().getIdentifier(categoryIconName, "drawable", context.getPackageName());
                    imgCategoryIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.ic_placeholder_category);
                } catch (Exception e) {
                    Log.e("ViewHolderBind", "Error setting category icon", e);
                    imgCategoryIcon.setImageResource(R.drawable.ic_placeholder_category);
                }
            }

            // Amount Formatting (Handle currency symbol correctly)
            if(txtAmount!=null){
                double amount = transaction.getAmount();
                boolean isExpense = "expense".equalsIgnoreCase(transaction.getType());
                int amountColor;
                String prefix = isExpense ? "-" : "+";
                String formattedAmount;

                try {
                    // Create a specific formatter for the transaction's currency
                    NumberFormat specificFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    java.util.Currency transactionCurrency = null;
                    if (transaction.getCurrency() != null && !transaction.getCurrency().isEmpty()) {
                        try {
                            transactionCurrency = java.util.Currency.getInstance(transaction.getCurrency());
                            specificFormatter.setCurrency(transactionCurrency);
                        } catch (IllegalArgumentException e) {
                            Log.w("ViewHolderBind", "Invalid currency code: " + transaction.getCurrency() + ". Using default locale.");
                            // Keep default locale formatter if currency code is invalid
                        }
                    } else {
                        // Use default locale formatter if no currency is specified
                        Log.w("ViewHolderBind", "Transaction currency is null or empty. Using default locale.");
                    }
                    // Format the absolute amount
                    formattedAmount = specificFormatter.format(Math.abs(amount));

                } catch (Exception e) {
                    Log.e("ViewHolderBind", "Error formatting currency. Falling back to default.", e);
                    // Fallback to default formatter if specific currency fails
                    formattedAmount = defaultFormatter.format(Math.abs(amount));
                }


                txtAmount.setText(prefix + formattedAmount);

                if (isExpense) {
                    amountColor = ContextCompat.getColor(context, R.color.colorExpense);
                } else {
                    amountColor = ContextCompat.getColor(context, R.color.colorIncome);
                }
                txtAmount.setTextColor(amountColor);
                txtAmount.setVisibility(View.VISIBLE);
            }


            if (txtTransactionDate != null) {
                if (transaction.getDate() != null) {
                    txtTransactionDate.setText(transaction.getDate());
                    txtTransactionDate.setVisibility(View.VISIBLE);
                } else {
                    txtTransactionDate.setVisibility(View.GONE);
                }
            }
        }
    }
}