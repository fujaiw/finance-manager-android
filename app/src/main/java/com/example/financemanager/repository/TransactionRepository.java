package com.example.financemanager.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.financemanager.database.AppDatabase;
import com.example.financemanager.database.CategoryLevel;
import com.example.financemanager.database.TagLink;
import com.example.financemanager.database.Transaction;
import com.example.financemanager.database.TransactionDao;
import com.example.financemanager.database.TransactionTag;
import com.example.financemanager.database.TransactionTagCrossRef;
import com.example.financemanager.database.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private TransactionDao transactionDao;
    private com.example.financemanager.database.TemplateDao templateDao;
    private com.example.financemanager.database.TagDao tagDao;
    private AppDatabase database;
    private LiveData<List<Transaction>> allTransactions;
    private ExecutorService executorService;

    public TransactionRepository(Application application) {
        database = AppDatabase.getInstance(application);
        transactionDao = database.transactionDao();
        templateDao = database.templateDao();
        tagDao = database.tagDao();
        allTransactions = transactionDao.getAllTransactions();
        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    /**
     * 多条件组合搜索，Room 自动在后台线程执行并随数据表变化自动更新。
     */
    public LiveData<List<Transaction>> searchTransactions(String keyword, String startDate, String endDate,
                                                           Double minAmount, Double maxAmount,
                                                           String category, String subCategory,
                                                           Boolean householdAccounts, String tag,
                                                           boolean includeCanceled) {
        return transactionDao.searchTransactions(keyword, startDate, endDate, minAmount, maxAmount,
                category, subCategory, householdAccounts, tag, includeCanceled);
    }

    public void insert(Transaction transaction) {
        executorService.execute(() -> transactionDao.insert(transaction));
    }

    /**
     * 导入外部交易：按毫秒级时间戳去重后插入（与 PC 端 import_transactions 语义一致）。
     * 库里已有的时间戳跳过，同一批文件内的重复时间戳也跳过。
     */
    public void importWithDedup(final List<Transaction> transactions, final ImportCallback callback) {
        executorService.execute(() -> {
            Set<String> existing = new HashSet<>(transactionDao.getAllDateTimes());
            int added = 0;
            int skipped = 0;
            for (Transaction transaction : transactions) {
                if (!existing.add(transaction.dateTime)) {
                    skipped++;
                    continue;
                }
                transactionDao.insert(transaction);
                added++;
            }
            if (callback != null) {
                callback.onResult(added, skipped);
            }
        });
    }

    /**
     * 插入一笔交易并关联标签（同一数据库事务内完成）。
     * @param tagNames 标签名集合（可为 null/空）；标签不存在时自动创建
     * @param callback 回填新生成的交易 id
     */
    public void insertWithTags(final Transaction transaction, final List<String> tagNames,
                               final DataCallback<Long> callback) {
        executorService.execute(() -> {
            final long[] idHolder = new long[1];
            database.runInTransaction(() -> {
                long id = transactionDao.insert(transaction);
                idHolder[0] = id;
                if (tagNames != null && !tagNames.isEmpty()) {
                    List<TransactionTagCrossRef> refs = new ArrayList<>();
                    for (String name : tagNames) {
                        String trimmed = name.trim();
                        if (trimmed.isEmpty()) continue;
                        tagDao.insertTag(new TransactionTag(trimmed));
                        refs.add(new TransactionTagCrossRef(id, trimmed));
                    }
                    if (!refs.isEmpty()) {
                        tagDao.insertRefs(refs);
                    }
                }
            });
            if (callback != null) {
                callback.onResult(idHolder[0]);
            }
        });
    }

    public void update(Transaction transaction) {
        executorService.execute(() -> transactionDao.update(transaction));
    }

    public void deleteById(int id) {
        // 关联表通过外键 ON DELETE CASCADE 自动清理
        executorService.execute(() -> transactionDao.deleteById(id));
    }

    // ==================== 异常消费 ====================

    public void getCategoryLevelAsync(String subCategory, DataCallback<CategoryLevel> callback) {
        executorService.execute(() -> callback.onResult(transactionDao.getCategoryLevel(subCategory)));
    }

    // ==================== 记账模板 ====================

    public LiveData<List<TransactionTemplate>> getAllTemplates() {
        return templateDao.getAllTemplates();
    }

    public void insertTemplate(final TransactionTemplate template, final DataCallback<Long> callback) {
        executorService.execute(() -> {
            long id = templateDao.insert(template);
            if (callback != null) callback.onResult(id);
        });
    }

    public void updateTemplate(TransactionTemplate template) {
        executorService.execute(() -> templateDao.update(template));
    }

    public void deleteTemplateById(int id) {
        executorService.execute(() -> templateDao.deleteById(id));
    }

    public void markTemplateUsed(final int id) {
        executorService.execute(() -> templateDao.updateLastUsedAt(id, System.currentTimeMillis()));
    }

    // ==================== 标签 ====================

    public LiveData<List<TransactionTag>> getAllTags() {
        return tagDao.getAllTags();
    }

    public LiveData<List<TagLink>> getAllTagLinks() {
        return tagDao.getAllTagLinks();
    }

    public void createTagAsync(final String name, final DataCallback<Boolean> callback) {
        executorService.execute(() -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                if (callback != null) callback.onResult(false);
                return;
            }
            if (tagDao.tagExists(trimmed)) {
                if (callback != null) callback.onResult(false);
                return;
            }
            tagDao.insertTag(new TransactionTag(trimmed));
            if (callback != null) callback.onResult(true);
        });
    }

    public void renameTag(final String oldName, final String newName, final DataCallback<Boolean> callback) {
        executorService.execute(() -> {
            String trimmed = newName.trim();
            boolean ok = !trimmed.isEmpty() && !trimmed.equals(oldName) && !tagDao.tagExists(trimmed);
            if (ok) {
                database.runInTransaction(() -> {
                    tagDao.renameTag(oldName, trimmed);
                    tagDao.renameRefs(oldName, trimmed);
                });
            }
            if (callback != null) callback.onResult(ok);
        });
    }

    public void deleteTag(final String name) {
        executorService.execute(() -> database.runInTransaction(() -> {
            tagDao.deleteRefsOfTag(name);
            tagDao.deleteTag(name);
        }));
    }

    // ==================== 统计相关（保持原有） ====================

    public void getTodayExpenseAsync(String datePattern, DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getExpenseByDate(datePattern);
            callback.onResult(result);
        });
    }

    public void getTodayIncomeAsync(String datePattern, DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getIncomeByDate(datePattern);
            callback.onResult(result);
        });
    }

    public void getMonthlyExpenseAsync(String datePattern, DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getExpenseByDate(datePattern);
            callback.onResult(result);
        });
    }

    public void getMonthlyIncomeAsync(String datePattern, DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getIncomeByDate(datePattern);
            callback.onResult(result);
        });
    }

    public void getYearExpenseAsync(String yearPattern, DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getYearExpense(yearPattern);
            callback.onResult(result);
        });
    }

    public void getYearIncomeAsync(String yearPattern, DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getYearIncome(yearPattern);
            callback.onResult(result);
        });
    }

    public void getTotalExpenseAsync(DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getTotalExpense();
            callback.onResult(result);
        });
    }

    public void getTotalIncomeAsync(DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getTotalIncome();
            callback.onResult(result);
        });
    }

    public void getHouseholdExpenseAsync(DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getHouseholdExpense();
            callback.onResult(result);
        });
    }

    public void getHouseholdIncomeAsync(DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getHouseholdIncome();
            callback.onResult(result);
        });
    }

    public void getPersonalExpenseAsync(DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getPersonalExpense();
            callback.onResult(result);
        });
    }

    public void getPersonalIncomeAsync(DataCallback<Double> callback) {
        executorService.execute(() -> {
            double result = transactionDao.getPersonalIncome();
            callback.onResult(result);
        });
    }

    public void getMonthlyStatsAsync(DataCallback<List<TransactionDao.MonthlyStats>> callback) {
        executorService.execute(() -> {
            List<TransactionDao.MonthlyStats> result = transactionDao.getMonthlyStats();
            callback.onResult(result);
        });
    }

    public void getCategoryStatsAsync(String categoryType, DataCallback<List<TransactionDao.CategoryStats>> callback) {
        executorService.execute(() -> {
            List<TransactionDao.CategoryStats> result = transactionDao.getCategoryStats(categoryType);
            callback.onResult(result);
        });
    }

    public void getCategoryStatsByDateAsync(String categoryType, String datePattern, DataCallback<List<TransactionDao.CategoryStats>> callback) {
        executorService.execute(() -> {
            List<TransactionDao.CategoryStats> result = transactionDao.getCategoryStatsByDate(categoryType, datePattern);
            callback.onResult(result);
        });
    }

    public void getYearlyStatsAsync(DataCallback<List<TransactionDao.YearlyStats>> callback) {
        executorService.execute(() -> {
            List<TransactionDao.YearlyStats> result = transactionDao.getYearlyStats();
            callback.onResult(result);
        });
    }

    public void getMonthlyAssetSnapshotsAsync(DataCallback<List<TransactionDao.MonthlyAssetSnapshot>> callback) {
        executorService.execute(() -> {
            List<TransactionDao.MonthlyAssetSnapshot> result = transactionDao.getMonthlyAssetSnapshotsForYear();
            callback.onResult(result);
        });
    }

    public interface DataCallback<T> {
        void onResult(T result);
    }

    /** 导入去重结果回调：added 实际新增条数，skipped 因时间戳重复跳过的条数 */
    public interface ImportCallback {
        void onResult(int added, int skipped);
    }
}
