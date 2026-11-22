package com.example.financemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.financemanager.database.TransactionTag;
import com.example.financemanager.viewmodel.TransactionViewModel;

import java.util.List;

/**
 * 标签管理：新建 / 重命名 / 删除用户自定义标签。
 * 删除或改名会同步维护交易-标签关联（见 TagDao）。
 */
public class TagManagerActivity extends AppCompatActivity {

    private TransactionViewModel viewModel;
    private LinearLayout layoutList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_manager);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("标签管理");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        layoutList = findViewById(R.id.layoutTagList);
        findViewById(R.id.btnAddTag).setOnClickListener(v -> showInputDialog("新建标签", "", name ->
                viewModel.createTag(name, success -> runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "标签已创建", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "标签已存在或名称无效", Toast.LENGTH_SHORT).show();
                    }
                }))));

        viewModel.getAllTags().observe(this, this::render);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void render(List<TransactionTag> tags) {
        layoutList.removeAllViews();
        if (tags == null || tags.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("还没有标签，记账时可在对话框中创建，或点击上方按钮新建");
            empty.setTextSize(13);
            empty.setPadding(16, 32, 16, 16);
            layoutList.addView(empty);
            return;
        }
        for (TransactionTag tag : tags) {
            layoutList.addView(buildItem(tag.name));
        }
    }

    private View buildItem(final String name) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_tag, layoutList, false);
        TextView tvName = item.findViewById(R.id.tvTagName);
        tvName.setText("# " + name);

        item.findViewById(R.id.btnRename).setOnClickListener(v ->
                showInputDialog("重命名标签", name, newName ->
                        viewModel.renameTag(name, newName, success -> runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "已重命名（历史账单关联同步更新）", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "新名称无效或与已有标签重复", Toast.LENGTH_SHORT).show();
                            }
                        }))));

        item.findViewById(R.id.btnDelete).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("删除标签")
                        .setMessage("确定删除标签「" + name + "」吗？所有账单上的该标签会一并移除，账单本身不受影响。")
                        .setPositiveButton("删除", (d, w) -> {
                            viewModel.deleteTag(name);
                            Toast.makeText(this, "标签已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show());
        return item;
    }

    /** 单行文本输入对话框 */
    private void showInputDialog(String title, String initial, InputConfirmAction action) {
        final EditText input = new EditText(this);
        input.setText(initial);
        if (initial != null && !initial.isEmpty()) {
            input.setSelection(initial.length());
        }
        input.setHint("标签名称（1-12 个字）");
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty() || name.length() > 12) {
                        Toast.makeText(this, "标签名需为 1-12 个字", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    action.onConfirm(name);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private interface InputConfirmAction {
        void onConfirm(String name);
    }
}
