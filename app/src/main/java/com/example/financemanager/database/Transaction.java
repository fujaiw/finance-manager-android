package com.example.financemanager.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String dateTime;
    public double amount;
    public String category;
    public String subCategory;
    public String reason;
    public boolean householdAccounts;
    public boolean isCanceled;

    public Transaction() {}

    @Ignore
    public Transaction(String dateTime, double amount, String category, String subCategory, 
                      String reason, boolean householdAccounts, boolean isCanceled) {
        this.dateTime = dateTime;
        this.amount = amount;
        this.category = category;
        this.subCategory = subCategory;
        this.reason = reason;
        this.householdAccounts = householdAccounts;
        this.isCanceled = isCanceled;
    }

    public String getCategoryType() {
        return category;
    }

    public boolean isExpense() {
        return "Expense".equals(category);
    }

    public boolean isIncome() {
        return "Income".equals(category);
    }
}
