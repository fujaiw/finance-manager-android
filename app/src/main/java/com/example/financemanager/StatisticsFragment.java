package com.example.financemanager;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.financemanager.database.TransactionDao;
import com.example.financemanager.model.BudgetManager;
import com.example.financemanager.viewmodel.TransactionViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StatisticsFragment extends Fragment {
    private TransactionViewModel viewModel;
    private BudgetManager budgetManager;
    private TextView tvTotalExpense, tvTotalIncome, tvYearlyExpense, tvYearlyIncome;
    private TextView tvHouseholdExpense, tvPersonalExpense, tvEmpty;
    private TextView tvBudget, tvBudgetUsed, tvBudgetRemaining, tvBudgetPercent;
    private TextView tvDailyBudget, tvRemainingDays;
    private ProgressBar progressBudget;
    private LineChart lineChart;
    private BarChart barChartComparison;
    private PieChart pieChartExpense, pieChartIncome;
    private Button btnExpenseFullscreen, btnIncomeFullscreen;

    private List<TransactionDao.MonthlyStats> monthlyStatsData;
    private List<TransactionDao.CategoryStats> expenseCategoryData;
    private List<TransactionDao.CategoryStats> incomeCategoryData;

    private int[] colors = {
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1"),
            Color.parseColor("#96CEB4"),
            Color.parseColor("#FFEAA7"),
            Color.parseColor("#DDA0DD"),
            Color.parseColor("#98D8C8"),
            Color.parseColor("#F7DC6F"),
            Color.parseColor("#A29BFE"),
            Color.parseColor("#FD79A8")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvYearlyExpense = view.findViewById(R.id.tvYearlyExpense);
        tvYearlyIncome = view.findViewById(R.id.tvYearlyIncome);
        tvHouseholdExpense = view.findViewById(R.id.tvHouseholdExpense);
        tvPersonalExpense = view.findViewById(R.id.tvPersonalExpense);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvBudget = view.findViewById(R.id.tvBudget);
        tvBudgetUsed = view.findViewById(R.id.tvBudgetUsed);
        tvBudgetRemaining = view.findViewById(R.id.tvBudgetRemaining);
        tvBudgetPercent = view.findViewById(R.id.tvBudgetPercent);
        tvDailyBudget = view.findViewById(R.id.tvDailyBudget);
        tvRemainingDays = view.findViewById(R.id.tvRemainingDays);
        progressBudget = view.findViewById(R.id.progressBudget);
        lineChart = view.findViewById(R.id.lineChart);
        barChartComparison = view.findViewById(R.id.barChartComparison);
        pieChartExpense = view.findViewById(R.id.pieChartExpense);
        pieChartIncome = view.findViewById(R.id.pieChartIncome);
        btnExpenseFullscreen = view.findViewById(R.id.btnExpenseFullscreen);
        btnIncomeFullscreen = view.findViewById(R.id.btnIncomeFullscreen);

        setupCharts();
        setupChartClickListeners();

        if (getContext() != null) {
            budgetManager = new BudgetManager(getContext());
        }

        return view;
    }

    private void setupChartClickListeners() {
        lineChart.setOnClickListener(v -> showFullscreenLineChart());
        lineChart.setOnLongClickListener(v -> {
            showFullscreenLineChart();
            return true;
        });

        pieChartExpense.setOnClickListener(v -> showFullscreenPieChart(expenseCategoryData, "支出分类"));
        pieChartExpense.setOnLongClickListener(v -> {
            showFullscreenPieChart(expenseCategoryData, "支出分类");
            return true;
        });

        pieChartIncome.setOnClickListener(v -> showFullscreenPieChart(incomeCategoryData, "收入分类"));
        pieChartIncome.setOnLongClickListener(v -> {
            showFullscreenPieChart(incomeCategoryData, "收入分类");
            return true;
        });

        btnExpenseFullscreen.setOnClickListener(v -> showFullscreenPieChart(expenseCategoryData, "支出分类"));
        btnIncomeFullscreen.setOnClickListener(v -> showFullscreenPieChart(incomeCategoryData, "收入分类"));
    }

    private void showFullscreenLineChart() {
        if (monthlyStatsData == null || monthlyStatsData.isEmpty()) return;

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chart_fullscreen);

        LineChart fullscreenChart = dialog.findViewById(R.id.fullscreenLineChart);
        fullscreenChart.setVisibility(View.VISIBLE);

        setupFullscreenLineChart(fullscreenChart);

        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupFullscreenLineChart(LineChart chart) {
        Description description = new Description();
        description.setEnabled(false);
        chart.setDescription(description);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(16f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextSize(16f);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setSpaceTop(20f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(16f);

        List<Entry> expenseEntries = new ArrayList<>();
        List<Entry> incomeEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (TransactionDao.MonthlyStats stats : monthlyStatsData) {
            expenseEntries.add(new Entry(index, (float) stats.expense));
            incomeEntries.add(new Entry(index, (float) stats.income));
            labels.add(stats.month.substring(2));
            index++;
        }

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "支出");
        expenseSet.setColor(Color.parseColor("#FF6B6B"));
        expenseSet.setLineWidth(4f);
        expenseSet.setCircleColor(Color.parseColor("#FF6B6B"));
        expenseSet.setCircleRadius(8f);
        expenseSet.setValueTextSize(14f);
        expenseSet.setDrawValues(true);
        expenseSet.setValueFormatter(new ValueFormatter() {
            private DecimalFormat format = new DecimalFormat("¥#0.00");
            @Override
            public String getFormattedValue(float value) {
                return format.format(value);
            }
        });

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "收入");
        incomeSet.setColor(Color.parseColor("#4ECDC4"));
        incomeSet.setLineWidth(4f);
        incomeSet.setCircleColor(Color.parseColor("#4ECDC4"));
        incomeSet.setCircleRadius(8f);
        incomeSet.setValueTextSize(14f);
        incomeSet.setDrawValues(true);
        incomeSet.setValueFormatter(new ValueFormatter() {
            private DecimalFormat format = new DecimalFormat("¥#0.00");
            @Override
            public String getFormattedValue(float value) {
                return format.format(value);
            }
        });

        LineData data = new LineData(expenseSet, incomeSet);
        chart.setData(data);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.invalidate();
    }

    private void showFullscreenPieChart(List<TransactionDao.CategoryStats> categoryStats, String title) {
        if (categoryStats == null || categoryStats.isEmpty()) return;

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chart_fullscreen);

        PieChart fullscreenChart = dialog.findViewById(R.id.fullscreenPieChart);
        fullscreenChart.setVisibility(View.VISIBLE);

        setupFullscreenPieChart(fullscreenChart, categoryStats, title);

        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupFullscreenPieChart(PieChart chart, List<TransactionDao.CategoryStats> categoryStats, String title) {
        Description description = new Description();
        description.setEnabled(false);
        chart.setDescription(description);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(45f);
        chart.setCenterTextSize(22f);
        chart.setCenterTextColor(Color.DKGRAY);
        chart.setUsePercentValues(false);
        chart.setExtraBottomOffset(80f);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(18f);
        legend.setTextColor(Color.DKGRAY);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        List<PieEntry> entries = new ArrayList<>();
        double total = 0;

        for (TransactionDao.CategoryStats stats : categoryStats) {
            double percentage = (stats.total / getTotal(categoryStats)) * 100;
            if (percentage >= 1) {
                entries.add(new PieEntry((float) stats.total, stats.subCategory));
            }
            total += stats.total;
        }

        PieDataSet dataSet = new PieDataSet(entries, title);
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextSize(18f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setValueLineColor(Color.DKGRAY);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(chart));

        chart.setData(data);
        chart.setCenterText(title + "\n¥" + String.format("%.2f", total));
        chart.invalidate();
    }

    private void setupCharts() {
        setupLineChart();
        setupBarChart();
        setupPieChart(pieChartExpense);
        setupPieChart(pieChartIncome);
    }

    private void setupBarChart() {
        Description description = new Description();
        description.setEnabled(false);
        barChartComparison.setDescription(description);
        barChartComparison.setTouchEnabled(true);
        barChartComparison.setDragEnabled(true);
        barChartComparison.setScaleEnabled(false);
        barChartComparison.setPinchZoom(false);
        barChartComparison.setDrawGridBackground(false);
        barChartComparison.setExtraBottomOffset(10f);

        XAxis xAxis = barChartComparison.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);

        YAxis leftAxis = barChartComparison.getAxisLeft();
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = barChartComparison.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = barChartComparison.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        legend.setWordWrapEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
    }

    private void setupLineChart() {
        Description description = new Description();
        description.setEnabled(false);
        lineChart.setDescription(description);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setExtraRightOffset(20f);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setSpaceMin(0.5f);
        xAxis.setSpaceMax(0.5f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setSpaceTop(15f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(10f);
        legend.setWordWrapEnabled(true);
    }

    private void setupPieChart(PieChart pieChart) {
        Description description = new Description();
        description.setEnabled(false);
        pieChart.setDescription(description);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(38f);
        pieChart.setTransparentCircleRadius(42f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.DKGRAY);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.setUsePercentValues(false);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        legend.setTextColor(Color.DKGRAY);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setXEntrySpace(8f);
        legend.setYEntrySpace(4f);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null) {
            viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int chartMode = prefs.getInt("chart_mode", TransactionViewModel.CHART_MODE_MONTHLY);
            viewModel.setChartMode(chartMode);

            viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
                if (transactions.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    return;
                }
                tvEmpty.setVisibility(View.GONE);
            });

            viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense ->
                    tvTotalExpense.setText("¥" + String.format("%.2f", expense)));

            viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income ->
                    tvTotalIncome.setText("¥" + String.format("%.2f", income)));

            viewModel.getYearlyExpense().observe(getViewLifecycleOwner(), expense ->
                    tvYearlyExpense.setText("¥" + String.format("%.2f", expense)));

            viewModel.getYearlyIncome().observe(getViewLifecycleOwner(), income ->
                    tvYearlyIncome.setText("¥" + String.format("%.2f", income)));

            viewModel.getHouseholdExpense().observe(getViewLifecycleOwner(), expense -> {
                tvHouseholdExpense.setText("¥" + String.format("%.2f", expense));
                updateBarChart();
            });

            viewModel.getPersonalExpense().observe(getViewLifecycleOwner(), expense -> {
                tvPersonalExpense.setText("¥" + String.format("%.2f", expense));
                updateBarChart();
            });

            viewModel.getHouseholdIncome().observe(getViewLifecycleOwner(), income -> updateBarChart());
            viewModel.getPersonalIncome().observe(getViewLifecycleOwner(), income -> updateBarChart());

            viewModel.getMonthlyExpense().observe(getViewLifecycleOwner(), this::updateBudgetDisplay);

            viewModel.getMonthlyStats().observe(getViewLifecycleOwner(), stats -> {
                monthlyStatsData = stats;
                updateLineChart(stats);
            });

            viewModel.getExpenseCategoryStats().observe(getViewLifecycleOwner(), stats -> {
                expenseCategoryData = stats;
                updatePieChart(pieChartExpense, stats, "支出");
            });

            viewModel.getIncomeCategoryStats().observe(getViewLifecycleOwner(), stats -> {
                incomeCategoryData = stats;
                updatePieChart(pieChartIncome, stats, "收入");
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadStatistics();
        }
        updateBudgetDisplay(viewModel != null ? viewModel.getMonthlyExpense().getValue() : 0);
    }

    private void updateBudgetDisplay(Double monthlyExpense) {
        if (budgetManager == null) return;

        double spent = monthlyExpense != null ? monthlyExpense : 0;
        double budget = budgetManager.getMonthlyBudget();
        double remaining = budgetManager.getRemainingBudget(spent);
        double percent = budgetManager.getBudgetUsagePercent(spent);
        double dailyBudget = budgetManager.getDailyRemainingBudget(spent);
        int remainingDays = (int) budgetManager.getRemainingDays();

        tvBudget.setText("¥" + String.format("%.2f", budget));
        tvBudgetUsed.setText("¥" + String.format("%.2f", spent));
        tvBudgetRemaining.setText("¥" + String.format("%.2f", remaining));
        tvBudgetPercent.setText(String.format("%.0f%%", percent));
        tvDailyBudget.setText("¥" + String.format("%.2f", dailyBudget));
        tvRemainingDays.setText(remainingDays + "天");

        int progress = (int) Math.min(percent, 100);
        progressBudget.setProgress(progress);

        int colorRes = percent >= 100 ? Color.parseColor("#FF6B6B") :
                       percent >= 80 ? Color.parseColor("#F39C12") : Color.parseColor("#4ECDC4");
        progressBudget.getProgressDrawable().setColorFilter(colorRes, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void updateBarChart() {
        if (viewModel == null) return;

        Double householdExpense = viewModel.getHouseholdExpense().getValue();
        Double personalExpense = viewModel.getPersonalExpense().getValue();
        Double householdIncome = viewModel.getHouseholdIncome().getValue();
        Double personalIncome = viewModel.getPersonalIncome().getValue();

        if (householdExpense == null) householdExpense = 0.0;
        if (personalExpense == null) personalExpense = 0.0;
        if (householdIncome == null) householdIncome = 0.0;
        if (personalIncome == null) personalIncome = 0.0;

        List<BarEntry> expenseEntries = new ArrayList<>();
        List<BarEntry> incomeEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        labels.add("个人账");
        labels.add("家庭账");

        // 个人账：支出在左，收入在右
        expenseEntries.add(new BarEntry(0, personalExpense.floatValue()));
        incomeEntries.add(new BarEntry(0, personalIncome.floatValue()));

        // 家庭账：支出在左，收入在右
        expenseEntries.add(new BarEntry(1, householdExpense.floatValue()));
        incomeEntries.add(new BarEntry(1, householdIncome.floatValue()));

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "支出");
        expenseSet.setColor(Color.parseColor("#FF6B6B"));
        expenseSet.setValueTextSize(10f);
        expenseSet.setValueTextColor(Color.DKGRAY);
        expenseSet.setValueFormatter(new ValueFormatter() {
            private DecimalFormat format = new DecimalFormat("#0.00");
            @Override
            public String getFormattedValue(float value) {
                return "¥" + format.format(value);
            }
        });

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "收入");
        incomeSet.setColor(Color.parseColor("#4ECDC4"));
        incomeSet.setValueTextSize(10f);
        incomeSet.setValueTextColor(Color.DKGRAY);
        incomeSet.setValueFormatter(new ValueFormatter() {
            private DecimalFormat format = new DecimalFormat("#0.00");
            @Override
            public String getFormattedValue(float value) {
                return "¥" + format.format(value);
            }
        });

        BarData data = new BarData(expenseSet, incomeSet);
        data.setBarWidth(0.35f);

        barChartComparison.setData(data);
        barChartComparison.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartComparison.groupBars(0f, 0.3f, 0.05f);
        barChartComparison.invalidate();
    }

    private void updateLineChart(List<TransactionDao.MonthlyStats> monthlyStats) {
        if (monthlyStats == null || monthlyStats.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }

        List<Entry> expenseEntries = new ArrayList<>();
        List<Entry> incomeEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (TransactionDao.MonthlyStats stats : monthlyStats) {
            expenseEntries.add(new Entry(index, (float) stats.expense));
            incomeEntries.add(new Entry(index, (float) stats.income));
            labels.add(stats.month.substring(2));
            index++;
        }

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "支出");
        expenseSet.setColor(Color.parseColor("#FF6B6B"));
        expenseSet.setLineWidth(3f);
        expenseSet.setCircleColor(Color.parseColor("#FF6B6B"));
        expenseSet.setCircleRadius(5f);
        expenseSet.setFillAlpha(30);
        expenseSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        expenseSet.setValueTextSize(9f);
        expenseSet.setValueTextColor(Color.DKGRAY);
        expenseSet.setDrawValues(true);
        expenseSet.setValueFormatter(new ValueFormatter() {
            private DecimalFormat format = new DecimalFormat("#0.00");
            @Override
            public String getFormattedValue(float value) {
                return "¥" + format.format(value);
            }
        });

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "收入");
        incomeSet.setColor(Color.parseColor("#4ECDC4"));
        incomeSet.setLineWidth(3f);
        incomeSet.setCircleColor(Color.parseColor("#4ECDC4"));
        incomeSet.setCircleRadius(5f);
        incomeSet.setFillAlpha(30);
        incomeSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        incomeSet.setValueTextSize(9f);
        incomeSet.setValueTextColor(Color.DKGRAY);
        incomeSet.setDrawValues(true);
        incomeSet.setValueFormatter(new ValueFormatter() {
            private DecimalFormat format = new DecimalFormat("#0.00");
            @Override
            public String getFormattedValue(float value) {
                return "¥" + format.format(value);
            }
        });

        LineData data = new LineData(expenseSet, incomeSet);
        lineChart.setData(data);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        lineChart.invalidate();
    }

    private void updatePieChart(PieChart pieChart, List<TransactionDao.CategoryStats> categoryStats, String title) {
        if (categoryStats == null || categoryStats.isEmpty()) {
            pieChart.clear();
            pieChart.setCenterText(title + "\n暂无数据");
            pieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        double total = 0;

        for (TransactionDao.CategoryStats stats : categoryStats) {
            double percentage = (stats.total / getTotal(categoryStats)) * 100;
            if (percentage >= 1) {
                entries.add(new PieEntry((float) stats.total, stats.subCategory));
            }
            total += stats.total;
        }

        PieDataSet dataSet = new PieDataSet(entries, title);
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(8f);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setValueLineColor(Color.DKGRAY);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.DKGRAY);
        data.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.setCenterText(title + "\n¥" + String.format("%.2f", total));
        pieChart.invalidate();
    }

    private double getTotal(List<TransactionDao.CategoryStats> stats) {
        double total = 0;
        for (TransactionDao.CategoryStats s : stats) {
            total += s.total;
        }
        return total;
    }
}