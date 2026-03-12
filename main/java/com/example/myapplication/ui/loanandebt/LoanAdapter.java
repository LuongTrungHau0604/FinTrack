package com.example.myapplication.ui.loanandebt; // Hoặc package adapters của bạn

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Loan; // Import model Loan

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.LoanViewHolder> {

    private static final String TAG = "LoanAdapter";
    private List<Loan> loanList;
    private Context context;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); // Để hiển thị ngày
    private OnLoanInteractionListener interactionListener;

    public interface OnLoanInteractionListener {
        void onEditLoanClick(Loan loan, int position);
        void onDeleteLoanClick(Loan loan, int position);
        // void onLoanItemClick(Loan loan, int position); // Optional
    }

    public LoanAdapter(Context context, List<Loan> loanList, OnLoanInteractionListener listener) {
        this.context = context;
        this.loanList = (loanList != null) ? loanList : new ArrayList<>();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        this.interactionListener = listener;
    }

    public void updateData(List<Loan> newLoans) {
        this.loanList = (newLoans != null) ? new ArrayList<>(newLoans) : new ArrayList<>();
        notifyDataSetChanged(); // Hoặc dùng DiffUtil
    }

    @NonNull
    @Override
    public LoanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_loan, parent, false);
        return new LoanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoanViewHolder holder, int position) {
        Loan loan = loanList.get(position);
        holder.bind(loan, context, currencyFormatter, displayDateFormat);

        if (interactionListener != null) {
            if (holder.btnEdit != null) {
                holder.btnEdit.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        interactionListener.onEditLoanClick(loanList.get(currentPosition), currentPosition);
                    }
                });
            }
            if (holder.btnDelete != null) {
                holder.btnDelete.setOnClickListener(v -> {
                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        interactionListener.onDeleteLoanClick(loanList.get(currentPosition), currentPosition);
                    }
                });
            }
            // Gắn listener cho itemView nếu cần
        }
    }

    @Override
    public int getItemCount() {
        return loanList.size();
    }

    static class LoanViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtName, txtEntity, txtDueDate, txtRemainingBalance;
        ImageButton btnEdit, btnDelete;

        public LoanViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_loan_icon);
            txtName = itemView.findViewById(R.id.txt_loan_name);
            txtEntity = itemView.findViewById(R.id.txt_loan_entity);
            txtDueDate = itemView.findViewById(R.id.txt_loan_due_date);
            txtRemainingBalance = itemView.findViewById(R.id.txt_loan_remaining_balance);
            btnEdit = itemView.findViewById(R.id.btn_edit_loan);
            btnDelete = itemView.findViewById(R.id.btn_delete_loan);
        }

        public void bind(Loan loan, Context context, NumberFormat defaultFormatter, SimpleDateFormat dateFormatter) {
            if (loan == null) return;

            if(txtName != null) txtName.setText(loan.getName() != null ? loan.getName() : "N/A");
            if(txtEntity != null) txtEntity.setText(loan.getEntityName() != null ? loan.getEntityName() : "");
            if(txtDueDate != null) {
                // Hiển thị ngày đến hạn (có thể format lại nếu muốn)
                String dueDateText = "Due: " + (loan.getDueDate() != null ? loan.getDueDate() : "N/A");
                // Example formatting timestamp if available
                 /*
                 if(loan.getDueDateTimestamp() != null) { // Assuming you add a Timestamp field
                      dueDateText = "Due: " + dateFormatter.format(loan.getDueDateTimestamp().toDate());
                 }
                 */
                txtDueDate.setText(dueDateText);
            }

            // Format số dư còn lại
            if (txtRemainingBalance != null) {
                try {
                    NumberFormat specificFormatter = NumberFormat.getCurrencyInstance();
                    Currency loanCurrency = null;
                    String currencyCode = "VND"; // Default or get from loan/settings
                    // if (loan.getCurrency() != null && !loan.getCurrency().isEmpty()) {
                    //    currencyCode = loan.getCurrency();
                    // }
                    try {
                        loanCurrency = Currency.getInstance(currencyCode);
                        specificFormatter.setCurrency(loanCurrency);
                        specificFormatter.setMinimumFractionDigits(loanCurrency.getDefaultFractionDigits());
                        specificFormatter.setMaximumFractionDigits(loanCurrency.getDefaultFractionDigits());
                    } catch (Exception e) { specificFormatter = defaultFormatter; }

                    txtRemainingBalance.setText(specificFormatter.format(loan.getCurrentBalance()));
                    // Đặt màu nếu cần (vd: màu đỏ nếu sắp đến hạn/quá hạn)

                } catch (Exception e) {
                    txtRemainingBalance.setText(String.format(Locale.US, "%.2f", loan.getCurrentBalance()));
                }
            }


        }
    }
}