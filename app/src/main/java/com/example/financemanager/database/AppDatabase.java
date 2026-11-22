package com.example.financemanager.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Transaction.class, TransactionTemplate.class,
        TransactionTag.class, TransactionTagCrossRef.class},
        version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract TransactionDao transactionDao();
    public abstract TemplateDao templateDao();
    public abstract TagDao tagDao();

    /**
     * 仅支持 v3：不再提供 v1→v2、v2→v3 迁移。
     * 旧版本数据库（v1/v2）打开时会走 fallbackToDestructiveMigration 重建为空库，
     * 即放弃对旧版本数据的兼容。
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "finance_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
