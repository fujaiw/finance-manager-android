package com.example.financemanager.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CategoryManager {
    private static final String PREF_NAME = "CategoryKeywords";
    private static final String KEY_EXPENSE_PREFIX = "expense_";
    private static final String KEY_INCOME_PREFIX = "income_";

    private static final Map<String, String[]> DEFAULT_EXPENSE_CATEGORIES = new HashMap<>();
    private static final Map<String, String[]> DEFAULT_INCOME_CATEGORIES = new HashMap<>();
    private static final Map<String, Map<String, String[]>> DEFAULT_KEYWORD_RULES = new HashMap<>();

    private static Map<String, String[]> EXPENSE_CATEGORIES = new HashMap<>();
    private static Map<String, String[]> INCOME_CATEGORIES = new HashMap<>();
    private static Map<String, Map<String, String[]>> KEYWORD_RULES = new HashMap<>();

    private static SharedPreferences prefs;
    private static Map<String, Set<String>> learnedKeywords = new HashMap<>();

    static {
        DEFAULT_EXPENSE_CATEGORIES.put("餐饮", new String[]{"[吃]", "#FF6B6B"});
        DEFAULT_EXPENSE_CATEGORIES.put("交通", new String[]{"[行]", "#4ECDC4"});
        DEFAULT_EXPENSE_CATEGORIES.put("购物", new String[]{"[购]", "#45B7D1"});
        DEFAULT_EXPENSE_CATEGORIES.put("房租", new String[]{"[住]", "#96CEB4"});
        DEFAULT_EXPENSE_CATEGORIES.put("日常", new String[]{"[用]", "#FFEAA7"});
        DEFAULT_EXPENSE_CATEGORIES.put("娱乐", new String[]{"[乐]", "#DDA0DD"});
        DEFAULT_EXPENSE_CATEGORIES.put("教育", new String[]{"[教]", "#98D8C8"});
        DEFAULT_EXPENSE_CATEGORIES.put("医疗", new String[]{"[医]", "#A29BFE"});
        DEFAULT_EXPENSE_CATEGORIES.put("理财亏损", new String[]{"[损]", "#6C5CE7"});

        DEFAULT_INCOME_CATEGORIES.put("工资", new String[]{"[薪]", "#00B894"});
        DEFAULT_INCOME_CATEGORIES.put("兼职", new String[]{"[兼]", "#00CEC9"});
        DEFAULT_INCOME_CATEGORIES.put("理财", new String[]{"[财]", "#0984E3"});
        DEFAULT_INCOME_CATEGORIES.put("红包", new String[]{"[红]", "#E17055"});
        DEFAULT_INCOME_CATEGORIES.put("奖金", new String[]{"[奖]", "#6C5CE7"});
        DEFAULT_INCOME_CATEGORIES.put("其他", new String[]{"[其]", "#B2BEC3"});

        Map<String, String[]> expenseRules = new HashMap<>();
        expenseRules.put("餐饮", new String[]{"烧烤", "水果", "零食", "餐饮", "午餐", "晚餐", "早餐", "快餐", "外卖", "饭店", "食堂", "奶茶", "饮料", "超市", "便利店", "小卖部"});
        expenseRules.put("交通", new String[]{"油费", "车载", "滴滴", "打车", "地铁", "公交", "出租车", "火车", "高铁", "飞机", "停车", "过路费", "保养", "维修"});
        expenseRules.put("日常", new String[]{"话费", "流量", "会员", "宽带", "水电费", "物业费", "日用品", "洗漱用品", "护肤", "化妆品", "理发", "干洗"});
        expenseRules.put("购物", new String[]{"购物", "衣服", "鞋子", "包", "京东", "淘宝", "天猫", "拼多多", "唯品会", "电器", "电子产品"});
        expenseRules.put("医疗", new String[]{"保险", "医疗", "医院", "药品", "挂号", "门诊", "体检", "药店", "口罩", "消毒"});
        expenseRules.put("娱乐", new String[]{"电影", "游戏", "KTV", "酒吧", "咖啡", "旅游", "健身", "游泳", "球类", "演唱会", "话剧"});
        expenseRules.put("教育", new String[]{"学费", "培训", "书籍", "课程", "考试", "报名", "教材", "文具", "补习", "辅导"});
        expenseRules.put("房租", new String[]{"房租", "租金", "房贷", "住房", "房租费", "中介费"});
        DEFAULT_KEYWORD_RULES.put("Expense", expenseRules);

        Map<String, String[]> incomeRules = new HashMap<>();
        incomeRules.put("工资", new String[]{"工资", "底薪", "提成", "补贴", "餐补", "交通补", "全勤奖"});
        incomeRules.put("兼职", new String[]{"兼职", "外快", "副业", "私单", "接单", "外包"});
        incomeRules.put("理财", new String[]{"理财", "利息", "证券", "基金", "股票", "分红", "股息", "投资收益"});
        incomeRules.put("红包", new String[]{"红包", "返现", "代金券", "优惠券", "奖励金"});
        incomeRules.put("奖金", new String[]{"奖金", "年终奖", "项目奖", "绩效", "年中奖"});
        incomeRules.put("其他", new String[]{"退款", "补偿", "赔偿", "礼金", "礼尚往来"});
        DEFAULT_KEYWORD_RULES.put("Income", incomeRules);

        learnedKeywords.put("Expense", new HashSet<String>());
        learnedKeywords.put("Income", new HashSet<String>());
    }

    public static void init(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ConfigFileManager.initDefaultConfig(context);
        loadFromConfig(context);
        loadLearnedKeywords();
    }

    public static void loadFromConfig(Context context) {
        String configContent = ConfigFileManager.readConfigFile(context);

        Map<String, String[]> expenseCats = ConfigFileManager.parseCategories(configContent, "ExpenseCategories");
        Map<String, String[]> incomeCats = ConfigFileManager.parseCategories(configContent, "IncomeCategories");

        if (expenseCats.isEmpty()) {
            expenseCats = new HashMap<>(DEFAULT_EXPENSE_CATEGORIES);
        }
        if (incomeCats.isEmpty()) {
            incomeCats = new HashMap<>(DEFAULT_INCOME_CATEGORIES);
        }

        EXPENSE_CATEGORIES = expenseCats;
        INCOME_CATEGORIES = incomeCats;

        Map<String, String[]> expenseRules = ConfigFileManager.parseKeywordRules(configContent, "Expense");
        Map<String, String[]> incomeRules = ConfigFileManager.parseKeywordRules(configContent, "Income");

        if (expenseRules.isEmpty()) {
            expenseRules = new HashMap<>(DEFAULT_KEYWORD_RULES.get("Expense"));
        }
        if (incomeRules.isEmpty()) {
            incomeRules = new HashMap<>(DEFAULT_KEYWORD_RULES.get("Income"));
        }

        KEYWORD_RULES.put("Expense", expenseRules);
        KEYWORD_RULES.put("Income", incomeRules);
    }

    public static void saveToConfig(Context context) {
        String content = ConfigFileManager.buildConfigContent(
                EXPENSE_CATEGORIES,
                INCOME_CATEGORIES,
                KEYWORD_RULES.get("Expense"),
                KEYWORD_RULES.get("Income")
        );
        ConfigFileManager.writeConfigFile(context, content);
    }

    public static void addCategory(Context context, String categoryType, String name, String icon, String color) {
        Map<String, String[]> categories = "Expense".equals(categoryType) ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
        categories.put(name, new String[]{icon, color});

        Map<String, String[]> rules = KEYWORD_RULES.get(categoryType);
        if (rules != null && !rules.containsKey(name)) {
            rules.put(name, new String[]{});
        }

        saveToConfig(context);
    }

    public static void updateCategory(Context context, String categoryType, String oldName, String newName, String icon, String color) {
        Map<String, String[]> categories = "Expense".equals(categoryType) ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;

        String[] oldInfo = categories.remove(oldName);
        if (oldInfo == null) {
            oldInfo = new String[]{icon, color};
        }

        categories.put(newName, new String[]{icon, color});

        Map<String, String[]> rules = KEYWORD_RULES.get(categoryType);
        if (rules != null) {
            String[] oldKeywords = rules.remove(oldName);
            if (oldKeywords != null) {
                rules.put(newName, oldKeywords);
            } else {
                rules.put(newName, new String[]{});
            }
        }

        saveToConfig(context);
    }

    public static void deleteCategory(Context context, String categoryType, String name) {
        Map<String, String[]> categories = "Expense".equals(categoryType) ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
        categories.remove(name);

        Map<String, String[]> rules = KEYWORD_RULES.get(categoryType);
        if (rules != null) {
            rules.remove(name);
        }

        saveToConfig(context);
    }

    public static void updateKeywordRules(Context context, String categoryType, String category, String[] keywords) {
        Map<String, String[]> rules = KEYWORD_RULES.get(categoryType);
        if (rules != null) {
            rules.put(category, keywords);
            saveToConfig(context);
        }
    }

    private static void loadLearnedKeywords() {
        if (prefs == null) return;

        Map<String, String> expenseKeywordMap = new HashMap<>();
        for (String category : EXPENSE_CATEGORIES.keySet()) {
            Set<String> keywords = prefs.getStringSet(KEY_EXPENSE_PREFIX + category, new HashSet<String>());
            for (String key : keywords) {
                int sepIdx = key.indexOf("=>");
                if (sepIdx > 0) {
                    String keyword = key.substring(sepIdx + 2);
                    expenseKeywordMap.put(keyword, key);
                }
            }
        }
        learnedKeywords.get("Expense").clear();
        learnedKeywords.get("Expense").addAll(expenseKeywordMap.values());

        Map<String, String> incomeKeywordMap = new HashMap<>();
        for (String category : INCOME_CATEGORIES.keySet()) {
            Set<String> keywords = prefs.getStringSet(KEY_INCOME_PREFIX + category, new HashSet<String>());
            for (String key : keywords) {
                int sepIdx = key.indexOf("=>");
                if (sepIdx > 0) {
                    String keyword = key.substring(sepIdx + 2);
                    incomeKeywordMap.put(keyword, key);
                }
            }
        }
        learnedKeywords.get("Income").clear();
        learnedKeywords.get("Income").addAll(incomeKeywordMap.values());
    }

    private static void saveLearnedKeyword(String categoryType, String category, String keyword) {
        if (prefs == null) return;

        String prefix = "Expense".equals(categoryType) ? KEY_EXPENSE_PREFIX : KEY_INCOME_PREFIX;
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> existing = prefs.getStringSet(prefix + category, new HashSet<String>());
        existing.add(keyword.toLowerCase());
        editor.putStringSet(prefix + category, existing);
        editor.commit();
    }

    public static String[] getExpenseCategories() {
        return EXPENSE_CATEGORIES.keySet().toArray(new String[0]);
    }

    public static String[] getIncomeCategories() {
        return INCOME_CATEGORIES.keySet().toArray(new String[0]);
    }

    public static String[] getCategories(String type) {
        if ("Expense".equals(type)) {
            return getExpenseCategories();
        } else {
            return getIncomeCategories();
        }
    }

    public static String matchCategoryByKeyword(String categoryType, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return null;
        }

        String lowerReason = reason.toLowerCase().trim();

        Map<String, String[]> rules = KEYWORD_RULES.get(categoryType);
        if (rules == null) return null;

        for (Map.Entry<String, String[]> entry : rules.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerReason.contains(keyword.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }

        Set<String> learned = learnedKeywords.get(categoryType);
        if (learned != null) {
            Map<String, String> keywordToCategory = new HashMap<>();
            for (String learnedKeyword : learned) {
                int separatorIndex = learnedKeyword.indexOf("=>");
                if (separatorIndex > 0) {
                    String keywordPart = learnedKeyword.substring(separatorIndex + 2);
                    String categoryPart = learnedKeyword.substring(0, separatorIndex);
                    keywordToCategory.put(keywordPart, categoryPart);
                }
            }

            for (Map.Entry<String, String> entry : keywordToCategory.entrySet()) {
                if (lowerReason.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    public static void rememberKeyword(String categoryType, String category, String reason) {
        if (categoryType == null || category == null || reason == null) return;
        if (reason.trim().isEmpty()) return;

        String lowerReason = reason.toLowerCase().trim();
        String newKey = category + "=>" + lowerReason;

        Set<String> learned = learnedKeywords.get(categoryType);
        if (learned != null) {
            for (String existingKey : new HashSet<>(learned)) {
                int sepIdx = existingKey.indexOf("=>");
                if (sepIdx > 0) {
                    String existingKeyword = existingKey.substring(sepIdx + 2);
                    if (existingKeyword.equals(lowerReason)) {
                        learned.remove(existingKey);
                        removeLearnedKeywordFromPrefs(categoryType, existingKey);
                    }
                }
            }
            learned.add(newKey);
            saveLearnedKeyword(categoryType, category, newKey);
        }
    }

    private static void removeLearnedKeywordFromPrefs(String categoryType, String key) {
        if (prefs == null) return;

        int sepIdx = key.indexOf("=>");
        if (sepIdx <= 0) return;

        String keywordToRemove = key.substring(sepIdx + 2);
        String prefix = "Expense".equals(categoryType) ? KEY_EXPENSE_PREFIX : KEY_INCOME_PREFIX;
        SharedPreferences.Editor editor = prefs.edit();

        Map<String, String[]> categories = "Expense".equals(categoryType) ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
        for (String category : categories.keySet()) {
            Set<String> existing = prefs.getStringSet(prefix + category, new HashSet<String>());
            Set<String> toRemove = new HashSet<>();
            for (String existingKey : existing) {
                int existingSepIdx = existingKey.indexOf("=>");
                if (existingSepIdx > 0) {
                    String existingKeyword = existingKey.substring(existingSepIdx + 2);
                    if (existingKeyword.equals(keywordToRemove)) {
                        toRemove.add(existingKey);
                    }
                }
            }
            if (!toRemove.isEmpty()) {
                existing.removeAll(toRemove);
                editor.putStringSet(prefix + category, existing);
            }
        }
        editor.commit();
    }

    public static String getCategoryIcon(String category, String type) {
        Map<String, String[]> categories = "Expense".equals(type) ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
        String[] info = categories.get(category);
        return info != null ? info[0] : "[其]";
    }

    public static String getCategoryColor(String category, String type) {
        Map<String, String[]> categories = "Expense".equals(type) ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
        String[] info = categories.get(category);
        return info != null ? info[1] : "#B2BEC3";
    }

    public static Map<String, String[]> getKeywordRules(String categoryType) {
        return KEYWORD_RULES.get(categoryType);
    }

    public static void clearLearnedKeywords() {
        if (prefs != null) {
            prefs.edit().clear().commit();
        }
        learnedKeywords.get("Expense").clear();
        learnedKeywords.get("Income").clear();
    }

    public static Map<String, Set<String>> getLearnedKeywords() {
        Map<String, Set<String>> result = new HashMap<>();
        result.put("Expense", new HashSet<String>());
        result.put("Income", new HashSet<String>());

        if (prefs == null) return result;

        String prefix = KEY_EXPENSE_PREFIX;
        Map<String, String[]> categories = EXPENSE_CATEGORIES;
        for (String category : categories.keySet()) {
            Set<String> keywords = prefs.getStringSet(prefix + category, new HashSet<String>());
            result.get("Expense").addAll(keywords);
        }

        prefix = KEY_INCOME_PREFIX;
        categories = INCOME_CATEGORIES;
        for (String category : categories.keySet()) {
            Set<String> keywords = prefs.getStringSet(prefix + category, new HashSet<String>());
            result.get("Income").addAll(keywords);
        }

        return result;
    }

    public static void removeLearnedKeyword(String categoryType, String category, String keyword) {
        if (prefs == null || categoryType == null || category == null || keyword == null) return;

        String prefix = "Expense".equals(categoryType) ? KEY_EXPENSE_PREFIX : KEY_INCOME_PREFIX;
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> existing = prefs.getStringSet(prefix + category, new HashSet<String>());
        String keyToRemove = category + "=>" + keyword.toLowerCase();
        existing.remove(keyToRemove);
        editor.putStringSet(prefix + category, existing);
        editor.commit();

        loadLearnedKeywords();
    }

    public static void updateLearnedKeyword(String categoryType, String oldKeyword, String newKeyword, String newCategory) {
        if (categoryType == null || oldKeyword == null || newKeyword == null || newCategory == null) return;
        if (newKeyword.trim().isEmpty()) return;

        loadLearnedKeywords();

        String lowerOldKeyword = oldKeyword.toLowerCase().trim();
        String lowerNewKeyword = newKeyword.toLowerCase().trim();

        String prefix = "Expense".equals(categoryType) ? KEY_EXPENSE_PREFIX : KEY_INCOME_PREFIX;
        Map<String, String[]> categories = "Expense".equals(categoryType) ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
        SharedPreferences.Editor editor = prefs.edit();

        for (String category : categories.keySet()) {
            Set<String> existing = prefs.getStringSet(prefix + category, new HashSet<String>());
            Set<String> toRemove = new HashSet<>();
            for (String existingKey : existing) {
                int sepIdx = existingKey.indexOf("=>");
                if (sepIdx > 0) {
                    String existingKeyword = existingKey.substring(sepIdx + 2);
                    if (existingKeyword.equals(lowerOldKeyword) || existingKeyword.equals(lowerNewKeyword)) {
                        toRemove.add(existingKey);
                    }
                }
            }
            if (!toRemove.isEmpty()) {
                existing.removeAll(toRemove);
                editor.putStringSet(prefix + category, existing);
            }
        }

        String newKey = newCategory + "=>" + lowerNewKeyword;
        Set<String> newSet = prefs.getStringSet(prefix + newCategory, new HashSet<String>());
        newSet.add(newKey);
        editor.putStringSet(prefix + newCategory, newSet);

        editor.commit();

        loadLearnedKeywords();
    }

    public static Map<String, String[]> getExpenseCategoriesMap() {
        return EXPENSE_CATEGORIES;
    }

    public static Map<String, String[]> getIncomeCategoriesMap() {
        return INCOME_CATEGORIES;
    }
}