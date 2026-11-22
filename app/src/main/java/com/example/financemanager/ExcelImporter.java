package com.example.financemanager;

import android.content.Context;
import android.net.Uri;

import com.example.financemanager.database.Transaction;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExcelImporter {
    private Context context;
    private Map<String, String> categoryMapping = new HashMap<>();
    private Map<String, String> typeMapping = new HashMap<>();
    /** 本次导入中，每个分钟级时间戳已分配的毫秒序号 */
    private Map<String, Integer> importMillisSeq = new HashMap<>();

    public ExcelImporter(Context context) {
        this.context = context;
        initMapping();
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

    public ImportResult importFromUri(Uri uri) {
        importMillisSeq.clear();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return new ImportResult(false, "无法打开文件", 0, 0);
            }

            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            List<Transaction> transactions = new ArrayList<>();
            int totalRows = 0;
            int successCount = 0;

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRows++;

                if (i == 0) continue;

                Transaction transaction = parseRow(row);
                if (transaction != null) {
                    transactions.add(transaction);
                    successCount++;
                }
            }

            workbook.close();
            inputStream.close();

            return new ImportResult(true, "成功导入 " + successCount + " 条记录",
                    totalRows - 1, successCount);

        } catch (Exception e) {
            e.printStackTrace();
            return new ImportResult(false, "导入失败: " + e.getMessage(), 0, 0);
        }
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
                amount = Double.parseDouble(amountStr.replace(",", "").replace("¥", ""));
            } catch (NumberFormatException e) {
                try {
                    amount = Double.parseDouble(amountStr);
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
                        householdAccountsStr.equalsIgnoreCase("true") ||
                        householdAccountsStr.equalsIgnoreCase("yes") ||
                        householdAccountsStr.equals("1");
            }

            boolean isCanceled = false;
            if (isCanceledStr != null) {
                isCanceled = isCanceledStr.contains("是") ||
                        isCanceledStr.equalsIgnoreCase("true") ||
                        isCanceledStr.equalsIgnoreCase("yes") ||
                        isCanceledStr.equals("1");
            }

            String mappedCategory = categoryMapping.get(subCategory);
            if (mappedCategory == null) {
                mappedCategory = subCategory;
            }

            if (mappedCategory == null || mappedCategory.isEmpty()) {
                mappedCategory = "其他";
            }

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

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue();
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                java.util.Date date = cell.getDateCellValue();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
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

    public static class ImportResult {
        public boolean success;
        public String message;
        public int totalRows;
        public int successCount;

        public ImportResult(boolean success, String message, int totalRows, int successCount) {
            this.success = success;
            this.message = message;
            this.totalRows = totalRows;
            this.successCount = successCount;
        }
    }
}
