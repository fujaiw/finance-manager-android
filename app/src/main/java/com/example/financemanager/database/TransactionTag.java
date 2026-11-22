package com.example.financemanager.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 用户自定义交易标签。标签名全局唯一。
 */
@Entity(tableName = "transaction_tags",
        indices = {@Index(value = "name", unique = true)})
public class TransactionTag {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public TransactionTag() {
    }

    @Ignore
    public TransactionTag(String name) {
        this.name = name;
    }
}
