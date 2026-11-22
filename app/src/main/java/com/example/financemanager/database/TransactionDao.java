package com.example.financemanager.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM transactions WHERE isCanceled = 0 ORDER BY dateTime DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE isCanceled = 0 ORDER BY dateTime DESC LIMIT :limit")
    LiveData<List<Transaction>> getRecentTransactions(int limit);

    @Query("SELECT * FROM transactions WHERE category = :category AND isCanceled = 0 ORDER BY dateTime DESC")
    LiveData<List<Transaction>> getTransactionsByCategory(String category);

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Expense' AND isCanceled = 0 AND dateTime LIKE :datePattern")
    double getExpenseByDate(String datePattern);

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Income' AND isCanceled = 0 AND dateTime LIKE :datePattern")
    double getIncomeByDate(String datePattern);

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Expense' AND isCanceled = 0")
    double getTotalExpense();

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Income' AND isCanceled = 0")
    double getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Expense' AND isCanceled = 0 AND householdAccounts = 1")
    double getHouseholdExpense();

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Income' AND isCanceled = 0 AND householdAccounts = 1")
    double getHouseholdIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Expense' AND isCanceled = 0 AND householdAccounts = 0")
    double getPersonalExpense();

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Income' AND isCanceled = 0 AND householdAccounts = 0")
    double getPersonalIncome();

    @Query("SELECT strftime('%Y-%m', dateTime) as month, SUM(CASE WHEN category = 'Expense' THEN amount ELSE 0 END) as expense, SUM(CASE WHEN category = 'Income' THEN amount ELSE 0 END) as income FROM transactions WHERE isCanceled = 0 GROUP BY strftime('%Y-%m', dateTime) ORDER BY month DESC")
    List<MonthlyStats> getMonthlyStats();

    @Query("SELECT subCategory, SUM(amount) as total FROM transactions WHERE category = :categoryType AND isCanceled = 0 GROUP BY subCategory ORDER BY total DESC")
    List<CategoryStats> getCategoryStats(String categoryType);

    @Query("SELECT subCategory, SUM(amount) as total FROM transactions WHERE category = :categoryType AND isCanceled = 0 AND dateTime LIKE :datePattern GROUP BY subCategory ORDER BY total DESC")
    List<CategoryStats> getCategoryStatsByDate(String categoryType, String datePattern);

    @Query("SELECT * FROM transactions WHERE isCanceled = 0 ORDER BY dateTime DESC")
    List<Transaction> getAllTransactionsSync();

    /** 仅返回所有时间戳，用于导入去重（对应 PC 端 get_all_timestamps，内存开销最小） */
    @Query("SELECT dateTime FROM transactions")
    List<String> getAllDateTimes();

    @Query("SELECT strftime('%Y', dateTime) as year, SUM(CASE WHEN category = 'Expense' THEN amount ELSE 0 END) as expense, SUM(CASE WHEN category = 'Income' THEN amount ELSE 0 END) as income FROM transactions WHERE isCanceled = 0 GROUP BY strftime('%Y', dateTime) ORDER BY year DESC")
    List<YearlyStats> getYearlyStats();

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Expense' AND isCanceled = 0 AND dateTime LIKE :yearPattern")
    double getYearExpense(String yearPattern);

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'Income' AND isCanceled = 0 AND dateTime LIKE :yearPattern")
    double getYearIncome(String yearPattern);

    /**
     * 多条件组合搜索。所有筛选条件均可为 null（布尔包装类型为 null 表示不限），
     * 通过 ":param IS NULL OR ..." 模式实现可选条件；作废记录默认排除。
     * tag 不为 null 时，通过 EXISTS 子查询与标签关联表组合过滤。
     */
    @Query("SELECT * FROM transactions WHERE " +
            "(:keyword IS NULL OR reason LIKE '%' || :keyword || '%') AND " +
            "(:startDate IS NULL OR dateTime >= :startDate) AND " +
            "(:endDate IS NULL OR dateTime <= :endDate) AND " +
            "(:minAmount IS NULL OR amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR amount <= :maxAmount) AND " +
            "(:category IS NULL OR category = :category) AND " +
            "(:subCategory IS NULL OR subCategory = :subCategory) AND " +
            "(:householdAccounts IS NULL OR householdAccounts = :householdAccounts) AND " +
            "(:tag IS NULL OR EXISTS (SELECT 1 FROM transaction_tag_cross_ref r " +
            "  WHERE r.transactionId = transactions.id AND r.tag = :tag)) AND " +
            "(:includeCanceled = 1 OR isCanceled = 0) " +
            "ORDER BY dateTime DESC")
    LiveData<List<Transaction>> searchTransactions(String keyword, String startDate, String endDate,
                                                   Double minAmount, Double maxAmount,
                                                   String category, String subCategory,
                                                   Boolean householdAccounts, String tag,
                                                   boolean includeCanceled);

    /**
     * 某二级支出分类的历史平均水平（全部非作废记录），用于异常消费提醒。
     */
    @Query("SELECT COUNT(*) as count, AVG(amount) as avgAmount FROM transactions " +
            "WHERE category = 'Expense' AND isCanceled = 0 AND subCategory = :subCategory")
    CategoryLevel getCategoryLevel(String subCategory);

    /**
     * 近一年按月聚合的收支快照（仅有交易的月份会出现），按月份倒序取最近12条。
     */
    @Query("SELECT strftime('%Y-%m', dateTime) as month, " +
            "SUM(CASE WHEN category = 'Income' THEN amount ELSE 0 END) as income, " +
            "SUM(CASE WHEN category = 'Expense' THEN amount ELSE 0 END) as expense " +
            "FROM transactions WHERE isCanceled = 0 " +
            "GROUP BY strftime('%Y-%m', dateTime) " +
            "ORDER BY month DESC LIMIT 12")
    List<MonthlyAssetSnapshot> getMonthlyAssetSnapshotsForYear();

    class MonthlyStats {
        public String month;
        public double expense;
        public double income;
    }

    class MonthlyAssetSnapshot {
        public String month;
        public double income;
        public double expense;
    }

    class CategoryStats {
        public String subCategory;
        public double total;
    }

    class YearlyStats {
        public String year;
        public double expense;
        public double income;
    }
}
