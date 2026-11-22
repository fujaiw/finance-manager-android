package com.example.financemanager.util;

import android.graphics.Color;

import com.example.financemanager.database.TransactionDao;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 资产趋势图构建工具：将 DAO 的月度快照转换为近12个连续自然月的累计资产曲线。
 *
 * 基准校准：曲线末端必须等于"当前资产"（起始资金+全部历史净额），
 * 因此基准 = 当前资产 - 近12个月窗口内净额，再从早到晚逐月累加；
 * 窗口外的历史净额已包含在基准中，不会丢失。
 * 无任何交易时，12个点全部等于当前资产（即起始资金），显示为水平线。
 */
public final class AssetChartHelper {

    public static final int MONTH_COUNT = 12;
    private static final int LINE_COLOR = Color.parseColor("#4ECDC4");

    private AssetChartHelper() {
    }

    public static class AssetPoint {
        public final String month;     // yyyy-MM
        public final double asset;     // 当月末累计资产

        AssetPoint(String month, double asset) {
            this.month = month;
            this.asset = asset;
        }
    }

    /**
     * 根据月度快照与当前资产生成近12个月（含当月）的累计资产点，时间正序。
     */
    public static List<AssetPoint> buildTrendPoints(
            List<TransactionDao.MonthlyAssetSnapshot> snapshots, double currentAsset) {

        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

        // 最近12个自然月（含当月），时间正序
        List<String> months = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, -(MONTH_COUNT - 1));
        for (int i = 0; i < MONTH_COUNT; i++) {
            months.add(monthFormat.format(calendar.getTime()));
            calendar.add(Calendar.MONTH, 1);
        }

        // 快照转 Map，只保留窗口内的月份（查询为 DESC LIMIT 12，理论上都在窗口内）
        Map<String, TransactionDao.MonthlyAssetSnapshot> snapshotMap = new HashMap<>();
        if (snapshots != null) {
            for (TransactionDao.MonthlyAssetSnapshot s : snapshots) {
                if (s != null && s.month != null && months.contains(s.month)) {
                    snapshotMap.put(s.month, s);
                }
            }
        }

        // 窗口内净收支
        double windowNet = 0;
        for (String month : months) {
            TransactionDao.MonthlyAssetSnapshot s = snapshotMap.get(month);
            if (s != null) {
                windowNet += s.income - s.expense;
            }
        }

        // 以"当前资产"为锚点反推窗口起点基准，保证曲线末端 == 当前资产
        double cumulative = currentAsset - windowNet;
        List<AssetPoint> points = new ArrayList<>();
        for (String month : months) {
            TransactionDao.MonthlyAssetSnapshot s = snapshotMap.get(month);
            if (s != null) {
                cumulative += s.income - s.expense;
            }
            points.add(new AssetPoint(month, cumulative));
        }
        return points;
    }

    /**
     * 用累计资产点渲染折线图（主线程调用；数据准备可在后台完成）。
     *
     * @param fullscreen true=全屏展示时绘制数值标签，首页小图不绘制
     */
    public static void renderChart(LineChart chart, List<AssetPoint> points, boolean fullscreen) {
        if (chart == null || points == null || points.isEmpty()) {
            if (chart != null) {
                chart.clear();
            }
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            AssetPoint p = points.get(i);
            entries.add(new Entry(i, (float) p.asset));
            // X 轴只显示 MM，避免12个 yyyy-MM 标签重叠
            labels.add(p.month.length() >= 7 ? p.month.substring(5) : p.month);
        }

        LineDataSet dataSet = new LineDataSet(entries, "资产趋势");
        dataSet.setColor(LINE_COLOR);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(LINE_COLOR);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(LINE_COLOR);
        dataSet.setFillAlpha(35);
        dataSet.setValueTextSize(fullscreen ? 10f : 0f);
        dataSet.setDrawValues(fullscreen);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setHighLightColor(Color.TRANSPARENT);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(Math.min(6, labels.size()), true);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#666666"));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#666666"));
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        chart.getAxisRight().setEnabled(false);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);
        chart.setExtraBottomOffset(fullscreen ? 8f : 4f);
        chart.animateX(300);
        chart.invalidate();
    }

    /**
     * 生成图表标题，如 "2025-11 至 2026-10"。
     */
    public static String buildRangeTitle(List<AssetPoint> points) {
        if (points == null || points.isEmpty()) {
            return "";
        }
        SimpleDateFormat titleFormat = new SimpleDateFormat("yyyy年MM月", Locale.getDefault());
        try {
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Date start = monthFormat.parse(points.get(0).month);
            Date end = monthFormat.parse(points.get(points.size() - 1).month);
            return titleFormat.format(start) + " 至 " + titleFormat.format(end);
        } catch (Exception e) {
            return points.get(0).month + " 至 " + points.get(points.size() - 1).month;
        }
    }
}
