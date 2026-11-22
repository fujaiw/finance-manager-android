package com.example.financemanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.financemanager.R;
import com.example.financemanager.database.Transaction;
import com.example.financemanager.model.CategoryManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends ArrayAdapter<Transaction> {
    private Context context;
    private List<Transaction> transactions;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        super(context, 0, transactions);
        this.context = context;
        this.transactions = transactions;
    }

    /**
     * 动态替换数据源并刷新列表（搜索/筛选结果更新时调用）。
     */
    public void updateData(List<Transaction> newTransactions) {
        this.transactions = newTransactions != null ? newTransactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Transaction transaction = transactions.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        }

        TextView tvCategory = convertView.findViewById(R.id.tvCategory);
        TextView tvReason = convertView.findViewById(R.id.tvReason);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);
        TextView tvDate = convertView.findViewById(R.id.tvDate);

        String category = transaction.subCategory != null ? transaction.subCategory : "未分类";
        tvCategory.setText(category);
        tvReason.setText(transaction.reason);

        String amount = transaction.isExpense() ? "-¥" + String.format("%.2f", transaction.amount) 
                                                : "+¥" + String.format("%.2f", transaction.amount);
        tvAmount.setText(amount);
        tvAmount.setTextColor(transaction.isExpense() 
                ? context.getResources().getColor(R.color.expense_color)
                : context.getResources().getColor(R.color.income_color));

        String dateStr = transaction.dateTime;
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            dateStr = outputFormat.format(inputFormat.parse(dateStr));
        } catch (Exception e) {
            e.printStackTrace();
        }
        tvDate.setText(dateStr);

        return convertView;
    }
}
