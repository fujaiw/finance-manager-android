package com.example.financemanager.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

/**
 * 交易-标签 多对多关联表。
 * 交易被删除时级联删除其标签关联；标签本身的删除/改名由 TagDao 维护。
 */
@Entity(tableName = "transaction_tag_cross_ref",
        primaryKeys = {"transactionId", "tag"},
        indices = {@Index("transactionId"), @Index("tag")},
        foreignKeys = @ForeignKey(
                entity = Transaction.class,
                parentColumns = "id",
                childColumns = "transactionId",
                onDelete = ForeignKey.CASCADE))
public class TransactionTagCrossRef {
    public long transactionId;

    @NonNull
    public String tag = "";

    public TransactionTagCrossRef() {
    }

    @Ignore
    public TransactionTagCrossRef(long transactionId, String tag) {
        this.transactionId = transactionId;
        this.tag = tag == null ? "" : tag;
    }
}
