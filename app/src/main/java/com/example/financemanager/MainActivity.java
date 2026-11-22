package com.example.financemanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.financemanager.database.Transaction;
import com.example.financemanager.database.TransactionTag;
import com.example.financemanager.database.TransactionTemplate;
import com.example.financemanager.model.BudgetManager;
import com.example.financemanager.model.CategoryManager;
import com.example.financemanager.viewmodel.TransactionViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private BudgetManager budgetManager;
    private TransactionViewModel viewModel;

    /** 全部标签名与全部模板的缓存，由 LiveData 维护，打开记账对话框时读取 */
    private final List<String> allTagNames = new ArrayList<>();
    private final List<TransactionTemplate> allTemplates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CategoryManager.init(this);
        budgetManager = new BudgetManager(this);
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // 标签库：变化时更新缓存（对话框每次打开时按缓存重建 Chip）
        viewModel.getAllTags().observe(this, tags -> {
            allTagNames.clear();
            if (tags != null) {
                for (TransactionTag t : tags) allTagNames.add(t.name);
            }
        });
        // 模板库缓存
        viewModel.getAllTemplates().observe(this, templates -> {
            allTemplates.clear();
            if (templates != null) allTemplates.addAll(templates);
        });

        initListeners();
        checkBudgetSetting();

        if (savedInstanceState == null) {
            showHome();
        }
    }

    private void checkBudgetSetting() {
        if (budgetManager.needsNewMonthBudget()) {
            showBudgetDialog();
        }
    }

    private void showBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_budget, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        final EditText etBudget = dialogView.findViewById(R.id.etBudget);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        double currentBudget = budgetManager.getMonthlyBudget();
        etBudget.setText(String.format("%.0f", currentBudget));

        final AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String budgetStr = etBudget.getText().toString().trim();
                if (budgetStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请输入预算金额", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double budget = Double.parseDouble(budgetStr);
                    if (budget <= 0) {
                        Toast.makeText(MainActivity.this, "预算金额必须大于0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    budgetManager.setMonthlyBudget(budget);
                    Toast.makeText(MainActivity.this, "预算设置成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void initListeners() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    showHome();
                    return true;
                } else if (id == R.id.nav_statistics) {
                    showFragment(new StatisticsFragment());
                    return true;
                } else if (id == R.id.nav_export) {
                    showFragment(new ExportFragment());
                    return true;
                } else if (id == R.id.nav_settings) {
                    showFragment(new SettingsFragment());
                    return true;
                }
                return false;
            }
        });
    }

    private void showHome() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            return;
        }
        ft.replace(R.id.fragment_container, new HomeFragment());
        ft.commit();
    }

    private void showFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void showAddTransactionDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);

        final EditText etAmount = dialogView.findViewById(R.id.etAmount);
        final EditText etReason = dialogView.findViewById(R.id.etReason);
        final Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        final Spinner spinnerTemplate = dialogView.findViewById(R.id.spinnerTemplate);
        final CheckBox cbHousehold = dialogView.findViewById(R.id.cbHousehold);
        final CheckBox cbSaveTemplate = dialogView.findViewById(R.id.cbSaveTemplate);
        final ChipGroup chipGroupTags = dialogView.findViewById(R.id.chipGroupTags);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        final String[] categories = CategoryManager.getCategories(type);
        final ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        final AlertDialog dialog = builder.create();
        final boolean[] userManuallySelected = {false};
        final boolean[] isInitialSelection = {true};
        final boolean[] templateLoading = {false};

        // ---------- 快捷模板 ----------
        final List<TransactionTemplate> typeTemplates = new ArrayList<>();
        for (TransactionTemplate t : allTemplates) {
            if (type.equals(t.type)) typeTemplates.add(t);
        }
        final String[] templateNames = new String[typeTemplates.size() + 1];
        templateNames[0] = "不使用模板";
        for (int i = 0; i < typeTemplates.size(); i++) {
            String suffix = typeTemplates.get(i).amount > 0
                    ? (" ¥" + String.format(Locale.getDefault(), "%.2f", typeTemplates.get(i).amount)) : "";
            templateNames[i + 1] = typeTemplates.get(i).name + suffix;
        }
        ArrayAdapter<String> templateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, templateNames);
        templateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTemplate.setAdapter(templateAdapter);

        spinnerTemplate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || templateLoading[0]) return;
                TransactionTemplate tpl = typeTemplates.get(position - 1);
                if (tpl.amount > 0) {
                    etAmount.setText(String.format(Locale.getDefault(), "%.2f", tpl.amount));
                }
                if (tpl.reason != null && !tpl.reason.isEmpty()) {
                    etReason.setText(tpl.reason);
                }
                cbHousehold.setChecked(tpl.householdAccounts);
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equals(tpl.subCategory)) {
                        spinnerCategory.setSelection(i);
                        userManuallySelected[0] = true;
                        break;
                    }
                }
                viewModel.markTemplateUsed(tpl.id);
                Toast.makeText(MainActivity.this, "已填充模板：" + tpl.name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        etReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!userManuallySelected[0]) {
                    String reason = s.toString();
                    String matchedCategory = CategoryManager.matchCategoryByKeyword(type, reason);
                    if (matchedCategory != null) {
                        for (int i = 0; i < categories.length; i++) {
                            if (categories[i].equals(matchedCategory)) {
                                spinnerCategory.setSelection(i);
                                break;
                            }
                        }
                    }
                }
            }
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialSelection[0]) {
                    isInitialSelection[0] = false;
                    return;
                }
                userManuallySelected[0] = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ---------- 标签 Chip ----------
        final Set<String> selectedTags = new HashSet<>();
        rebuildTagChips(chipGroupTags, selectedTags);
        // 对话框弹出后 ChipGroup 宽度就绪，再添加"新建标签"按钮 Chip
        chipGroupTags.post(() -> addCreateTagChip(chipGroupTags, selectedTags));

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = etAmount.getText().toString().trim();
                String reason = etReason.getText().toString().trim();
                String subCategory = spinnerCategory.getSelectedItem().toString();

                if (amountStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.please_enter_amount, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (reason.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.please_enter_reason, Toast.LENGTH_SHORT).show();
                    return;
                }

                final double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        Toast.makeText(MainActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!userManuallySelected[0]) {
                    String matchedCategory = CategoryManager.matchCategoryByKeyword(type, reason);
                    if (matchedCategory != null) {
                        subCategory = matchedCategory;
                    }
                }

                final String finalSubCategory = subCategory;
                final List<String> tags = new ArrayList<>(selectedTags);

                if ("Expense".equals(type)) {
                    // 异常消费检测：回调在后台线程，UI 操作切回主线程
                    viewModel.checkExpenseAnomaly(finalSubCategory, amount, info -> runOnUiThread(() -> {
                        if (info.anomaly) {
                            String message = "这笔 ¥" + String.format(Locale.getDefault(), "%.2f", amount)
                                    + " 达到「" + finalSubCategory + "」历史均值 ¥"
                                    + String.format(Locale.getDefault(), "%.2f", info.avgAmount)
                                    + "（共 " + info.count + " 笔）的 "
                                    + String.format(Locale.getDefault(), "%.0f", info.multiplier)
                                    + " 倍，确认要记录吗？";
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("消费异常提醒")
                                    .setMessage(message)
                                    .setPositiveButton("仍要保存", (d, w) ->
                                            doSave(type, amount, finalSubCategory, reason,
                                                    cbHousehold.isChecked(), tags,
                                                    cbSaveTemplate.isChecked(), dialog))
                                    .setNegativeButton("再想想", null)
                                    .show();
                        } else {
                            doSave(type, amount, finalSubCategory, reason,
                                    cbHousehold.isChecked(), tags,
                                    cbSaveTemplate.isChecked(), dialog);
                        }
                    }));
                } else {
                    doSave(type, amount, finalSubCategory, reason,
                            cbHousehold.isChecked(), tags,
                            cbSaveTemplate.isChecked(), dialog);
                }
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /** 重建标签 Chip（保留选中状态），末尾由调用方追加"新建标签" */
    private void rebuildTagChips(ChipGroup chipGroup, Set<String> selectedTags) {
        chipGroup.removeAllViews();
        for (String name : allTagNames) {
            Chip chip = new Chip(this);
            chip.setText(name);
            chip.setCheckable(true);
            chip.setChecked(selectedTags.contains(name));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedTags.add(name);
                else selectedTags.remove(name);
            });
            chipGroup.addView(chip);
        }
    }

    private void addCreateTagChip(final ChipGroup chipGroup, final Set<String> selectedTags) {
        Chip createChip = new Chip(this);
        createChip.setText("＋ 新标签");
        createChip.setCheckable(false);
        createChip.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setHint("输入标签名称");
            new AlertDialog.Builder(this)
                    .setTitle("新建标签")
                    .setView(input)
                    .setPositiveButton("创建", (d, w) -> {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(this, "标签名不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (name.length() > 12) {
                            Toast.makeText(this, "标签名最多 12 个字", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        viewModel.createTag(name, success -> runOnUiThread(() -> {
                            if (success) {
                                if (!allTagNames.contains(name)) allTagNames.add(name);
                                selectedTags.add(name);
                                rebuildTagChips(chipGroup, selectedTags);
                                addCreateTagChip(chipGroup, selectedTags);
                                Toast.makeText(this, "标签已创建并选中", Toast.LENGTH_SHORT).show();
                            } else {
                                // 标签已存在：直接选中
                                selectedTags.add(name);
                                rebuildTagChips(chipGroup, selectedTags);
                                addCreateTagChip(chipGroup, selectedTags);
                                Toast.makeText(this, "标签已存在，已为你选中", Toast.LENGTH_SHORT).show();
                            }
                        }));
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        chipGroup.addView(createChip);
    }

    /** 最终保存：交易 + 标签 + 可选模板 */
    private void doSave(String type, double amount, String subCategory, String reason,
                        boolean household, List<String> tags, boolean saveAsTemplate,
                        AlertDialog dialog) {
        CategoryManager.rememberKeyword(type, subCategory, reason);

        // 毫秒级时间戳，与 PC 端格式对齐，便于作为唯一标识去重
        String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        Transaction transaction = new Transaction();
        transaction.dateTime = dateTime;
        transaction.amount = amount;
        transaction.category = type;
        transaction.subCategory = subCategory;
        transaction.reason = reason;
        transaction.householdAccounts = household;
        transaction.isCanceled = false;

        viewModel.insertWithTags(transaction, tags);

        if (saveAsTemplate) {
            TransactionTemplate tpl = new TransactionTemplate();
            tpl.name = reason.length() > 10 ? reason.substring(0, 10) : reason;
            tpl.type = type;
            tpl.subCategory = subCategory;
            tpl.amount = amount;
            tpl.reason = reason;
            tpl.householdAccounts = household;
            viewModel.saveTemplate(tpl);
        }

        Toast.makeText(this, type.equals("Expense") ? "支出记录已添加" : "收入记录已添加",
                Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }
}
