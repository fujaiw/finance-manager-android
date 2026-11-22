package com.example.financemanager.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertTag(TransactionTag tag);

    @Query("SELECT * FROM transaction_tags ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<TransactionTag>> getAllTags();

    @Query("SELECT name FROM transaction_tags ORDER BY name COLLATE NOCASE ASC")
    List<String> getAllTagNamesSync();

    @Query("SELECT tag FROM transaction_tag_cross_ref WHERE transactionId = :transactionId ORDER BY tag")
    LiveData<List<String>> getTagsForTransaction(long transactionId);

    /** 全量关联，列表渲染时在内存中按 transactionId 分组，避免 N 次查询 */
    @Query("SELECT transactionId, tag FROM transaction_tag_cross_ref ORDER BY tag")
    LiveData<List<TagLink>> getAllTagLinks();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertRefs(List<TransactionTagCrossRef> refs);

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transactionId = :transactionId")
    void deleteRefsOfTransaction(long transactionId);

    /** 删除标签同时清理所有交易上的该标签关联 */
    @Query("DELETE FROM transaction_tag_cross_ref WHERE tag = :name")
    void deleteRefsOfTag(String name);

    @Query("DELETE FROM transaction_tags WHERE name = :name")
    void deleteTag(String name);

    /** 标签改名（关联表同步改名，保持关联不丢失） */
    @Query("UPDATE transaction_tags SET name = :newName WHERE name = :oldName")
    void renameTag(String oldName, String newName);

    @Query("UPDATE transaction_tag_cross_ref SET tag = :newName WHERE tag = :oldName")
    void renameRefs(String oldName, String newName);

    @Query("SELECT EXISTS(SELECT 1 FROM transaction_tags WHERE name = :name)")
    boolean tagExists(String name);
}
