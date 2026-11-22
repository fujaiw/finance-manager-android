package com.example.financemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.financemanager.database.TransactionTemplate;
import com.example.financemanager.model.CategoryManager;
import com.example.financemanager.viewmodel.TransactionViewModel;

import java.util.List;
import java.util.Locale;

/**
 * 记账模板管理：查看 / 新建 / 编辑 / 删除快捷记账模板。
 */
public class TemplateManagerActivity extends AppCompatActivity {

    private TransactionViewModel viewModel;
    private LinearLayout layoutList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_manager);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("快捷记账模板");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        layoutList = findViewById(R.id.layoutTemplateList);
        findViewById(R.id.btnAddTemplate).setOnClickListener(v -> showEditDialog(null));

        viewModel.getAllTemplates().observe(this, this::render);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void render(List<TransactionTemplate> templates) {
        layoutList.removeAllViews();
        if (templates == null || templates.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("还没有模板，记一笔时勾选“保存为快捷模板”，或点击上方按钮新建");
            empty.setTextSize(13);
            empty.setPadding(16, 32, 16, 16);
            layoutList.addView(empty);
            return;
        }
        for (TransactionTemplate tpl : templates) {
            layoutList.addView(buildItem(tpl));
        }
    }

    private View buildItem(final TransactionTemplate tpl) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_template, layoutList, false);
        TextView tvName = item.findViewById(R.id.tvTemplateName);
        TextView tvDetail = item.findViewById(R.id.tvTemplateDetail);
        Button btnEdit = item.findViewById(R.id.btnEdit);
        Button btnDelete = item.findViewById(R.id.btnDelete);

        tvName.setText(tpl.name);
        String typeText = "Income".equals(tpl.type) ? "收入" : "支出";
        String amountText = tpl.amount > 0
                ? "¥" + String.format(Locale.getDefault(), "%.2f", tpl.amount) : "金额不预设";
        String reasonText = (tpl.reason != null && !tpl.reason.isEmpty()) ? tpl.reason : "备注不预设";
        tvDetail.setText(typeText + " · " + tpl.subCategory + " · " + amountText
                + "\n" + reasonText + (tpl.householdAccounts ? "\n家庭账目" : ""));

        btnEdit.setOnClickListener(v -> showEditDialog(tpl));
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("删除模板")
                .setMessage("确定删除模板「" + tpl.name + "」吗？不影响已记录的账单。")
                .setPositiveButton("删除", (d, w) -> {
                    viewModel.deleteTemplateById(tpl.id);
                    Toast.makeText(this, "模板已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show());
        return item;
    }

    /** 新建/编辑模板对话框 */
    private void showEditDialog(final TransactionTemplate existing) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_template_edit, null);
        final EditText etName = form.findViewById(R.id.etTemplateName);
        final Spinner spType = form.findViewById(R.id.spinnerTemplateType);
        final Spinner spCategory = form.findViewById(R.id.spinnerTemplateCategory);
        final EditText etAmount = form.findViewById(R.id.etTemplateAmount);
        final EditText etReason = form.findViewById(R.id.etTemplateReason);
        final CheckBox cbHousehold = form.findViewById(R.id.cbTemplateHousehold);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{"支出", "收入"});
        spType.setAdapter(typeAdapter);

        final String[][] categoryHolder = new String[1][];
        Runnable refreshCategories = () -> {
            String type = spType.getSelectedItemPosition() == 1 ? "Income" : "Expense";
            categoryHolder[0] = CategoryManager.getCategories(type);
            spCategory.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, categoryHolder[0]));
        };
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshCategories.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        refreshCategories.run();

        if (existing != null) {
            etName.setText(existing.name);
            spType.setSelection("Income".equals(existing.type) ? 1 : 0);
            if (existing.amount > 0) {
                etAmount.setText(String.format(Locale.getDefault(), "%.2f", existing.amount));
            }
            etReason.setText(existing.reason);
            cbHousehold.setChecked(existing.householdAccounts);
            spType.post(() -> {
                for (int i = 0; i < categoryHolder[0].length; i++) {
                    if (categoryHolder[0][i].equals(existing.subCategory)) {
                        spCategory.setSelection(i);
                        break;
                    }
                }
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "新建模板" : "编辑模板")
                .setView(form)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "请填写模板名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount = 0;
                    String amountStr = etAmount.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        try {
                            amount = Double.parseDouble(amountStr);
                            if (amount < 0) throw new NumberFormatException();
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "金额格式不正确", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    TransactionTemplate tpl = existing != null ? existing : new TransactionTemplate();
                    tpl.name = name;
                    tpl.type = spType.getSelectedItemPosition() == 1 ? "Income" : "Expense";
                    tpl.subCategory = spCategory.getSelectedItem().toString();
                    tpl.amount = amount;
                    tpl.reason = etReason.getText().toString().trim();
                    tpl.householdAccounts = cbHousehold.isChecked();

                    if (existing == null) {
                        viewModel.saveTemplate(tpl);
                    } else {
                        viewModel.updateTemplate(tpl);
                    }
                    Toast.makeText(this, "模板已保存", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));
        dialog.show();
    }
}
