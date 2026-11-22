package com.example.financemanager.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import android.Manifest;

import java.io.File;

/**
 * 全面适配各大厂商定制安卓系统存储权限
 * 支持：小米HyperOS、华为鸿蒙EMUI、OPPO ColorOS、vivo OriginOS、荣耀MagicOS、
 *       一加、realme、iQOO、魅族、三星OneUI等
 */
public class StoragePermissionUtil {

    /**
     * 厂商类型枚举
     */
    public enum Manufacturer {
        XIAOMI,      // 小米HyperOS/MIUI
        HUAWEI,      // 华为鸿蒙EMUI/HarmonyOS
        OPPO,        // OPPO ColorOS
        VIVO,        // vivo OriginOS
        HONOR,       // 荣耀MagicOS
        MEIZU,       // 魅族Flyme
        SAMSUNG,     // 三星OneUI
        ONEPLUS,     // 一加OxygenOS
        REALME,      // realme UI
        IQOO,        // iQOO UI
        GOOGLE,      // 原生Android
        OTHER        // 其他厂商
    }

    /**
     * 获取当前设备厂商类型
     */
    public static Manufacturer getManufacturer() {
        String brand = Build.BRAND.toLowerCase();
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        // 小米系（包括红米）
        if (brand.contains("xiaomi") || brand.contains("redmi") || manufacturer.contains("xiaomi")) {
            return Manufacturer.XIAOMI;
        }
        // 华为系
        if (brand.contains("huawei") || manufacturer.contains("huawei")) {
            return Manufacturer.HUAWEI;
        }
        // OPPO系
        if (brand.contains("oppo") || manufacturer.contains("oppo")) {
            return Manufacturer.OPPO;
        }
        // vivo系
        if (brand.contains("vivo") || manufacturer.contains("vivo")) {
            return Manufacturer.VIVO;
        }
        // 荣耀
        if (brand.contains("honor") || manufacturer.contains("honor")) {
            return Manufacturer.HONOR;
        }
        // 魅族
        if (brand.contains("meizu") || manufacturer.contains("meizu")) {
            return Manufacturer.MEIZU;
        }
        // 三星
        if (brand.contains("samsung") || manufacturer.contains("samsung")) {
            return Manufacturer.SAMSUNG;
        }
        // 一加
        if (brand.contains("oneplus") || manufacturer.contains("oneplus")) {
            return Manufacturer.ONEPLUS;
        }
        // realme
        if (brand.contains("realme") || manufacturer.contains("realme")) {
            return Manufacturer.REALME;
        }
        // iQOO
        if (brand.contains("iqoo") || manufacturer.contains("iqoo")) {
            return Manufacturer.IQOO;
        }
        // Google原生
        if (brand.contains("google") || manufacturer.contains("google")) {
            return Manufacturer.GOOGLE;
        }
        return Manufacturer.OTHER;
    }

    /**
     * 判断是否需要全盘文件管理权限（针对厂商定制系统）
     */
    public static boolean requiresAllFilesPermission() {
        Manufacturer mf = getManufacturer();
        
        // 以下厂商需要全盘权限
        switch (mf) {
            case XIAOMI:    // HyperOS内核沙箱强拦截
            case HUAWEI:    // 华为文件隔离
            case OPPO:      // 权限分级管控
            case VIVO:      // 权限分级管控
            case HONOR:     // MagicOS权限管控
            case IQOO:      // 同vivo系
            case REALME:    // 同OPPO系
            case ONEPLUS:   // 同OPPO系
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
            default:
                return false;
        }
    }

    /**
     * 判断是否拥有存储权限（兼容各厂商）
     */
    public static boolean hasStoragePermission(Context context) {
        // Android 11+ 检查全盘管理权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 厂商定制系统需要全盘权限
            if (requiresAllFilesPermission()) {
                return Environment.isExternalStorageManager();
            }
            // 原生系统使用Scoped Storage
            return true;
        } else {
            // Android 10及以下检查传统存储权限
            int writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            return writePermission == PackageManager.PERMISSION_GRANTED &&
                   readPermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 跳转到存储权限设置页面（适配各厂商）
     */
    public static void jumpToStorageSettings(Context context) {
        Manufacturer mf = getManufacturer();
        
        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ 全盘文件管理权限
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            } else {
                // Android 10及以下传统权限设置
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            }

            context.startActivity(intent);
        } catch (Exception e) {
            // 备用方案：通用应用设置页面
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ex) {
                // 最终备用
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    /**
     * 获取推荐的文件保存路径（适配各厂商存储策略）
     */
    public static String getRecommendedStoragePath(Context context, String folderName) {
        Manufacturer mf = getManufacturer();
        
        // 优先使用应用私有目录（所有系统都有权限）
        File appPrivateDir = context.getExternalFilesDir(null);
        if (appPrivateDir != null && appPrivateDir.exists()) {
            File folder = new File(appPrivateDir, folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            return folder.getAbsolutePath();
        }

        // 备用：公共目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            File publicDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (publicDir != null) {
                File folder = new File(publicDir, folderName);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                return folder.getAbsolutePath();
            }
        } else {
            File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (publicDir != null) {
                File folder = new File(publicDir, folderName);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                return folder.getAbsolutePath();
            }
        }

        // 最终备用：缓存目录
        File cacheDir = context.getCacheDir();
        File folder = new File(cacheDir, folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder.getAbsolutePath();
    }

    /**
     * 检查并请求存储权限
     * @return true表示已有权限，false表示需要跳转设置
     */
    public static boolean checkAndRequestPermission(Context context) {
        if (hasStoragePermission(context)) {
            return true;
        }
        jumpToStorageSettings(context);
        return false;
    }

    /**
     * 获取厂商名称描述
     */
    public static String getManufacturerName() {
        Manufacturer mf = getManufacturer();
        switch (mf) {
            case XIAOMI: return "小米 HyperOS/MIUI";
            case HUAWEI: return "华为 HarmonyOS/EMUI";
            case OPPO: return "OPPO ColorOS";
            case VIVO: return "vivo OriginOS";
            case HONOR: return "荣耀 MagicOS";
            case MEIZU: return "魅族 Flyme";
            case SAMSUNG: return "三星 OneUI";
            case ONEPLUS: return "一加 OxygenOS";
            case REALME: return "realme UI";
            case IQOO: return "iQOO UI";
            case GOOGLE: return "Google Android";
            default: return "其他设备";
        }
    }

    /**
     * 判断是否为HyperOS系统
     */
    public static boolean isHyperOS() {
        if (getManufacturer() != Manufacturer.XIAOMI) {
            return false;
        }
        try {
            String osName = System.getProperty("ro.miui.ui.version.name", "");
            // HyperOS版本号通常以"OS"开头或包含"Hyp"
            return osName.startsWith("OS") || osName.toLowerCase().contains("hyp");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为鸿蒙系统
     */
    public static boolean isHarmonyOS() {
        if (getManufacturer() != Manufacturer.HUAWEI) {
            return false;
        }
        try {
            // 鸿蒙系统特征
            String osVersion = System.getProperty("ro.build.version.emui", "");
            return osVersion.contains("Harmony") || osVersion.contains("HMOS");
        } catch (Exception e) {
            return false;
        }
    }
}