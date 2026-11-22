package com.example.financemanager.database;

/**
 * 交易与标签的关联投影，用于列表页一次性加载全部关联，避免逐条查询。
 */
public class TagLink {
    public long transactionId;
    public String tag;
}
