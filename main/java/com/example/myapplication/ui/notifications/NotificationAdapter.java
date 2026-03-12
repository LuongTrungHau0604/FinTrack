package com.example.myapplication.ui.notifications;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.NotificationItem;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<NotificationItem> notificationList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public NotificationAdapter(Context context, List<NotificationItem> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notificationList.get(position);

        holder.titleTextView.setText(item.getTitle());
        holder.descriptionTextView.setText(item.getDescription());

        // Hiển thị thời gian
        Timestamp timestamp = item.getTimestamp();
        if (timestamp != null) {
            Date date = timestamp.toDate();
            holder.dateTextView.setText(dateFormat.format(date));
        } else {
            holder.dateTextView.setText("N/A");
        }

        // Thiết lập icon dựa trên loại thông báo
        setupNotificationIcon(holder, item);

        // Nếu là thông báo quan trọng (sắp hết hạn), hiển thị đánh dấu đỏ
        if (item.getIsImportant()) {
            holder.importantIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.importantIndicator.setVisibility(View.GONE);
        }
    }

    private void setupNotificationIcon(NotificationViewHolder holder, NotificationItem item) {
        String type = item.getType();

        if (type != null) {
            switch (type.toLowerCase()) {
                case "loan":
                    holder.iconImageView.setImageResource(R.drawable.ic_loan);
                    holder.iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                    break;
                case "debt":
                    holder.iconImageView.setImageResource(R.drawable.ic_debt);
                    holder.iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
                    break;
                case "transaction":
                    if (item.getTitle().contains("Income")) {
                        holder.iconImageView.setImageResource(R.drawable.ic_income);
                        holder.iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorIncome));
                    } else {
                        holder.iconImageView.setImageResource(R.drawable.ic_expense);
                        holder.iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorExpense));
                    }
                    break;
                default:
                    holder.iconImageView.setImageResource(R.drawable.ic_notification);
                    holder.iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                    break;
            }
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_notification);
            holder.iconImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
        }
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView dateTextView;
        View importantIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.imageViewNotificationIcon);
            titleTextView = itemView.findViewById(R.id.textViewNotificationTitle);
            descriptionTextView = itemView.findViewById(R.id.textViewNotificationDescription);
            dateTextView = itemView.findViewById(R.id.textViewNotificationDate);
            importantIndicator = itemView.findViewById(R.id.viewImportantIndicator);
        }
    }
}