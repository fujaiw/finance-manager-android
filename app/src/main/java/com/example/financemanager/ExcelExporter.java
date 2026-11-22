package com.example.financemanager;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.financemanager.database.Transaction;
import com.example.financemanager.util.StoragePermissionUtil;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExcelExporter {

    /** 导出文件在公共「下载」目录下的子文件夹名 */
    public static final String EXPORT_FOLDER = "FinanceManager";

    private static final String MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private Context context;

    public ExcelExporter(Context context) {
        this.context = context;
    }

    /**
     * 导出Excel前的权限检查
     * Android 10+ 通过MediaStore写入公共下载目录，无需任何存储权限；
     * Android 9及以下需要传统的外部存储写入权限。
     * @return true表示有权限，false表示无权限
     */
    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return StoragePermissionUtil.hasStoragePermission(context);
    }

    /**
     * 请求存储权限（会跳转到系统设置页面，适配各厂商）
     */
    public void requestPermission() {
        StoragePermissionUtil.jumpToStorageSettings(context);
    }

    /**
     * 获取当前设备厂商信息
     */
    public String getDeviceInfo() {
        return StoragePermissionUtil.getManufacturerName();
    }

    public String exportToExcel(List<Transaction> transactions) {
        String fileName = "finance_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".xlsx";
        return exportToExcel(transactions, fileName);
    }

    public String exportToExcel(List<Transaction> transactions, String fileName) {
        // 权限检查（仅Android 9及以下需要传统存储权限）
        if (!checkPermission()) {
            requestPermission();
            return null;
        }

        Workbook workbook = null;
        try {
            workbook = buildWorkbook(transactions);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+：通过MediaStore保存到公共「下载/FinanceManager」目录
                return saveViaMediaStore(workbook, fileName);
            } else {
                // Android 9及以下：直接写入公共下载目录
                return saveToPublicDownloads(workbook, fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 构建导出用的Excel工作簿
     */
    private Workbook buildWorkbook(List<Transaction> transactions) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("交易记录");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"日期时间", "金额", "类型", "原因", "家庭账目", "二级分类", "作废标记"};
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm"));

        int rowNum = 1;
        for (Transaction transaction : transactions) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(transaction.dateTime);
            row.createCell(1).setCellValue(transaction.amount);
            row.createCell(2).setCellValue(transaction.category);
            row.createCell(3).setCellValue(transaction.reason != null ? transaction.reason : "");
            row.createCell(4).setCellValue(transaction.householdAccounts ? "是" : "否");
            row.createCell(5).setCellValue(transaction.subCategory != null ? transaction.subCategory : "");
            row.createCell(6).setCellValue(transaction.isCanceled ? "是" : "否");
        }

        // 在Android上避免使用autoSizeColumn，因为它会引用AWT类导致NoClassDefFoundError
        // 为列设置固定宽度以避免内容截断
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 20 * 256); // 约20字符宽度
        }
        // 第一列是毫秒级时间戳（23 字符），单独加宽以免显示被截断
        sheet.setColumnWidth(0, 25 * 256);

        return workbook;
    }

    /**
     * Android 10+ 使用MediaStore写入公共 下载/FinanceManager 目录。
     * 无需存储权限，写入完成后文件立即可在系统「下载」应用和文件管理器中看到。
     *
     * @return 文件的真实绝对路径
     */
    private String saveViaMediaStore(Workbook workbook, String fileName) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, MIME_XLSX);
        values.put(MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/" + EXPORT_FOLDER);
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        try {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("无法在下载目录中创建文件");
            }

            OutputStream os = resolver.openOutputStream(uri);
            if (os == null) {
                throw new IOException("无法获取文件输出流");
            }
            try {
                workbook.write(os);
                os.flush();
            } finally {
                os.close();
            }

            // 写入完成，解除待处理状态，文件对用户和其他应用可见
            ContentValues finishedValues = new ContentValues();
            finishedValues.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, finishedValues, null, null);

            return queryActualPath(resolver, uri, fileName);
        } catch (Exception e) {
            // 写入失败时清理残留的空记录
            if (uri != null) {
                try {
                    resolver.delete(uri, null, null);
                } catch (Exception ignored) {
                }
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e);
        }
    }

    /**
     * 查询MediaStore中文件的真实绝对路径，用于界面展示；查询失败时返回预期路径
     */
    private String queryActualPath(ContentResolver resolver, Uri uri, String fileName) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri,
                    new String[]{MediaStore.Downloads.DATA}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.Downloads.DATA);
                if (columnIndex >= 0) {
                    String path = cursor.getString(columnIndex);
                    if (path != null && !path.isEmpty()) {
                        return path;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        // 兜底：返回预期的公共下载路径
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                EXPORT_FOLDER + File.separator + fileName).getAbsolutePath();
    }

    /**
     * Android 9及以下：直接写入公共 下载/FinanceManager 目录（需WRITE_EXTERNAL_STORAGE权限）
     *
     * @return 文件的绝对路径
     */
    private String saveToPublicDownloads(Workbook workbook, String fileName) throws IOException {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File folder = new File(downloadsDir, EXPORT_FOLDER);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("无法创建目录: " + folder.getAbsolutePath());
        }

        File file = new File(folder, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.flush();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

        // 通知媒体库扫描，使文件在文件管理器中立即可见
        MediaScannerConnection.scanFile(context,
                new String[]{file.getAbsolutePath()}, new String[]{MIME_XLSX}, null);

        return file.getAbsolutePath();
    }
}
