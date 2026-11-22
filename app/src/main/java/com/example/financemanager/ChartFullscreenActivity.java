package com.example.financemanager;

import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.financemanager.database.AppDatabase;
import com.example.financemanager.database.TransactionDao;
import com.example.financemanager.model.BudgetManager;
import com.example.financemanager.util.AssetChartHelper;
import com.github.mikephil.charting.charts.LineChart;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全屏图表查看器：
 * 1) TYPE_ASSET_TREND：近一年资产趋势折线图，页面内部自行查库计算（无需调用方传数据）；
 * 2) 兼容旧模式：通过 EXTRA_CHART_DATA 传入图表 Bitmap 字节数组时直接展示图片。
 */
public class ChartFullscreenActivity extends AppCompatActivity {
    public static final String EXTRA_CHART_TYPE = "chart_type";
    public static final String EXTRA_CHART_DATA = "chart_data";
    public static final String TYPE_ASSET_TREND = "asset_trend";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        String chartType = getIntent().getStringExtra(EXTRA_CHART_TYPE);
        byte[] chartBytes = getIntent().getByteArrayExtra(EXTRA_CHART_DATA);

        if (TYPE_ASSET_TREND.equals(chartType)) {
            showAssetTrendChart();
        } else if (chartBytes != null && chartBytes.length > 0) {
            showBitmap(chartBytes);
        } else {
            // 无数据时展示空白页，点击关闭
            showBitmap(null);
        }
    }

    /**
     * 资产趋势全屏页：标题 + LineChart，后台线程完成 SQL 聚合与数据准备。
     */
    private void showAssetTrendChart() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        final TextView tvTitle = new TextView(this);
        tvTitle.setText("近一年资产趋势");
        tvTitle.setTextSize(16);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, pad / 2);
        root.addView(tvTitle);

        final LineChart lineChart = new LineChart(this);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        lineChart.setLayoutParams(chartParams);
        root.addView(lineChart);

        root.setOnClickListener(v -> finish());
        setContentView(root);

        // 后台线程：起始资金 + 总收支 + 近12个月快照（全部为 SQL 聚合）
        final AppDatabase database = AppDatabase.getInstance(getApplication());
        final BudgetManager budgetManager = new BudgetManager(getApplication());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                TransactionDao dao = database.transactionDao();
                double initial = budgetManager.getInitialAsset();
                double totalIncome = dao.getTotalIncome();
                double totalExpense = dao.getTotalExpense();
                final double currentAsset = initial + totalIncome - totalExpense;
                final List<TransactionDao.MonthlyAssetSnapshot> snapshots =
                        dao.getMonthlyAssetSnapshotsForYear();
                final List<AssetChartHelper.AssetPoint> points =
                        AssetChartHelper.buildTrendPoints(snapshots, currentAsset);

                runOnUiThread(() -> {
                    tvTitle.setText("近一年资产趋势（" + AssetChartHelper.buildRangeTitle(points)
                            + "）  当前资产 ¥" + String.format("%.2f", currentAsset));
                    AssetChartHelper.renderChart(lineChart, points, true);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        executor.shutdown();
    }

    /**
     * 旧模式：直接展示调用方传入的图表截图。
     */
    private void showBitmap(byte[] chartBytes) {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(0xFFFFFFFF);

        if (chartBytes != null && chartBytes.length > 0) {
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(chartBytes, 0, chartBytes.length));
        }

        imageView.setOnClickListener(v -> finish());

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xFFFFFFFF);
        frame.addView(imageView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(frame);
    }
}
