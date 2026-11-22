package com.example.financemanager;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.financemanager.database.Transaction;
import com.example.financemanager.database.TransactionDao;
import com.example.financemanager.database.TagLink;
import com.example.financemanager.model.CategoryManager;
import com.example.financemanager.util.AssetChartHelper;
import com.example.financemanager.viewmodel.TransactionViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.github.mikephil.charting.charts.LineChart;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private TransactionViewModel viewModel;
    private LinearLayout layoutTodayTransactions, layoutMonthlyTransactions;
    private LinearLayout layoutSearchResults;
    private View layoutSearchPanel, layoutNormalPanel;
    private TextView tvTodayExpense, tvTodayIncome, tvTodayBalance;
    private TextView tvMonthlyExpense, tvMonthlyIncome, tvMonthlyBalance;
    private TextView tvTodayEmpty, tvMonthlyEmpty, tvSearchEmpty, tvSearchTitle;
    private TextView tvCurrentAsset, tvInitialAsset;
    private LineChart assetTrendChart;
    private Button btnFilter;
    private Button btnClearSearch;
    private SearchView searchView;
    private androidx.core.widget.NestedScrollView homeScrollView;
    private List<Transaction> allTransactions;
    /** transactionId -> 标签名集合，由全量关联 LiveData 维护，供列表渲染 */
    private final Map<Integer, List<String>> tagsByTransaction = new HashMap<>();

    /**
     * 单次渲染的最大条目数。明细区是 LinearLayout + NestedScrollView（非 RecyclerView），
     * 无上限地一次性 inflate 上千行会阻塞主线程数秒，直接触发 ANR。
     */
    private static final int MAX_RENDER_ITEMS = 200;

    /** 当前生效的筛选条件（关键词 + 高级筛选共用） */
    private TransactionViewModel.SearchParams currentParams;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    /** 用户正在操作 SearchView 时不要用代码 setQuery 反向触发查询 */
    private boolean internalQueryChange = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvTodayExpense = view.findViewById(R.id.tvTodayExpense);
        tvTodayIncome = view.findViewById(R.id.tvTodayIncome);
        tvTodayBalance = view.findViewById(R.id.tvTodayBalance);
        tvMonthlyExpense = view.findViewById(R.id.tvMonthlyExpense);
        tvMonthlyIncome = view.findViewById(R.id.tvMonthlyIncome);
        tvMonthlyBalance = view.findViewById(R.id.tvMonthlyBalance);
        layoutTodayTransactions = view.findViewById(R.id.layoutTodayTransactions);
        layoutMonthlyTransactions = view.findViewById(R.id.layoutMonthlyTransactions);
        tvTodayEmpty = view.findViewById(R.id.tvTodayEmpty);
        tvMonthlyEmpty = view.findViewById(R.id.tvMonthlyEmpty);

        layoutSearchPanel = view.findViewById(R.id.layoutSearchPanel);
        layoutNormalPanel = view.findViewById(R.id.layoutNormalPanel);
        layoutSearchResults = view.findViewById(R.id.layoutSearchResults);
        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty);
        tvSearchTitle = view.findViewById(R.id.tvSearchTitle);
        searchView = view.findViewById(R.id.searchView);
        btnFilter = view.findViewById(R.id.btnFilter);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);

        tvCurrentAsset = view.findViewById(R.id.tvCurrentAsset);
        tvInitialAsset = view.findViewById(R.id.tvInitialAsset);
        assetTrendChart = view.findViewById(R.id.assetTrendChart);
        homeScrollView = view.findViewById(R.id.homeScrollView);

        FloatingActionButton fabExpense = view.findViewById(R.id.fabExpense);
        FloatingActionButton fabIncome = view.findViewById(R.id.fabIncome);

        fabExpense.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showAddTransactionDialog("Expense");
            }
        });

        fabIncome.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showAddTransactionDialog("Income");
            }
        });

        setupSearchView();
        setupAssetArea(view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null) {
            viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

            viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactionList -> {
                allTransactions = transactionList;
                updateTransactionLists();
            });

            // 标签关联：变化时重建映射并刷新当前可见列表
            viewModel.getAllTagLinks().observe(getViewLifecycleOwner(), links -> {
                tagsByTransaction.clear();
                if (links != null) {
                    for (TagLink link : links) {
                        List<String> names = tagsByTransaction.get((int) link.transactionId);
                        if (names == null) {
                            names = new ArrayList<>();
                            tagsByTransaction.put((int) link.transactionId, names);
                        }
                        names.add(link.tag);
                    }
                }
                if (currentParams == null) {
                    updateTransactionLists();
                } else {
                    renderSearchResults(viewModel.getFilteredTransactions().getValue());
                }
            });

            // 筛选结果：未激活搜索时此 LiveData 即全量数据；激活时为数据库筛选结果
            viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactionList -> {
                if (currentParams != null) {
                    renderSearchResults(transactionList);
                }
            });

            viewModel.getTodayExpense().observe(getViewLifecycleOwner(), expense -> {
                tvTodayExpense.setText("¥" + String.format("%.2f", expense));
                updateTodayBalance();
            });

            viewModel.getTodayIncome().observe(getViewLifecycleOwner(), income -> {
                tvTodayIncome.setText("¥" + String.format("%.2f", income));
                updateTodayBalance();
            });

            viewModel.getMonthlyExpense().observe(getViewLifecycleOwner(), expense -> {
                tvMonthlyExpense.setText("¥" + String.format("%.2f", expense));
                updateMonthlyBalance();
            });

            viewModel.getMonthlyIncome().observe(getViewLifecycleOwner(), income -> {
                tvMonthlyIncome.setText("¥" + String.format("%.2f", income));
                updateMonthlyBalance();
            });

            // 资产总览
            viewModel.getCurrentAsset().observe(getViewLifecycleOwner(), this::renderCurrentAsset);
            viewModel.getInitialAsset().observe(getViewLifecycleOwner(), asset ->
                    tvInitialAsset.setText("¥" + String.format("%.2f", asset)));
            viewModel.getMonthlyAssetSnapshots().observe(getViewLifecycleOwner(), snapshots -> {
                Double asset = viewModel.getCurrentAsset().getValue();
                renderAssetTrend(snapshots, asset != null ? asset : 0.0);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadStatistics();
            viewModel.loadAssetData();
        }
    }

    // ==================== 搜索与高级筛选 ====================

    private void cancelPendingSearch() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                cancelPendingSearch();
                applyKeyword(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (internalQueryChange) return true;
                // 300ms 防抖，避免输入过程中频繁查询
                cancelPendingSearch();
                final String keyword = newText;
                searchRunnable = () -> applyKeyword(keyword);
                searchHandler.postDelayed(searchRunnable, 300);
                return true;
            }
        });

        // 高级筛选
        btnFilter.setOnClickListener(v -> showFilterDialog());

        // 清除全部搜索条件（关键词+高级筛选），恢复今日/本月明细
        btnClearSearch.setOnClickListener(v -> {
            cancelPendingSearch();
            internalQueryChange = true;
            searchView.setQuery("", false);
            internalQueryChange = false;
            exitSearchMode();
        });
    }

    /** 将关键词合并进当前条件并触发查询；关键词为空且无其他条件时退出搜索 */
    private void applyKeyword(String keyword) {
        String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();
        // 关键词未变化时不重复发起查询（SearchView 有重复回调的情况）
        String current = currentParams != null ? currentParams.keyword : null;
        if (kw == null ? current == null : kw.equals(current)) {
            return;
        }
        if (currentParams == null) {
            currentParams = new TransactionViewModel.SearchParams();
        }
        currentParams.keyword = kw;
        applyCurrentParams();
    }

    private void applyCurrentParams() {
        if (currentParams == null || !currentParams.hasAnyCondition()) {
            exitSearchMode();
            return;
        }
        viewModel.performSearch(currentParams);
        enterSearchMode();
    }

    private void enterSearchMode() {
        layoutSearchPanel.setVisibility(View.VISIBLE);
        layoutNormalPanel.setVisibility(View.GONE);
        btnFilter.setText(hasAdvancedFilter() ? "筛选●" : "筛选");
        homeScrollView.scrollTo(0, 0);
    }

    private void exitSearchMode() {
        currentParams = null;
        viewModel.clearSearch();
        layoutSearchPanel.setVisibility(View.GONE);
        layoutNormalPanel.setVisibility(View.VISIBLE);
        btnFilter.setText("筛选");
        layoutSearchResults.removeAllViews();
        homeScrollView.scrollTo(0, 0);
    }

    private boolean hasAdvancedFilter() {
        if (currentParams == null) return false;
        return currentParams.startDate != null || currentParams.endDate != null
                || currentParams.minAmount != null || currentParams.maxAmount != null
                || currentParams.category != null || currentParams.subCategory != null
                || currentParams.householdAccounts != null || currentParams.includeCanceled;
    }

    private void renderSearchResults(List<Transaction> results) {
        layoutSearchResults.removeAllViews();
        String prefix = currentParams != null && currentParams.tag != null
                ? "标签「" + currentParams.tag + "」的结果" : "搜索结果";
        if (results == null || results.isEmpty()) {
            tvSearchEmpty.setVisibility(View.VISIBLE);
            tvSearchTitle.setText(prefix + "（0 条）");
            return;
        }
        tvSearchEmpty.setVisibility(View.GONE);
        int total = results.size();
        int shown = Math.min(total, MAX_RENDER_ITEMS);
        tvSearchTitle.setText(total > shown
                ? prefix + "（共 " + total + " 条，仅显示前 " + shown + " 条）"
                : prefix + "（" + total + " 条）");
        for (int i = 0; i < shown; i++) {
            layoutSearchResults.addView(createTransactionItem(results.get(i)));
        }
    }

    /** 点击明细上的标签：按标签筛选（与已有关键词/高级筛选条件取交集） */
    private void applyTagFilter(String tag) {
        if (currentParams == null) {
            currentParams = new TransactionViewModel.SearchParams();
        }
        currentParams.tag = tag;
        applyCurrentParams();
    }

    /** 高级筛选对话框 */
    private void showFilterDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_search_filter, null);
        final EditText etStartDate = dialogView.findViewById(R.id.etStartDate);
        final EditText etEndDate = dialogView.findViewById(R.id.etEndDate);
        final EditText etMinAmount = dialogView.findViewById(R.id.etMinAmount);
        final EditText etMaxAmount = dialogView.findViewById(R.id.etMaxAmount);
        final Spinner spCategory = dialogView.findViewById(R.id.spinnerCategory);
        final Spinner spSubCategory = dialogView.findViewById(R.id.spinnerSubCategory);
        final Spinner spHousehold = dialogView.findViewById(R.id.spinnerHousehold);
        final CheckBox cbIncludeCanceled = dialogView.findViewById(R.id.cbIncludeCanceled);

        // 收支类型
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"全部类型", "支出", "收入"});
        spCategory.setAdapter(categoryAdapter);

        // 二级分类：不限 + 全部支出/收入分类名
        List<String> subNames = new ArrayList<>();
        subNames.add("全部分类");
        for (String name : CategoryManager.getExpenseCategories()) subNames.add(name);
        for (String name : CategoryManager.getIncomeCategories()) subNames.add(name);
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, subNames);
        spSubCategory.setAdapter(subAdapter);

        ArrayAdapter<String> householdAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"全部账户", "仅家庭账目", "仅个人账目"});
        spHousehold.setAdapter(householdAdapter);

        // 回填当前条件（endDate 查询时补到当天 23:59，回填时只取日期部分）
        TransactionViewModel.SearchParams p = currentParams != null
                ? currentParams : new TransactionViewModel.SearchParams();
        if (p.startDate != null) etStartDate.setText(p.startDate);
        if (p.endDate != null) {
            etEndDate.setText(p.endDate.length() >= 10 ? p.endDate.substring(0, 10) : p.endDate);
        }
        if (p.minAmount != null) etMinAmount.setText(String.format(Locale.getDefault(), "%s", p.minAmount));
        if (p.maxAmount != null) etMaxAmount.setText(String.format(Locale.getDefault(), "%s", p.maxAmount));
        if ("Expense".equals(p.category)) spCategory.setSelection(1);
        else if ("Income".equals(p.category)) spCategory.setSelection(2);
        if (p.subCategory != null) {
            int idx = subNames.indexOf(p.subCategory);
            if (idx >= 0) spSubCategory.setSelection(idx);
        }
        if (Boolean.TRUE.equals(p.householdAccounts)) spHousehold.setSelection(1);
        else if (Boolean.FALSE.equals(p.householdAccounts)) spHousehold.setSelection(2);
        cbIncludeCanceled.setChecked(p.includeCanceled);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("应用", null)
                .setNegativeButton("取消", null)
                .setNeutralButton("重置", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String startDate = parseDateInput(etStartDate);
                String endDate = parseDateInput(etEndDate);
                // parseDateInput：null=未填写，空串=格式非法
                if ("".equals(startDate) || "".equals(endDate)) {
                    Toast.makeText(getContext(), "日期格式应为 yyyy-MM-dd", Toast.LENGTH_SHORT).show();
                    return;
                }
                Double minAmount = parseAmountInput(etMinAmount);
                Double maxAmount = parseAmountInput(etMaxAmount);
                if (minAmount == null || minAmount.isNaN() || maxAmount == null || maxAmount.isNaN()) {
                    Toast.makeText(getContext(), "金额格式不正确", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
                    Toast.makeText(getContext(), "最小金额不能大于最大金额", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (startDate != null && endDate != null && startDate.compareTo(endDate) > 0) {
                    Toast.makeText(getContext(), "开始日期不能晚于结束日期", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentParams == null) {
                    currentParams = new TransactionViewModel.SearchParams();
                }
                currentParams.startDate = startDate;
                // dateTime 精度到毫秒，结束日期补到当天最后一毫秒，避免漏掉结束当天的记录
                currentParams.endDate = endDate != null ? endDate + " 23:59:59.999" : null;
                currentParams.minAmount = minAmount;
                currentParams.maxAmount = maxAmount;
                currentParams.category = spCategory.getSelectedItemPosition() == 1 ? "Expense"
                        : spCategory.getSelectedItemPosition() == 2 ? "Income" : null;
                int subPos = spSubCategory.getSelectedItemPosition();
                currentParams.subCategory = subPos == 0 ? null : subNames.get(subPos);
                currentParams.householdAccounts = spHousehold.getSelectedItemPosition() == 1 ? Boolean.TRUE
                        : spHousehold.getSelectedItemPosition() == 2 ? Boolean.FALSE : null;
                currentParams.includeCanceled = cbIncludeCanceled.isChecked();

                applyCurrentParams();
                dialog.dismiss();
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                etStartDate.setText("");
                etEndDate.setText("");
                etMinAmount.setText("");
                etMaxAmount.setText("");
                spCategory.setSelection(0);
                spSubCategory.setSelection(0);
                spHousehold.setSelection(0);
                cbIncludeCanceled.setChecked(false);
            });
        });
        dialog.show();
    }

    /** 返回 yyyy-MM-dd；空输入返回 null（不限）；格式非法返回特殊标记由调用方判断——这里非法返回空串标记 */
    private String parseDateInput(EditText et) {
        String raw = et.getText().toString().trim();
        if (raw.isEmpty()) return null;
        return raw.matches("\\d{4}-\\d{2}-\\d{2}") ? raw : "";
    }

    private Double parseAmountInput(EditText et) {
        String raw = et.getText().toString().trim();
        if (raw.isEmpty()) return null;
        try {
            double v = Double.parseDouble(raw);
            return v >= 0 ? v : Double.NaN;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    // ==================== 资产总览 ====================

    private void setupAssetArea(View view) {
        // 点击"起始资金"区域修改起始资金
        view.findViewById(R.id.layoutInitialAsset).setOnClickListener(v -> showInitialAssetDialog());

        // 全屏查看资产趋势
        View.OnClickListener fullscreenListener = v -> {
            if (getContext() == null) return;
            startActivity(new Intent(getContext(), ChartFullscreenActivity.class)
                    .putExtra(ChartFullscreenActivity.EXTRA_CHART_TYPE,
                            ChartFullscreenActivity.TYPE_ASSET_TREND));
        };
        view.findViewById(R.id.layoutAssetCard).setOnClickListener(fullscreenListener);
        view.findViewById(R.id.btnAssetFullscreen).setOnClickListener(fullscreenListener);
    }

    private void showInitialAssetDialog() {
        if (getContext() == null) return;
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("请输入起始资金（不小于 0）");
        Double current = viewModel.getInitialAsset().getValue();
        if (current != null && current > 0) {
            input.setText(String.format(Locale.getDefault(), "%.2f", current));
        }
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(getContext())
                .setTitle("设置起始资金")
                .setView(input)
                .setPositiveButton("保存", (d, w) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) {
                        Toast.makeText(getContext(), "请输入金额", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double amount = Double.parseDouble(raw);
                        if (amount < 0) {
                            Toast.makeText(getContext(), "起始资金不能为负数", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        viewModel.setInitialAsset(amount);
                        Toast.makeText(getContext(), "起始资金已更新", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "金额格式不正确", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void renderCurrentAsset(Double asset) {
        double value = asset != null ? asset : 0.0;
        tvCurrentAsset.setText("¥" + String.format("%.2f", value));
        // 资产变化时同步重绘趋势图（曲线末端锚定为当前资产）
        List<TransactionDao.MonthlyAssetSnapshot> snapshots =
                viewModel.getMonthlyAssetSnapshots().getValue();
        renderAssetTrend(snapshots, value);
    }

    private void renderAssetTrend(List<TransactionDao.MonthlyAssetSnapshot> snapshots, double currentAsset) {
        if (assetTrendChart == null) return;
        List<AssetChartHelper.AssetPoint> points =
                AssetChartHelper.buildTrendPoints(snapshots, currentAsset);
        AssetChartHelper.renderChart(assetTrendChart, points, false);
    }

    // ==================== 常规明细 ====================

    private void updateTransactionLists() {
        if (allTransactions == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String month = today.substring(0, 7);

        List<Transaction> todayList = new ArrayList<>();
        List<Transaction> monthlyList = new ArrayList<>();

        for (Transaction t : allTransactions) {
            if (t.isCanceled) continue;

            if (t.dateTime.startsWith(today)) {
                todayList.add(t);
            }
            if (t.dateTime.startsWith(month)) {
                monthlyList.add(t);
            }
        }

        layoutTodayTransactions.removeAllViews();
        if (todayList.isEmpty()) {
            layoutTodayTransactions.addView(tvTodayEmpty);
            tvTodayEmpty.setVisibility(View.VISIBLE);
        } else {
            tvTodayEmpty.setVisibility(View.GONE);
            int shown = Math.min(todayList.size(), MAX_RENDER_ITEMS);
            for (int i = 0; i < shown; i++) {
                layoutTodayTransactions.addView(createTransactionItem(todayList.get(i)));
            }
            addTruncationHint(layoutTodayTransactions, shown, todayList.size());
        }

        layoutMonthlyTransactions.removeAllViews();
        if (monthlyList.isEmpty()) {
            layoutMonthlyTransactions.addView(tvMonthlyEmpty);
            tvMonthlyEmpty.setVisibility(View.VISIBLE);
        } else {
            tvMonthlyEmpty.setVisibility(View.GONE);
            int shown = Math.min(monthlyList.size(), MAX_RENDER_ITEMS);
            for (int i = 0; i < shown; i++) {
                layoutMonthlyTransactions.addView(createTransactionItem(monthlyList.get(i)));
            }
            addTruncationHint(layoutMonthlyTransactions, shown, monthlyList.size());
        }
    }

    /** 列表被截断时追加一行提示，引导用户用筛选缩小范围 */
    private void addTruncationHint(LinearLayout container, int shown, int total) {
        if (total <= shown || getContext() == null) return;
        TextView hint = new TextView(getContext());
        hint.setText("仅显示前 " + shown + " 条，共 " + total + " 条，可用筛选缩小范围");
        hint.setTextSize(12);
        hint.setTextColor(getResources().getColor(R.color.gray));
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        hint.setPadding(0, pad, 0, pad);
        container.addView(hint);
    }

    /**
     * 把 yyyy-MM-dd HH:mm[:ss.SSS] 显示为 MM-dd HH:mm。
     * 纯字符串截取，不做日期解析，避免大列表渲染时的性能开销。
     */
    private String formatItemDate(String dateTime) {
        if (dateTime == null) return "";
        if (dateTime.length() >= 16) return dateTime.substring(5, 16);
        return dateTime;
    }

    private View createTransactionItem(Transaction transaction) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View itemView = inflater.inflate(R.layout.item_transaction, null);

        TextView tvCategory = itemView.findViewById(R.id.tvCategory);
        TextView tvReason = itemView.findViewById(R.id.tvReason);
        TextView tvAmount = itemView.findViewById(R.id.tvAmount);
        TextView tvDate = itemView.findViewById(R.id.tvDate);

        String category = transaction.subCategory != null ? transaction.subCategory : "未分类";
        tvCategory.setText(transaction.isCanceled ? category + "（已作废）" : category);
        tvReason.setText(transaction.reason);

        String amount = transaction.isExpense() ? "-¥" + String.format("%.2f", transaction.amount)
                : "+¥" + String.format("%.2f", transaction.amount);
        tvAmount.setText(amount);

        // 已作废记录：置灰 + 金额删除线，与正常记录明显区分
        if (transaction.isCanceled) {
            tvCategory.setTextColor(getResources().getColor(R.color.gray));
            tvReason.setTextColor(getResources().getColor(R.color.gray));
            tvAmount.setTextColor(getResources().getColor(R.color.gray));
            tvAmount.setPaintFlags(tvAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvDate.setTextColor(getResources().getColor(R.color.gray));
        } else {
            tvAmount.setTextColor(transaction.isExpense()
                    ? getResources().getColor(R.color.expense_color)
                    : getResources().getColor(R.color.income_color));
        }

        // dateTime 形如 yyyy-MM-dd HH:mm:ss.SSS，直接截取 "MM-dd HH:mm"，
        // 避免每行创建两个 SimpleDateFormat 做解析（大列表下这是主要耗时点之一）
        tvDate.setText(formatItemDate(transaction.dateTime));

        // 标签小 Chip：点击可按该标签筛选
        LinearLayout layoutItemTags = itemView.findViewById(R.id.layoutItemTags);
        layoutItemTags.removeAllViews();
        List<String> tagNames = tagsByTransaction.get(transaction.id);
        if (tagNames != null && !tagNames.isEmpty()) {
            float density = getResources().getDisplayMetrics().density;
            for (String tagName : tagNames) {
                TextView chip = new TextView(getContext());
                chip.setText("# " + tagName);
                chip.setTextSize(10);
                chip.setTextColor(0xFF00897B);
                chip.setBackgroundResource(R.drawable.bg_tag_chip);
                int padH = (int) (8 * density);
                int padV = (int) (2 * density);
                chip.setPadding(padH, padV, padH, padV);
                LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                chipParams.rightMargin = (int) (6 * density);
                chip.setLayoutParams(chipParams);
                chip.setOnClickListener(v -> applyTagFilter(tagName));
                layoutItemTags.addView(chip);
            }
            itemView.findViewById(R.id.tagScroll).setVisibility(View.VISIBLE);
        } else {
            itemView.findViewById(R.id.tagScroll).setVisibility(View.GONE);
        }

        // 长按：作废 / 取消作废
        itemView.setOnLongClickListener(v -> {
            showTransactionActionDialog(transaction);
            return true;
        });
        itemView.setLongClickable(true);

        return itemView;
    }

    /**
     * 长按单条记录：根据当前状态提供“作废”或“取消作废”。
     * 作废后记录不计入统计、不出现在今日/本月列表；
     * 需要查看/恢复时，可在首页“筛选”中勾选“包含已作废记录”后再次长按取消作废。
     */
    private void showTransactionActionDialog(final Transaction transaction) {
        if (getContext() == null) return;

        final boolean canceled = transaction.isCanceled;
        String title = (transaction.subCategory != null ? transaction.subCategory : "记录")
                + "\n" + transaction.dateTime + "  ¥" + String.format(Locale.getDefault(), "%.2f", transaction.amount);

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setItems(canceled ? new String[]{"取消作废（恢复正常）"}
                                : new String[]{"作废此记录"},
                        (dialog, which) -> {
                            transaction.isCanceled = !canceled;
                            viewModel.update(transaction);
                            if (canceled) {
                                Toast.makeText(getContext(), "已取消作废，记录恢复正常", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(),
                                        "已作废，不再计入统计（可在筛选中勾选包含已作废记录后恢复）",
                                        Toast.LENGTH_LONG).show();
                            }
                        })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void updateTodayBalance() {
        double expense = viewModel.getTodayExpense().getValue() != null ? viewModel.getTodayExpense().getValue() : 0;
        double income = viewModel.getTodayIncome().getValue() != null ? viewModel.getTodayIncome().getValue() : 0;
        double balance = income - expense;
        tvTodayBalance.setText("¥" + String.format("%.2f", balance));
    }

    private void updateMonthlyBalance() {
        double expense = viewModel.getMonthlyExpense().getValue() != null ? viewModel.getMonthlyExpense().getValue() : 0;
        double income = viewModel.getMonthlyIncome().getValue() != null ? viewModel.getMonthlyIncome().getValue() : 0;
        double balance = income - expense;
        tvMonthlyBalance.setText("¥" + String.format("%.2f", balance));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelPendingSearch();
    }
}
