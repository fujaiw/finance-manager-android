package com.example.financemanager.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 记账模板（快捷记账）。
 * amount=0 表示模板不预设金额；reason 为空串表示不预设备注。
 */
@Entity(tableName = "transaction_templates")
public class TransactionTemplate {
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** 模板名称（通常取原因） */
    public String name;
    /** Expense / Income */
    public String type;
    /** 二级分类 */
    public String subCategory;
    /** 默认金额，0 表示不预设 */
    public double amount;
    /** 默认原因/备注，空串表示不预设 */
    public String reason;
    /** 是否家庭账目 */
    public boolean householdAccounts;
    public long createdAt;
    public long lastUsedAt;

    public TransactionTemplate() {
    }
}
