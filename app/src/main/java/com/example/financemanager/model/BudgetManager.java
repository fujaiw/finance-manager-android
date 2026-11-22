package com.example.financemanager.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BudgetManager {
    private static final String PREF_NAME = "BudgetSettings";
    private static final String KEY_MONTHLY_BUDGET = "monthly_budget";
    private static final String KEY_BUDGET_SET_DATE = "budget_set_date";
    private static final String KEY_BUDGET_REMINDER = "budget_reminder_enabled";
    private static final String KEY_INITIAL_ASSET = "initial_asset";
    private static final String KEY_ANOMALY_ALERT_ENABLED = "anomaly_alert_enabled";

    private SharedPreferences prefs;

    public BudgetManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public double getMonthlyBudget() {
        return prefs.getFloat(KEY_MONTHLY_BUDGET, 5000.0f);
    }

    public void setMonthlyBudget(double budget) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_MONTHLY_BUDGET, (float) budget);
        editor.putString(KEY_BUDGET_SET_DATE, getCurrentDate());
        editor.apply();
    }

    /**
     * 起始资金（资产总览用），与预算共用 BudgetPreferences。
     * 使用 float 存储与本类其他金额字段保持一致。
     */
    public double getInitialAsset() {
        return prefs.getFloat(KEY_INITIAL_ASSET, 0.0f);
    }

    public void setInitialAsset(double amount) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_INITIAL_ASSET, (float) amount);
        editor.apply();
    }

    /**
     * 消费异常提醒开关，默认开启。
     */
    public boolean isAnomalyAlertEnabled() {
        return prefs.getBoolean(KEY_ANOMALY_ALERT_ENABLED, true);
    }

    public void setAnomalyAlertEnabled(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ANOMALY_ALERT_ENABLED, enabled);
        editor.apply();
    }

    public boolean isReminderEnabled() {
        return prefs.getBoolean(KEY_BUDGET_REMINDER, true);
    }

    public void setReminderEnabled(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_BUDGET_REMINDER, enabled);
        editor.apply();
    }

    public String getBudgetSetDate() {
        return prefs.getString(KEY_BUDGET_SET_DATE, "");
    }

    public double getDailyBudget() {
        double monthlyBudget = getMonthlyBudget();
        int daysInMonth = getDaysInCurrentMonth();
        return monthlyBudget / daysInMonth;
    }

    public double getRemainingDays() {
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = getDaysInCurrentMonth();
        return daysInMonth - today + 1;
    }

    public double getRemainingBudget(double spentAmount) {
        return getMonthlyBudget() - spentAmount;
    }

    public double getDailyRemainingBudget(double spentAmount) {
        double remainingBudget = getRemainingBudget(spentAmount);
        double remainingDays = getRemainingDays();
        if (remainingDays <= 0) return 0;
        return remainingBudget / remainingDays;
    }

    public double getBudgetUsagePercent(double spentAmount) {
        double budget = getMonthlyBudget();
        if (budget <= 0) return 0;
        return (spentAmount / budget) * 100;
    }

    public boolean isOverBudget(double spentAmount) {
        return spentAmount > getMonthlyBudget();
    }

    public boolean isBudgetWarning(double spentAmount) {
        double percent = getBudgetUsagePercent(spentAmount);
        return percent >= 80;
    }

    private int getDaysInCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public boolean needsNewMonthBudget() {
        String lastSetDate = getBudgetSetDate();
        if (lastSetDate.isEmpty()) return true;

        Calendar lastSet = Calendar.getInstance();
        try {
            lastSet.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastSetDate));
        } catch (Exception e) {
            return true;
        }

        Calendar now = Calendar.getInstance();
        return lastSet.get(Calendar.YEAR) != now.get(Calendar.YEAR) ||
               lastSet.get(Calendar.MONTH) != now.get(Calendar.MONTH);
    }
}
