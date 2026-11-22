package com.example.financemanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.financemanager.database.CategoryLevel;
import com.example.financemanager.database.TagLink;
import com.example.financemanager.database.Transaction;
import com.example.financemanager.database.TransactionDao;
import com.example.financemanager.database.TransactionTag;
import com.example.financemanager.database.TransactionTemplate;
import com.example.financemanager.model.BudgetManager;
import com.example.financemanager.repository.TransactionRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionViewModel extends AndroidViewModel {
    public static final int CHART_MODE_MONTHLY = 0;
    public static final int CHART_MODE_YEARLY = 1;

    /** 异常消费判定：历史样本最少笔数 */
    public static final int ANOMALY_MIN_SAMPLES = 3;
    /** 异常消费判定：达到历史均值的倍数 */
    public static final double ANOMALY_MULTIPLIER = 3.0;

    private TransactionRepository repository;
    private BudgetManager budgetManager;
    private LiveData<List<Transaction>> allTransactions;

    // ---------- 搜索与高级筛选 ----------
    /** 当前生效的筛选条件；null 表示未激活搜索，首页展示常规的今日/本月明细 */
    private MutableLiveData<SearchParams> searchParamsLive = new MutableLiveData<>();

    /**
     * 筛选结果随 searchParamsLive 切换：未激活时直接观察全量数据。
     * 使用 switchMap 保证对外暴露的 LiveData 实例稳定，UI 只需 observe 一次。
     */
    private LiveData<List<Transaction>> filteredTransactions =
            Transformations.switchMap(searchParamsLive, params -> {
                if (params == null) {
                    return allTransactions;
                }
                return repository.searchTransactions(params.keyword, params.startDate, params.endDate,
                        params.minAmount, params.maxAmount, params.category, params.subCategory,
                        params.householdAccounts, params.tag, params.includeCanceled);
            });
    private MutableLiveData<Double> todayExpense = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> todayIncome = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> monthlyExpense = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> monthlyIncome = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> yearlyExpense = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> yearlyIncome = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> totalExpense = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> totalIncome = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> householdExpense = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> householdIncome = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> personalExpense = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> personalIncome = new MutableLiveData<>(0.0);
    private MutableLiveData<List<TransactionDao.MonthlyStats>> monthlyStats = new MutableLiveData<>();
    private MutableLiveData<List<TransactionDao.CategoryStats>> expenseCategoryStats = new MutableLiveData<>();
    private MutableLiveData<List<TransactionDao.CategoryStats>> incomeCategoryStats = new MutableLiveData<>();

    // ---------- 资产总览 ----------
    private MutableLiveData<Double> currentAsset = new MutableLiveData<>(0.0);
    private MutableLiveData<Double> initialAsset = new MutableLiveData<>(0.0);
    private MutableLiveData<List<TransactionDao.MonthlyAssetSnapshot>> monthlyAssetSnapshots =
            new MutableLiveData<>();

    private int chartMode = CHART_MODE_MONTHLY;

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
        budgetManager = new BudgetManager(application);
        allTransactions = repository.getAllTransactions();
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    // ---------- 搜索与高级筛选 ----------

    /** 触发组合查询；params 为 null 等价于清除搜索 */
    public void performSearch(SearchParams params) {
        searchParamsLive.setValue(params);
    }

    /** 清除搜索条件，恢复首页常规展示 */
    public void clearSearch() {
        searchParamsLive.setValue(null);
    }

    public LiveData<SearchParams> getSearchParams() {
        return searchParamsLive;
    }

    public LiveData<List<Transaction>> getFilteredTransactions() {
        return filteredTransactions;
    }

    /**
     * 搜索/筛选条件集合。字段为 null 表示该条件不参与过滤。
     */
    public static class SearchParams {
        public String keyword;
        public String startDate;        // yyyy-MM-dd
        public String endDate;          // yyyy-MM-dd
        public Double minAmount;
        public Double maxAmount;
        public String category;         // Expense / Income / null
        public String subCategory;
        public Boolean householdAccounts; // true=仅家庭 false=仅个人 null=不限
        /** 按单个标签筛选（明细页点击标签时使用），可与其他条件同时生效 */
        public String tag;
        public boolean includeCanceled;

        /** 是否存在任意激活的条件 */
        public boolean hasAnyCondition() {
            return keyword != null || startDate != null || endDate != null
                    || minAmount != null || maxAmount != null
                    || category != null || subCategory != null
                    || householdAccounts != null || tag != null || includeCanceled;
        }
    }

    // ---------- 资产总览 ----------

    public LiveData<Double> getCurrentAsset() {
        return currentAsset;
    }

    public LiveData<Double> getInitialAsset() {
        return initialAsset;
    }

    public LiveData<List<TransactionDao.MonthlyAssetSnapshot>> getMonthlyAssetSnapshots() {
        return monthlyAssetSnapshots;
    }

    /**
     * 加载资产数据：当前资产 = 起始资金 + 总收入 - 总支出（SQL 聚合，后台线程）。
     * 同时加载近一年月度快照供趋势图使用。
     */
    public void loadAssetData() {
        final double initial = budgetManager.getInitialAsset();
        initialAsset.postValue(initial);

        final double[] totalIncome = new double[1];
        final double[] totalExpense = new double[1];
        final java.util.concurrent.atomic.AtomicInteger pending =
                new java.util.concurrent.atomic.AtomicInteger(2);

        repository.getTotalIncomeAsync(result -> {
            totalIncome[0] = result != null ? result : 0.0;
            if (pending.decrementAndGet() == 0) {
                currentAsset.postValue(initial + totalIncome[0] - totalExpense[0]);
            }
        });
        repository.getTotalExpenseAsync(result -> {
            totalExpense[0] = result != null ? result : 0.0;
            if (pending.decrementAndGet() == 0) {
                currentAsset.postValue(initial + totalIncome[0] - totalExpense[0]);
            }
        });
        repository.getMonthlyAssetSnapshotsAsync(result ->
                monthlyAssetSnapshots.postValue(result));
    }

    /**
     * 设置起始资金（存入 BudgetSettings SharedPreferences）并立即刷新资产数据。
     */
    public void setInitialAsset(double amount) {
        budgetManager.setInitialAsset(amount);
        loadAssetData();
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction);
    }

    /** 导入 Excel 用：按毫秒级时间戳去重后插入 */
    public void importWithDedup(List<Transaction> transactions, TransactionRepository.ImportCallback callback) {
        repository.importWithDedup(transactions, callback);
    }

    /** 插入交易并关联标签 */
    public void insertWithTags(Transaction transaction, List<String> tagNames) {
        repository.insertWithTags(transaction, tagNames, null);
    }

    // ---------- 记账模板 ----------

    public LiveData<List<TransactionTemplate>> getAllTemplates() {
        return repository.getAllTemplates();
    }

    public void saveTemplate(TransactionTemplate template) {
        long now = System.currentTimeMillis();
        if (template.createdAt == 0) template.createdAt = now;
        template.lastUsedAt = now;
        repository.insertTemplate(template, null);
    }

    public void updateTemplate(TransactionTemplate template) {
        repository.updateTemplate(template);
    }

    public void deleteTemplateById(int id) {
        repository.deleteTemplateById(id);
    }

    public void markTemplateUsed(int id) {
        repository.markTemplateUsed(id);
    }

    // ---------- 标签 ----------

    public LiveData<List<TransactionTag>> getAllTags() {
        return repository.getAllTags();
    }

    public LiveData<List<TagLink>> getAllTagLinks() {
        return repository.getAllTagLinks();
    }

    public void createTag(String name, final RepositoryBooleanCallback callback) {
        repository.createTagAsync(name, ok -> {
            if (callback != null) callback.onResult(ok);
        });
    }

    public void renameTag(String oldName, String newName, final RepositoryBooleanCallback callback) {
        repository.renameTag(oldName, newName, ok -> {
            if (callback != null) callback.onResult(ok);
        });
    }

    public void deleteTag(String name) {
        repository.deleteTag(name);
    }

    public interface RepositoryBooleanCallback {
        void onResult(boolean success);
    }

    // ---------- 消费异常提醒 ----------

    /** 异常检测结果 */
    public static class AnomalyInfo {
        public boolean anomaly;
        public double avgAmount;
        public int count;
        public double multiplier;
    }

    /**
     * 检测一笔支出是否显著高于其二级分类的历史均值。
     * 开关关闭、样本不足（<3笔）时一律不提醒。
     */
    public void checkExpenseAnomaly(final String subCategory, final double amount,
                                    final RepositoryCallback<AnomalyInfo> callback) {
        final AnomalyInfo info = new AnomalyInfo();
        info.multiplier = ANOMALY_MULTIPLIER;
        if (!budgetManager.isAnomalyAlertEnabled()) {
            callback.onResult(info);
            return;
        }
        repository.getCategoryLevelAsync(subCategory, new TransactionRepository.DataCallback<CategoryLevel>() {
            @Override
            public void onResult(CategoryLevel level) {
                if (level != null && level.count >= ANOMALY_MIN_SAMPLES && level.avgAmount > 0
                        && amount >= level.avgAmount * ANOMALY_MULTIPLIER) {
                    info.anomaly = true;
                    info.avgAmount = level.avgAmount;
                    info.count = level.count;
                }
                callback.onResult(info);
            }
        });
    }

    public boolean isAnomalyAlertEnabled() {
        return budgetManager.isAnomalyAlertEnabled();
    }

    public void setAnomalyAlertEnabled(boolean enabled) {
        budgetManager.setAnomalyAlertEnabled(enabled);
    }

    /** 通用结果回调（主线程 postValue 的调用方自行切线程） */
    public interface RepositoryCallback<T> {
        void onResult(T result);
    }

    public void update(Transaction transaction) {
        repository.update(transaction);
    }

    public void deleteById(int id) {
        repository.deleteById(id);
    }

    public LiveData<Double> getTodayExpense() {
        return todayExpense;
    }

    public LiveData<Double> getTodayIncome() {
        return todayIncome;
    }

    public LiveData<Double> getMonthlyExpense() {
        return monthlyExpense;
    }

    public LiveData<Double> getMonthlyIncome() {
        return monthlyIncome;
    }

    public LiveData<Double> getYearlyExpense() {
        return yearlyExpense;
    }

    public LiveData<Double> getYearlyIncome() {
        return yearlyIncome;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getHouseholdExpense() {
        return householdExpense;
    }

    public LiveData<Double> getHouseholdIncome() {
        return householdIncome;
    }

    public LiveData<Double> getPersonalExpense() {
        return personalExpense;
    }

    public LiveData<Double> getPersonalIncome() {
        return personalIncome;
    }

    public LiveData<List<TransactionDao.MonthlyStats>> getMonthlyStats() {
        return monthlyStats;
    }

    public LiveData<List<TransactionDao.CategoryStats>> getExpenseCategoryStats() {
        return expenseCategoryStats;
    }

    public LiveData<List<TransactionDao.CategoryStats>> getIncomeCategoryStats() {
        return incomeCategoryStats;
    }

    public int getChartMode() {
        return chartMode;
    }

    public void setChartMode(int chartMode) {
        this.chartMode = chartMode;
    }

    public void loadStatistics() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String todayPattern = today + "%";
        String monthPattern = today.substring(0, 7) + "%";
        String yearPattern = today.substring(0, 4) + "%";

        repository.getTodayExpenseAsync(todayPattern, result -> todayExpense.postValue(result));
        repository.getTodayIncomeAsync(todayPattern, result -> todayIncome.postValue(result));
        repository.getMonthlyExpenseAsync(monthPattern, result -> monthlyExpense.postValue(result));
        repository.getMonthlyIncomeAsync(monthPattern, result -> monthlyIncome.postValue(result));
        repository.getYearExpenseAsync(yearPattern, result -> yearlyExpense.postValue(result));
        repository.getYearIncomeAsync(yearPattern, result -> yearlyIncome.postValue(result));
        repository.getTotalExpenseAsync(result -> totalExpense.postValue(result));
        repository.getTotalIncomeAsync(result -> totalIncome.postValue(result));
        repository.getHouseholdExpenseAsync(result -> householdExpense.postValue(result));
        repository.getHouseholdIncomeAsync(result -> householdIncome.postValue(result));
        repository.getPersonalExpenseAsync(result -> personalExpense.postValue(result));
        repository.getPersonalIncomeAsync(result -> personalIncome.postValue(result));
        repository.getMonthlyStatsAsync(result -> monthlyStats.postValue(result));

        String categoryPattern = chartMode == CHART_MODE_MONTHLY ? monthPattern : yearPattern;
        repository.getCategoryStatsByDateAsync("Expense", categoryPattern, result -> expenseCategoryStats.postValue(result));
        repository.getCategoryStatsByDateAsync("Income", categoryPattern, result -> incomeCategoryStats.postValue(result));
    }
}
