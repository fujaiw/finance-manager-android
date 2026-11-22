package com.example.financemanager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.financemanager.database.Transaction;
import com.example.financemanager.viewmodel.TransactionViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportFragment extends Fragment {
    private TextView tvExportResult;
    private TransactionViewModel viewModel;
    private List<Transaction> allTransactions;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export, container, false);

        Button btnExportToday = view.findViewById(R.id.btnExportToday);
        Button btnExportMonth = view.findViewById(R.id.btnExportMonth);
        Button btnExportAll = view.findViewById(R.id.btnExportAll);
        tvExportResult = view.findViewById(R.id.tvExportResult);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        btnExportToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportData("today");
            }
        });

        btnExportMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportData("month");
            }
        });

        btnExportAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportData("all");
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (getActivity() != null) {
            viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
            
            viewModel.getAllTransactions().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<List<Transaction>>() {
                @Override
                public void onChanged(List<Transaction> transactions) {
                    allTransactions = transactions;
                }
            });
        }
    }

    private void exportData(String type) {
        if (allTransactions == null || allTransactions.isEmpty()) {
            Toast.makeText(getContext(), "暂无数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Transaction> filteredTransactions = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String month = today.substring(0, 7);

        for (Transaction t : allTransactions) {
            if (t.isCanceled) continue;

            switch (type) {
                case "today":
                    if (t.dateTime.startsWith(today)) {
                        filteredTransactions.add(t);
                    }
                    break;
                case "month":
                    if (t.dateTime.startsWith(month)) {
                        filteredTransactions.add(t);
                    }
                    break;
                case "all":
                    filteredTransactions.add(t);
                    break;
            }
        }

        if (filteredTransactions.isEmpty()) {
            Toast.makeText(getContext(), "当前筛选条件下无数据", Toast.LENGTH_SHORT).show();
            return;
        }

        tvExportResult.setText("正在导出...");
        
        final List<Transaction> finalTransactions = filteredTransactions;
        final String exportType = type;
        
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ExcelExporter exporter = new ExcelExporter(getContext());
                    String filename = "finance_" + exportType + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".xlsx";
                    final String filePath = exporter.exportToExcel(finalTransactions, filename);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (filePath != null) {
                                int separator = filePath.lastIndexOf(java.io.File.separatorChar);
                                String savedName = separator >= 0 ? filePath.substring(separator + 1) : filePath;
                                tvExportResult.setText("导出成功!\n"
                                        + "保存位置: 下载/" + ExcelExporter.EXPORT_FOLDER + "\n"
                                        + "文件名: " + savedName + "\n"
                                        + "完整路径: " + filePath);
                                Toast.makeText(getContext(),
                                        "已保存到 下载/" + ExcelExporter.EXPORT_FOLDER,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                tvExportResult.setText("导出失败");
                                Toast.makeText(getContext(), "导出失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvExportResult.setText("导出失败: " + e.getMessage());
                            Toast.makeText(getContext(), "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
