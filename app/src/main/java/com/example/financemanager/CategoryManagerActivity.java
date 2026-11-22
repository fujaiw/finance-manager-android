package com.example.financemanager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.financemanager.model.CategoryManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CategoryManagerActivity extends AppCompatActivity {
    private Button btnExpense;
    private Button btnIncome;
    private EditText etCategoryName;
    private Button btnAddCategory;
    private LinearLayout categoryList;
    private TextView tvSelectedCategory;
    private EditText etKeyword;
    private Button btnAddKeyword;
    private LinearLayout keywordList;

    private String currentType = "Expense";
    private String selectedCategory = null;

    private final String[] ICONS = {"[食]", "[行]", "[购]", "[住]", "[用]", "[乐]", "[教]", "[医]", "[薪]", "[兼]", "[财]", "[红]", "[奖]", "[其]", "[杂]", "[礼]", "[邮]", "[网]", "[水]", "[电]", "[气]", "[煤]", "[车]", "[房]", "[衣]", "[书]", "[花]", "[玩]", "[学]", "[工]"};
    private final String[] COLORS = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#98D8C8", "#A29BFE", "#6C5CE7", "#00B894", "#00CEC9", "#0984E3", "#E17055", "#B2BEC3", "#FD79A8", "#E17055", "#00CEC9", "#74B9FF", "#00B894", "#FDCB6E", "#E84393", "#6C5CE7", "#FD79A8", "#00B894", "#FD79A8", "#74B9FF", "#00CEC9", "#FDCB6E", "#E84393", "#0984E3"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        initViews();
        loadCategories();
    }

    private void initViews() {
        btnExpense = findViewById(R.id.btnExpense);
        btnIncome = findViewById(R.id.btnIncome);
        etCategoryName = findViewById(R.id.etCategoryName);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        categoryList = findViewById(R.id.categoryList);
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        etKeyword = findViewById(R.id.etKeyword);
        btnAddKeyword = findViewById(R.id.btnAddKeyword);
        keywordList = findViewById(R.id.keywordList);

        btnExpense.setOnClickListener(v -> switchType("Expense"));
        btnIncome.setOnClickListener(v -> switchType("Income"));

        btnAddCategory.setOnClickListener(v -> addCategory());
        btnAddKeyword.setOnClickListener(v -> addKeyword());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void switchType(String type) {
        currentType = type;
        selectedCategory = null;

        if ("Expense".equals(type)) {
            btnExpense.setTextColor(getResources().getColor(R.color.white));
            btnExpense.setBackgroundColor(getResources().getColor(R.color.purple_700));
            btnIncome.setTextColor(getResources().getColor(R.color.purple_200));
            btnIncome.setBackgroundColor(Color.TRANSPARENT);
        } else {
            btnIncome.setTextColor(getResources().getColor(R.color.white));
            btnIncome.setBackgroundColor(getResources().getColor(R.color.purple_700));
            btnExpense.setTextColor(getResources().getColor(R.color.purple_200));
            btnExpense.setBackgroundColor(Color.TRANSPARENT);
        }

        loadCategories();
        updateSelectedCategoryDisplay();
        loadKeywords();
    }

    private void loadCategories() {
        categoryList.removeAllViews();

        Map<String, String[]> categories = "Expense".equals(currentType)
                ? CategoryManager.getExpenseCategoriesMap()
                : CategoryManager.getIncomeCategoriesMap();

        List<String> sortedCategories = new ArrayList<>(categories.keySet());
        java.util.Collections.sort(sortedCategories);

        for (String category : sortedCategories) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_learned_category, categoryList, false);
            TextView tvKeyword = itemView.findViewById(R.id.tvKeyword);
            TextView tvCategory = itemView.findViewById(R.id.tvCategory);
            Button btnEdit = itemView.findViewById(R.id.btnEdit);
            Button btnDelete = itemView.findViewById(R.id.btnDelete);

            String[] info = categories.get(category);
            String icon = info != null ? info[0] : "[其]";
            String color = info != null ? info[1] : "#B2BEC3";

            tvKeyword.setText(icon + " " + category);
            tvCategory.setVisibility(View.GONE);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(20);
            bg.setColor(Color.parseColor(color));
            tvKeyword.setBackground(bg);
            tvKeyword.setTextColor(Color.WHITE);
            tvKeyword.setPadding(16, 8, 16, 8);

            if (category.equals(selectedCategory)) {
                tvKeyword.setBackgroundColor(getResources().getColor(R.color.purple_500));
            }

            itemView.setOnClickListener(v -> selectCategory(category));

            btnEdit.setOnClickListener(v -> showEditCategoryDialog(category));
            btnDelete.setOnClickListener(v -> showDeleteCategoryDialog(category));

            categoryList.addView(itemView);
        }

        if (categories.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("暂无分类，点击上方添加");
            tv.setTextSize(14);
            tv.setTextColor(getResources().getColor(R.color.grey_500));
            tv.setPadding(32, 32, 32, 32);
            tv.setGravity(android.view.Gravity.CENTER);
            categoryList.addView(tv);
        }
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateSelectedCategoryDisplay();
        loadCategories();
        loadKeywords();
    }

    private void updateSelectedCategoryDisplay() {
        if (selectedCategory != null) {
            tvSelectedCategory.setText("已选分类：" + selectedCategory);
            tvSelectedCategory.setTextColor(getResources().getColor(R.color.purple_500));
        } else {
            tvSelectedCategory.setText(getString(R.string.guide_step2));
            tvSelectedCategory.setTextColor(getResources().getColor(R.color.grey_500));
        }
    }

    private void loadKeywords() {
        keywordList.removeAllViews();

        if (selectedCategory == null) {
            TextView tv = new TextView(this);
            tv.setText("请先选择一个分类");
            tv.setTextSize(14);
            tv.setTextColor(getResources().getColor(R.color.grey_500));
            tv.setPadding(32, 32, 32, 32);
            tv.setGravity(android.view.Gravity.CENTER);
            keywordList.addView(tv);
            return;
        }

        Map<String, String[]> rules = CategoryManager.getKeywordRules(currentType);
        String[] keywords = rules != null ? rules.get(selectedCategory) : null;

        if (keywords == null || keywords.length == 0) {
            TextView tv = new TextView(this);
            tv.setText("该分类暂无关键词，点击上方添加");
            tv.setTextSize(14);
            tv.setTextColor(getResources().getColor(R.color.grey_500));
            tv.setPadding(32, 32, 32, 32);
            tv.setGravity(android.view.Gravity.CENTER);
            keywordList.addView(tv);
            return;
        }

        List<String> sortedKeywords = new ArrayList<>(Arrays.asList(keywords));
        java.util.Collections.sort(sortedKeywords);

        for (String keyword : sortedKeywords) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_learned_category, keywordList, false);
            TextView tvKeyword = itemView.findViewById(R.id.tvKeyword);
            TextView tvCategory = itemView.findViewById(R.id.tvCategory);
            Button btnEdit = itemView.findViewById(R.id.btnEdit);
            Button btnDelete = itemView.findViewById(R.id.btnDelete);

            tvKeyword.setText(keyword);
            tvCategory.setVisibility(View.GONE);
            btnEdit.setVisibility(View.GONE);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(20);
            bg.setColor(getResources().getColor(R.color.grey_200));
            tvKeyword.setBackground(bg);
            tvKeyword.setTextColor(getResources().getColor(R.color.textPrimary));
            tvKeyword.setPadding(16, 8, 16, 8);

            btnDelete.setOnClickListener(v -> showDeleteKeywordDialog(keyword));

            keywordList.addView(itemView);
        }
    }

    private void addCategory() {
        String name = etCategoryName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.category_name_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String[]> categories = "Expense".equals(currentType)
                ? CategoryManager.getExpenseCategoriesMap()
                : CategoryManager.getIncomeCategoriesMap();

        if (categories.containsKey(name)) {
            Toast.makeText(this, getString(R.string.category_exists), Toast.LENGTH_SHORT).show();
            return;
        }

        Random random = new Random();
        String icon = ICONS[random.nextInt(ICONS.length)];
        String color = COLORS[random.nextInt(COLORS.length)];

        CategoryManager.addCategory(this, currentType, name, icon, color);
        Toast.makeText(this, getString(R.string.category_added), Toast.LENGTH_SHORT).show();

        etCategoryName.setText("");
        loadCategories();
    }

    private void showEditCategoryDialog(String category) {
        Map<String, String[]> categories = "Expense".equals(currentType)
                ? CategoryManager.getExpenseCategoriesMap()
                : CategoryManager.getIncomeCategoriesMap();

        String[] info = categories.get(category);
        String icon = info != null ? info[0] : "[其]";
        String color = info != null ? info[1] : "#B2BEC3";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑分类");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_category, null);
        builder.setView(dialogView);

        final EditText etName = dialogView.findViewById(R.id.etKeyword);
        etName.setText(category);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, getString(R.string.category_name_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newName.equals(category)) {
                Map<String, String[]> cats = "Expense".equals(currentType)
                        ? CategoryManager.getExpenseCategoriesMap()
                        : CategoryManager.getIncomeCategoriesMap();
                if (cats.containsKey(newName)) {
                    Toast.makeText(this, getString(R.string.category_exists), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            CategoryManager.updateCategory(this, currentType, category, newName, icon, color);
            Toast.makeText(this, getString(R.string.category_updated), Toast.LENGTH_SHORT).show();

            if (selectedCategory != null && selectedCategory.equals(category)) {
                selectedCategory = newName;
            }

            loadCategories();
            updateSelectedCategoryDisplay();
            loadKeywords();
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteCategoryDialog(String category) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage(getString(R.string.confirm_delete_category))
                .setPositiveButton("确定", (dialog, which) -> {
                    CategoryManager.deleteCategory(this, currentType, category);
                    Toast.makeText(this, getString(R.string.category_deleted), Toast.LENGTH_SHORT).show();

                    if (selectedCategory != null && selectedCategory.equals(category)) {
                        selectedCategory = null;
                    }

                    loadCategories();
                    updateSelectedCategoryDisplay();
                    loadKeywords();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addKeyword() {
        if (selectedCategory == null) {
            Toast.makeText(this, "请先选择一个分类", Toast.LENGTH_SHORT).show();
            return;
        }

        String keyword = etKeyword.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, getString(R.string.keyword_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String[]> rules = CategoryManager.getKeywordRules(currentType);
        String[] existingKeywords = rules != null ? rules.get(selectedCategory) : null;

        if (existingKeywords != null) {
            for (String existing : existingKeywords) {
                if (existing.equalsIgnoreCase(keyword)) {
                    Toast.makeText(this, "关键词已存在", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        List<String> keywordsList = new ArrayList<>();
        if (existingKeywords != null) {
            keywordsList.addAll(Arrays.asList(existingKeywords));
        }
        keywordsList.add(keyword);

        CategoryManager.updateKeywordRules(this, currentType, selectedCategory, keywordsList.toArray(new String[0]));
        Toast.makeText(this, getString(R.string.keyword_added), Toast.LENGTH_SHORT).show();

        etKeyword.setText("");
        loadKeywords();
    }

    private void showDeleteKeywordDialog(String keyword) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage(getString(R.string.confirm_delete_keyword))
                .setPositiveButton("确定", (dialog, which) -> {
                    Map<String, String[]> rules = CategoryManager.getKeywordRules(currentType);
                    String[] existingKeywords = rules != null ? rules.get(selectedCategory) : null;

                    if (existingKeywords != null) {
                        List<String> keywordsList = new ArrayList<>(Arrays.asList(existingKeywords));
                        keywordsList.remove(keyword);
                        CategoryManager.updateKeywordRules(this, currentType, selectedCategory, keywordsList.toArray(new String[0]));
                        Toast.makeText(this, getString(R.string.keyword_deleted), Toast.LENGTH_SHORT).show();
                        loadKeywords();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}