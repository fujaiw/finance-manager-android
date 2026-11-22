package com.example.financemanager.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TransactionTemplate template);

    @Update
    void update(TransactionTemplate template);

    @Delete
    void delete(TransactionTemplate template);

    @Query("DELETE FROM transaction_templates WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM transaction_templates ORDER BY lastUsedAt DESC, createdAt DESC")
    LiveData<List<TransactionTemplate>> getAllTemplates();

    @Query("SELECT * FROM transaction_templates WHERE type = :type ORDER BY lastUsedAt DESC, createdAt DESC")
    LiveData<List<TransactionTemplate>> getTemplatesByType(String type);

    @Query("UPDATE transaction_templates SET lastUsedAt = :time WHERE id = :id")
    void updateLastUsedAt(int id, long time);
}
