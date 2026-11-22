package com.example.financemanager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.financemanager.database.Transaction;
import com.example.financemanager.model.CategoryManager;
import com.example.financemanager.viewmodel.TransactionViewModel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {
    private Button btnImportExcel;
    private Button btnClearData;
    private Button btnManageCategories;
    private Button btnChartModeMonthly;
    private Button btnChartModeYearly;
    private TextView tvImportResult;
    private ProgressBar progressBar;
    private TransactionViewModel viewModel;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private Map<String, String> categoryMapping = new HashMap<>();
    private Map<String, String> typeMapping = new HashMap<>();
    /** 本次导入中，每个分钟级时间戳已分配的毫秒序号 */
    private Map<String, Integer> importMillisSeq = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initMapping();

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            importExcelFile(uri);
                        }
                    }
                }
        );
    }

    private void initMapping() {
        categoryMapping.put("餐饮", "餐饮");
        categoryMapping.put("交通", "交通");
        categoryMapping.put("购物", "购物");
        categoryMapping.put("房租", "房租");
        categoryMapping.put("日常", "日常");
        categoryMapping.put("娱乐", "娱乐");
        categoryMapping.put("教育", "教育");
        categoryMapping.put("医疗", "医疗");
        categoryMapping.put("理财亏损", "理财亏损");
        categoryMapping.put("工资", "工资");
        categoryMapping.put("兼职", "兼职");
        categoryMapping.put("理财", "理财");
        categoryMapping.put("红包", "红包");
        categoryMapping.put("奖金", "奖金");
        categoryMapping.put("其他", "其他");

        typeMapping.put("支出", "Expense");
        typeMapping.put("收入", "Income");
        typeMapping.put("Expense", "Expense");
        typeMapping.put("Income", "Income");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null) {
            viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        }

        btnImportExcel = getView().findViewById(R.id.btnImportExcel);
        btnClearData = getView().findViewById(R.id.btnClearData);
        btnManageCategories = getView().findViewById(R.id.btnManageCategories);
        btnChartModeMonthly = getView().findViewById(R.id.btnChartModeMonthly);
        btnChartModeYearly = getView().findViewById(R.id.btnChartModeYearly);
        tvImportResult = getView().findViewById(R.id.tvImportResult);

        btnImportExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDataDialog();
            }
        });

        btnManageCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CategoryManagerActivity.class);
                startActivity(intent);
            }
        });

        Button btnManageTemplates = getView().findViewById(R.id.btnManageTemplates);
        btnManageTemplates.setOnClickListener(v ->
                startActivity(new Intent(getContext(), TemplateManagerActivity.class)));

        Button btnManageTags = getView().findViewById(R.id.btnManageTags);
        btnManageTags.setOnClickListener(v ->
                startActivity(new Intent(getContext(), TagManagerActivity.class)));

        SwitchCompat swAnomalyAlert = getView().findViewById(R.id.swAnomalyAlert);
        swAnomalyAlert.setChecked(viewModel.isAnomalyAlertEnabled());
        swAnomalyAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setAnomalyAlertEnabled(isChecked);
            Toast.makeText(getContext(),
                    isChecked ? "已开启消费异常提醒" : "已关闭消费异常提醒",
                    Toast.LENGTH_SHORT).show();
        });

        setupChartModeButtons();

        // 软件内版本号与 build.gradle 的 versionName 保持一致（动态读取，无需手改两处）
        TextView tvVersionName = getView().findViewById(R.id.tvVersionName);
        try {
            android.content.pm.PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            long versionCode = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P
                    ? info.getLongVersionCode() : info.versionCode;
            tvVersionName.setText("v" + info.versionName + " (" + versionCode + ")");
        } catch (Exception e) {
            tvVersionName.setText("v1.2.0");
        }
    }

    private void setupChartModeButtons() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int chartMode = prefs.getInt("chart_mode", 0);

        updateChartModeButtonStyle(chartMode);

        btnChartModeMonthly.setOnClickListener(v -> {
            saveChartMode(0);
            updateChartModeButtonStyle(0);
            Toast.makeText(getContext(), "饼图已切换为当月数据", Toast.LENGTH_SHORT).show();
        });

        btnChartModeYearly.setOnClickListener(v -> {
            saveChartMode(1);
            updateChartModeButtonStyle(1);
            Toast.makeText(getContext(), "饼图已切换为当年数据", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveChartMode(int mode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("chart_mode", mode);
        editor.apply();
    }

    private void updateChartModeButtonStyle(int mode) {
        if (mode == 0) {
            btnChartModeMonthly.setBackgroundTintList(getResources().getColorStateList(R.color.purple_500));
            btnChartModeMonthly.setTextColor(getResources().getColor(R.color.white));
            btnChartModeYearly.setBackgroundTintList(getResources().getColorStateList(R.color.grey_300));
            btnChartModeYearly.setTextColor(getResources().getColor(R.color.black));
        } else {
            btnChartModeYearly.setBackgroundTintList(getResources().getColorStateList(R.color.purple_500));
            btnChartModeYearly.setTextColor(getResources().getColor(R.color.white));
            btnChartModeMonthly.setBackgroundTintList(getResources().getColorStateList(R.color.grey_300));
            btnChartModeMonthly.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private void importExcelFile(Uri uri) {
        if (getContext() == null || viewModel == null) return;

        progressBar = getView().findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        tvImportResult.setText("正在导入...");
        importMillisSeq.clear();

        WeakReference<SettingsFragment> weakReference = new WeakReference<>(this);

        executor.execute(() -> {
            SettingsFragment fragment = weakReference.get();
            if (fragment == null || fragment.getContext() == null) return;

            try {
                InputStream inputStream = fragment.getContext().getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    mainHandler.post(() -> {
                        if (fragment.tvImportResult != null) {
                            fragment.tvImportResult.setText("无法打开文件");
                        }
                        if (fragment.progressBar != null) {
                            fragment.progressBar.setVisibility(View.GONE);
                        }
                    });
                    return;
                }

                Workbook workbook = WorkbookFactory.create(inputStream);
                Sheet sheet = workbook.getSheetAt(0);

                List<Transaction> transactions = new ArrayList<>();

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Transaction transaction = fragment.parseRow(row);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                }

                workbook.close();
                inputStream.close();

                // 按毫秒级时间戳去重后插入：库里已存在的、同一文件内重复的都会跳过
                viewModel.importWithDedup(transactions,
                        new com.example.financemanager.repository.TransactionRepository.ImportCallback() {
                            @Override
                            public void onResult(final int added, final int skipped) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String message = "新增 " + added + " 条，跳过重复 " + skipped + " 条";
                                        if (fragment.tvImportResult != null) {
                                            fragment.tvImportResult.setText(message);
                                        }
                                        if (fragment.progressBar != null) {
                                            fragment.progressBar.setVisibility(View.GONE);
                                        }
                                        Toast.makeText(fragment.getContext(), message, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });

            } catch (final Exception e) {
                e.printStackTrace();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (fragment.tvImportResult != null) {
                            fragment.tvImportResult.setText("导入失败: " + e.getMessage());
                        }
                        if (fragment.progressBar != null) {
                            fragment.progressBar.setVisibility(View.GONE);
                        }
                        Toast.makeText(fragment.getContext(), "导入失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 导入时间统一为毫秒级 YYYY-MM-DD HH:MM:SS.fff。
     * 分钟级旧数据按分钟分组依次补 .000 / .001 ...，保证同一分钟的多条记录时间戳不重复。
     */
    private String normalizeImportedDateTime(String raw) {
        if (raw.length() == 16) {
            int seq = importMillisSeq.containsKey(raw) ? importMillisSeq.get(raw) : 0;
            importMillisSeq.put(raw, seq + 1);
            return String.format(Locale.US, "%s:00.%03d", raw, seq);
        }
        if (raw.length() == 19) {
            return raw + ".000";
        }
        return raw;
    }

    private Transaction parseRow(Row row) {
        try {
            String dateTime = getCellStringValue(row.getCell(0));
            String amountStr = getCellStringValue(row.getCell(1));
            String type = getCellStringValue(row.getCell(2));
            String reason = getCellStringValue(row.getCell(3));
            String householdAccountsStr = getCellStringValue(row.getCell(4));
            String subCategory = getCellStringValue(row.getCell(5));
            String isCanceledStr = getCellStringValue(row.getCell(6));

            if (dateTime == null || amountStr == null || type == null) {
                return null;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr.replace(",", "").replace("¥", "").trim());
            } catch (NumberFormatException e) {
                try {
                    amount = Double.parseDouble(amountStr.trim());
                } catch (Exception ex) {
                    return null;
                }
            }

            String categoryType = typeMapping.get(type);
            if (categoryType == null) {
                if (type.contains("支出") || type.equals("Expense")) {
                    categoryType = "Expense";
                } else if (type.contains("收入") || type.equals("Income")) {
                    categoryType = "Income";
                } else {
                    return null;
                }
            }

            boolean householdAccounts = false;
            if (householdAccountsStr != null) {
                householdAccounts = householdAccountsStr.contains("是") ||
                        householdAccountsStr.equalsIgnoreCase("household_accounts") ||
                        householdAccountsStr.equalsIgnoreCase("true") ||
                        householdAccountsStr.equalsIgnoreCase("yes") ||
                        householdAccountsStr.equals("1");
            }

            boolean isCanceled = false;
            if (isCanceledStr != null) {
                isCanceled = isCanceledStr.contains("是") ||
                        isCanceledStr.equalsIgnoreCase("canceled") ||
                        isCanceledStr.equalsIgnoreCase("true") ||
                        isCanceledStr.equalsIgnoreCase("yes") ||
                        isCanceledStr.equals("1");
            }

            // 保留原文分类名，支持用户自定义分类；空值归"其他"
            String mappedCategory = (subCategory != null && !subCategory.isEmpty()) ? subCategory : "其他";

            dateTime = normalizeImportedDateTime(dateTime);

            Transaction transaction = new Transaction();
            transaction.dateTime = dateTime;
            transaction.amount = amount;
            transaction.category = categoryType;
            transaction.subCategory = mappedCategory;
            transaction.reason = (reason != null && !reason.isEmpty()) ? reason : "未填写";
            transaction.householdAccounts = householdAccounts;
            transaction.isCanceled = isCanceled;

            return transaction;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue();
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
                return sdf.format(date);
            } else {
                double value = cell.getNumericCellValue();
                if (value == (long) value) {
                    return String.valueOf((long) value);
                } else {
                    return String.valueOf(value);
                }
            }
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cell.getCellType() == CellType.FORMULA) {
            try {
                return String.valueOf(cell.getNumericCellValue());
            } catch (Exception e) {
                try {
                    return cell.getStringCellValue();
                } catch (Exception ex) {
                    return null;
                }
            }
        }

        return null;
    }

    private void showClearDataDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("确认清空");
        builder.setMessage("确定要清空所有数据吗？此操作不可恢复！");
        builder.setPositiveButton("确定", (dialog, which) -> {
            clearAllData();
        });
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }

    private void clearAllData() {
        if (viewModel == null || viewModel.getAllTransactions().getValue() == null) {
            Toast.makeText(getContext(), "没有数据可清空", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Transaction> transactions = viewModel.getAllTransactions().getValue();
        if (transactions != null) {
            for (Transaction transaction : transactions) {
                viewModel.deleteById(transaction.id);
            }
        }

        Toast.makeText(getContext(), "数据已清空", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
