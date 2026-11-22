# FinanceManager - 财务管理应用 - 开发者文档

## 项目概述

一款基于 Android 平台开发的个人/家庭财务管理应用，支持收支记录、分类管理、数据统计、Excel导入导出等功能。采用 MVVM 架构模式，使用 Room 数据库存储数据。

## 技术栈

| 技术                  | 版本     | 说明               |
| ------------------- | ------ | ---------------- |
| Android SDK         | 36     | compileSdk/targetSdk |
| minSdk              | 26     | 最低支持Android 8.0  |
| Gradle              | 8.13   | 构建工具             |
| Android Plugin      | 8.2.2  | Android Gradle插件 |
| Room                | 2.6.1  | 数据库ORM框架         |
| MPAndroidChart      | v3.1.0 | 图表库              |
| Apache POI          | 5.2.5  | Excel操作库         |
| Material Components | 1.12.0 | UI组件库            |

## 代码结构

```
app/src/main/java/com/example/financemanager/
├── MainActivity.java              # 主界面：底部导航 + 新增交易对话框（模板/标签/异常提醒）
├── HomeFragment.java              # 首页（搜索筛选、标签筛选、资产趋势、明细）
├── StatisticsFragment.java        # 统计页面
├── ExportFragment.java            # 导出页面
├── SettingsFragment.java          # 设置（导入、预算、模板/标签入口、异常提醒开关、版本号）
├── CategoryManagerActivity.java   # 分类管理独立页面
├── TemplateManagerActivity.java   # 快捷记账模板管理页
├── TagManagerActivity.java        # 交易标签管理页
├── ChartFullscreenActivity.java   # 图表全屏查看页（资产趋势自行查库绘图）
├── TestBuild.java                 # 构建/调试辅助类
├── ExcelImporter.java             # Excel导入器（SAF Uri + InputStream）
├── ExcelExporter.java             # Excel导出器（MediaStore/公共Download）
├── adapter/
│   └── TransactionAdapter.java    # 交易明细列表适配器
├── database/
│   ├── Transaction.java              # 交易记录实体
│   ├── TransactionDao.java           # 交易DAO（组合搜索/聚合/异常均值）
│   ├── TransactionTemplate.java      # 记账模板实体
│   ├── TemplateDao.java              # 模板DAO
│   ├── TransactionTag.java           # 标签实体
│   ├── TransactionTagCrossRef.java   # 交易-标签关联实体（复合主键+外键级联）
│   ├── TagLink.java                  # 关联投影(transactionId,tag)
│   ├── TagDao.java                   # 标签/关联DAO
│   ├── CategoryLevel.java            # 分类历史均值投影(count,avgAmount)
│   └── AppDatabase.java              # Room v3（仅支持 v3，不再提供 v1/v2 迁移）
├── repository/
│   └── TransactionRepository.java # 数据仓库（Executor 异步 + runInTransaction）
├── viewmodel/
│   └── TransactionViewModel.java  # ViewModel（switchMap 搜索、模板/标签/异常/资产）
├── model/
│   ├── CategoryManager.java       # 分类管理器
│   ├── BudgetManager.java         # 预算 + 起始资金 + 异常提醒开关
│   └── ConfigFileManager.java     # 分类配置文件读写
└── util/
    ├── AssetChartHelper.java      # 近12个月资产趋势数据补全与绘图
    └── StoragePermissionUtil.java # 存储权限/厂商适配工具
```

## 核心类说明

### 1. Transaction（实体类）

**功能**：交易记录数据结构

**字段**：

```java
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String dateTime;        // 日期时间 yyyy-MM-dd HH:mm:ss.SSS（毫秒级，与 PC 端对齐）
    public double amount;          // 金额
    public String category;        // 类型（Expense/Income）
    public String subCategory;     // 二级分类
    public String reason;         // 原因/备注
    public boolean householdAccounts; // 是否家庭账目
    public boolean isCanceled;    // 是否作废
}
```

### 2. TransactionDao（数据访问接口）

**功能**：数据库CRUD操作接口

**主要方法**：

| 方法 | 功能 | 返回值 |
| --- | --- | --- |
| `insert(Transaction)` | 插入记录，返回新生成 id | long |
| `update(Transaction)` | 更新记录 | void |
| `deleteById(int)` | 按 id 删除记录 | void |
| `getAllTransactions()` | 获取所有非作废记录（按日期倒序） | LiveData<List<Transaction>> |
| `getRecentTransactions(int)` | 获取最近 N 条非作废记录 | LiveData<List<Transaction>> |
| `getTransactionsByCategory(String)` | 按类型（Expense/Income）获取记录 | LiveData<List<Transaction>> |
| `getAllTransactionsSync()` | 同步获取所有非作废记录（导出用） | List<Transaction> |
| `getAllDateTimes()` | 同步获取全部时间戳（导入去重用，对应 PC 端 `get_all_timestamps`） | List<String> |
| `getExpenseByDate(String)` | 按日期模式（yyyy-MM-dd%）获取支出合计 | double |
| `getIncomeByDate(String)` | 按日期模式获取收入合计 | double |
| `getTotalExpense()` / `getTotalIncome()` | 全部非作废的支出/收入合计 | double |
| `getHouseholdExpense()` / `getHouseholdIncome()` | 家庭账目支出/收入合计 | double |
| `getPersonalExpense()` / `getPersonalIncome()` | 个人账目支出/收入合计 | double |
| `getYearExpense(String)` / `getYearIncome(String)` | 按年份模式（yyyy%）获取收支合计 | double |
| `getMonthlyStats()` | 按月聚合收支（倒序） | List<MonthlyStats> |
| `getYearlyStats()` | 按年聚合收支（倒序） | List<YearlyStats> |
| `getCategoryStats(String)` | 全量分类统计 | List<CategoryStats> |
| `getCategoryStatsByDate(String, String)` | 按日期模式的分类统计 | List<CategoryStats> |
| `getMonthlyAssetSnapshotsForYear()` | 近 12 个月按月收支快照 | List<MonthlyAssetSnapshot> |
| `searchTransactions(...)` | 关键词/日期/金额/类型/分类/账户/标签/作废 组合搜索（`:param IS NULL OR` 模式，标签用 EXISTS 子查询） | LiveData<List<Transaction>> |
| `getCategoryLevel(String)` | 某二级支出分类的笔数与均值（异常提醒用） | CategoryLevel |

### 3. TransactionRepository（数据仓库）

**功能**：封装数据操作，提供异步数据访问

**主要方法**：

- 交易 CRUD（异步）：`insert()` / `update()` / `deleteById()`；
- `insertWithTags(transaction, tagNames, callback)`：单笔事务内插交易 → 自动建标签 → 批量插关联，回调返回新 id；
- `importWithDedup(transactions, callback)`：导入 Excel 时按毫秒级时间戳去重后插入，回调返回 `(added, skipped)`，与 PC 端 `import_transactions` 语义一致；
- `searchTransactions(...)`：组合搜索（Room 自动后台 + 自动刷新）；
- 收支汇总异步系列：`getTodayExpenseAsync` / `getTodayIncomeAsync` / `getMonthlyExpenseAsync` / `getMonthlyIncomeAsync` / `getYearExpenseAsync` / `getYearIncomeAsync` / `getTotalExpenseAsync` / `getTotalIncomeAsync` / `getHouseholdExpenseAsync` / `getHouseholdIncomeAsync` / `getPersonalExpenseAsync` / `getPersonalIncomeAsync`；
- 统计异步：`getMonthlyStatsAsync` / `getYearlyStatsAsync` / `getCategoryStatsAsync` / `getCategoryStatsByDateAsync` / `getMonthlyAssetSnapshotsAsync`；
- 异常提醒：`getCategoryLevelAsync(subCategory, callback)`；
- 模板：`getAllTemplates()`（LiveData）/ `insertTemplate` / `updateTemplate` / `deleteTemplateById` / `markTemplateUsed`；
- 标签：`getAllTags()` / `getAllTagLinks()`（均为 LiveData）/ `createTagAsync` / `renameTag`（事务内改名两表）/ `deleteTag`（事务内先清关联再删字典）。

### 4. TransactionViewModel（ViewModel）

**功能**：管理UI状态，提供LiveData给UI层

**主要LiveData**：

- 交易列表：`allTransactions`（全量）、`filteredTransactions`（搜索/筛选结果，由 `searchParamsLive` switchMap 切换）；
- 今日/本月/本年/总收支：`todayExpense/Income`、`monthlyExpense/Income`、`yearlyExpense/Income`、`totalExpense/Income`；
- 家庭/个人账目：`householdExpense/Income`、`personalExpense/Income`；
- 统计：`monthlyStats`、`expenseCategoryStats`、`incomeCategoryStats`；
- 资产总览：`currentAsset`（= 起始资金 + 总收入 - 总支出）、`initialAsset`、`monthlyAssetSnapshots`（近 12 个月快照）；
- 模板：`getAllTemplates()`；
- 标签：`getAllTags()`、`getAllTagLinks()`。

**关键能力**：

- `performSearch(SearchParams)` / `clearSearch()`：触发组合搜索；`SearchParams` 字段为 null 表示不限，`tag` 与其他条件取交集；
- `checkExpenseAnomaly(subCategory, amount, callback)`：异常消费检测（样本 ≥3 且金额 ≥ 均值×3，受 `anomaly_alert_enabled` 开关控制）；
- `loadStatistics()` / `loadAssetData()`：刷新统计与资产；图表模式 `chartMode`（月度/年度）影响分类统计的时间范围。

### 5. CategoryManager（分类管理器）

**功能**：管理收支分类和关键词匹配规则，支持自动分类匹配和用户学习

**支出分类**：

| 分类 | 图标 | 颜色 | 关键词 |
|------|------|------|--------|
| 餐饮 | [吃] | #FF6B6B | 烧烤、水果、零食、餐饮、午餐、晚餐、早餐、快餐、外卖、饭店、食堂、奶茶、饮料、超市、便利店、小卖部 |
| 交通 | [行] | #4ECDC4 | 油费、车载、滴滴、打车、地铁、公交、出租车、火车、高铁、飞机、停车、过路费、保养、维修 |
| 购物 | [购] | #45B7D1 | 购物、衣服、鞋子、包、京东、淘宝、天猫、拼多多、唯品会、电器、电子产品 |
| 房租 | [住] | #96CEB4 | 房租、租金、房贷、住房、房租费、中介费 |
| 日常 | [用] | #FFEAA7 | 话费、流量、会员、宽带、水电费、物业费、日用品、洗漱用品、护肤、化妆品、理发、干洗 |
| 娱乐 | [乐] | #DDA0DD | 电影、游戏、KTV、酒吧、咖啡、旅游、健身、游泳、球类、演唱会、话剧 |
| 教育 | [教] | #98D8C8 | 学费、培训、书籍、课程、考试、报名、教材、文具、补习、辅导 |
| 医疗 | [医] | #A29BFE | 保险、医疗、医院、药品、挂号、门诊、体检、药店、口罩、消毒 |
| 理财亏损 | [损] | #6C5CE7 | （无默认关键词） |

**收入分类**：

| 分类 | 图标 | 颜色 | 关键词 |
|------|------|------|--------|
| 工资 | [薪] | #00B894 | 工资、底薪、提成、补贴、餐补、交通补、全勤奖 |
| 兼职 | [兼] | #00CEC9 | 兼职、外快、副业、私单、接单、外包 |
| 理财 | [财] | #0984E3 | 理财、利息、证券、基金、股票、分红、股息、投资收益 |
| 红包 | [红] | #E17055 | 红包、返现、代金券、优惠券、奖励金 |
| 奖金 | [奖] | #6C5CE7 | 奖金、年终奖、项目奖、绩效、年中奖 |
| 其他 | [其] | #B2BEC3 | 退款、补偿、赔偿、礼金、礼尚往来 |

**核心方法**：

| 方法                         | 功能                    |
| -------------------------- | --------------------- |
| `init(Context)`            | 初始化 SharedPreferences |
| `getCategories(String type)` | 获取分类列表（支出/收入）     |
| `matchCategoryByKeyword(String type, String reason)` | 关键词匹配分类 |
| `rememberKeyword(String type, String category, String reason)` | 记忆用户选择的分类对应关系 |
| `getCategoryIcon(String category, String type)` | 获取分类图标 |
| `getCategoryColor(String category, String type)` | 获取分类颜色 |
| `clearLearnedKeywords()`   | 清空学习的关键词           |
| `getLearnedKeywords()`     | 获取已学习的关键词规则（每次从存储读取） |
| `removeLearnedKeyword(String type, String category, String keyword)` | 删除学习的关键词 |
| `updateLearnedKeyword(String type, String oldKeyword, String newKeyword, String newCategory)` | 更新学习的关键词 |

**分类匹配逻辑**：

1. **自动匹配**：输入原因时，系统自动匹配内置关键词规则，更新分类选择（仅当用户未手动选择时）
2. **手动选择**：用户手动选择分类时，系统记忆该原因与分类的对应关系
3. **匹配优先级**：先匹配内置规则，再匹配用户学习的关键词
4. **用户控制**：用户手动选择分类后，自动匹配将不再覆盖用户选择
5. **唯一性保证**：同一关键词在同类型（支出/收入）中只能对应一个分类，修改时会自动删除该关键词在其他分类中的旧规则

**数据持久化机制**：

- 使用 SharedPreferences 存储学习的关键词规则
- 按分类分组存储：`expense_餐饮`、`expense_娱乐` 等
- `getLearnedKeywords()` 方法每次都从 SharedPreferences 重新读取，确保获取最新数据
- 应用重启后数据保持正确

### 6. BudgetManager（预算管理器）

**功能**：管理月度预算、起始资金、异常提醒开关、预算提醒开关（全部存于 `BudgetSettings` SharedPreferences）。

**主要方法**：

| 方法 | 功能 |
| --- | --- |
| `setMonthlyBudget(double)` / `getMonthlyBudget()` | 设置/获取月度预算（默认 5000） |
| `setInitialAsset(double)` / `getInitialAsset()` | 设置/获取起始资金（资产总览用） |
| `setAnomalyAlertEnabled(boolean)` / `isAnomalyAlertEnabled()` | 消费异常提醒开关（默认开） |
| `setReminderEnabled(boolean)` / `isReminderEnabled()` | 预算提醒开关（默认开） |
| `needsNewMonthBudget()` | 检查是否需要为新月份设置预算 |
| `getBudgetSetDate()` | 获取预算设置日期 |
| `getDailyBudget()` | 月预算 / 当月天数 |
| `getRemainingDays()` | 当月剩余天数（含今天） |
| `getRemainingBudget(spent)` | 剩余预算 = 月预算 - 已花 |
| `getDailyRemainingBudget(spent)` | 剩余预算 / 剩余天数 |
| `getBudgetUsagePercent(spent)` | 已花占预算百分比 |
| `isOverBudget(spent)` / `isBudgetWarning(spent)` | 是否超预算 / 是否达到 80% 预警线 |

### 7. ExcelImporter（Excel导入器）

**功能**：从Excel文件导入交易记录

**使用方法**：

```java
ExcelImporter importer = new ExcelImporter(context);
ImportResult result = importer.importFromUri(uri);
```

**支持的Excel格式**：

- 列1：日期时间（`yyyy-MM-dd HH:mm:ss.SSS` 毫秒级；兼容分钟级旧格式，导入时自动补毫秒序号保证唯一）
- 列2：金额
- 列3：类型（支出/收入）
- 列4：原因
- 列5：家庭账目标记
- 列6：二级分类
- 列7：作废标记

### 8. ExcelExporter（Excel导出器）

**功能**：导出交易记录为 .xlsx，统一保存到公共 **下载/FinanceManager** 目录，解决旧版写入 `Android/data/应用私有目录` 导致用户找不到文件的问题。

**使用方法**（须在后台线程调用，UI 层用 Handler 切回主线程）：

```java
ExcelExporter exporter = new ExcelExporter(context);
// 返回文件绝对路径；权限缺失(仅Android8/9)或写入失败时返回 null
String path = exporter.exportToExcel(transactions, fileName);
```

**分版本存储策略**：

| 系统版本 | 写入方式 | 目标位置 | 权限 |
| -------- | -------- | -------- | ---- |
| Android 10+（API 29+） | `MediaStore.Downloads` | `Download/FinanceManager/` | 无需任何权限 |
| Android 8/9（API 26-28） | `getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)` 直写 | `Download/FinanceManager/` | WRITE_EXTERNAL_STORAGE |

**MediaStore 写入流程**（`saveViaMediaStore`）：

1. `ContentValues` 设置 `DISPLAY_NAME`、xlsx MIME、`RELATIVE_PATH=Download/FinanceManager`、`IS_PENDING=1`；
2. `ContentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)` 得到 Uri；
3. `openOutputStream(uri)` 写入 `XSSFWorkbook`；
4. 成功后将 `IS_PENDING` 更新为 0，文件立即可见；
5. 查询 `DATA` 列得到真实路径用于界面展示，查询失败用预期路径兜底；
6. 异常时 `delete(uri)` 清理残留占位记录。

**低版本流程**（`saveToPublicDownloads`）：传统权限检查 → `mkdirs()` 建目录 → `FileOutputStream` 写入 → `MediaScannerConnection.scanFile(context, paths, mimeTypes, null)` 通知媒体扫描（paths、mimeTypes 均为 `String[]`）。

**注意事项**：

- POI 写表时禁止调用 `sheet.autoSizeColumn()`，会引用 AWT 类导致 `NoClassDefFoundError`，当前使用固定列宽 `20 * 256`；第一列为毫秒级时间戳（23 字符），单独加宽到 `25 * 256`；
- `checkPermission()` 在 API 29+ 恒返回 true，低版本委托 `StoragePermissionUtil`；
- Manifest 中保留 `MANAGE_EXTERNAL_STORAGE`（历史遗留，导出走 MediaStore 后非必需，可在确认无其他依赖后移除）。

### 9. ExportFragment（导出页面）

**功能**：按今日/本月/全部过滤非作废记录，在单线程 `ExecutorService` 中调用 `ExcelExporter`，通过主线程 `Handler` 更新结果文案。导出成功时提示"保存位置: 下载/FinanceManager + 文件名 + 完整路径"。

### 10. 模板 / 标签 / 异常提醒（1.2.0 新增）

**快捷记账模板**：
- `TransactionTemplate` 实体 + `TemplateDao`（LiveData 列表、按类型查询、更新最近使用时间）；
- `MainActivity` 在 Activity 级 ViewModel 观察模板，`showAddTransactionDialog(type)` 内按类型过滤填充 Spinner，选中即回填字段并 `markTemplateUsed()`；勾选"保存为模板"时随账单写入；
- 独立管理页 `TemplateManagerActivity` + `dialog_template_edit.xml`（新建/编辑共用）。

**交易标签（多对多）**：
- 实体三件套：`TransactionTag`（字典，name 唯一索引）、`TransactionTagCrossRef`（复合主键 `transactionId+tag`，外键 `ON DELETE CASCADE`，tag 字段 `@NonNull`）、投影 `TagLink`；
- `TagDao`：标签 CRUD、关联批量插入/按交易删除/按标签清理、改名同步两表、`getAllTagLinks()` 全量关联；
- `TransactionDao.searchTransactions` 新增 `tag` 参数，以 `EXISTS` 子查询过滤；`SearchParams.tag` 复用首页 switchMap 搜索链路与其他条件取交集；
- UI：记账对话框用 Material `ChipGroup` 多选 + "＋新标签"（无新增依赖）；明细项 `item_transaction.xml` 用 `bg_tag_chip.xml` 圆角样式自绘 TextView；`HomeFragment` 用 `Map<txId, List<tag>>` 一次性分组渲染；管理页 `TagManagerActivity`。

**消费异常提醒**：
- DAO `getCategoryLevel(subCategory)` → `CategoryLevel(count, avgAmount)`；
- ViewModel 常量 `ANOMALY_MIN_SAMPLES=3`、`ANOMALY_MULTIPLIER=3.0`，`checkExpenseAnomaly()` 仅对支出生效；开关读自 `BudgetManager`（key `anomaly_alert_enabled`，默认 true）；
- `MainActivity` 提交前异步检测，命中在主线程弹 AlertDialog（仍要保存/再想想）。

**版本号**：`SettingsFragment` 用 PackageManager 动态读取，API 28+ 用 `getLongVersionCode()`，低版本回退 `versionCode`。

## 数据库设计

### 表结构

```sql
CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dateTime TEXT NOT NULL,           -- yyyy-MM-dd HH:mm:ss.SSS（毫秒级）
    amount REAL NOT NULL,
    category TEXT NOT NULL,
    subCategory TEXT NOT NULL,
    reason TEXT,
    householdAccounts INTEGER NOT NULL DEFAULT 0,
    isCanceled INTEGER NOT NULL DEFAULT 0
);
```

### 索引

- `idx_dateTime` - 日期时间索引
- `idx_category` - 类型索引
- `idx_subCategory` - 二级分类索引

### v2 新增表（1.2.0：模板 / 标签） + v3 时间戳精度升级

```sql
CREATE TABLE transaction_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT, type TEXT, subCategory TEXT, reason TEXT,
    amount REAL NOT NULL, householdAccounts INTEGER NOT NULL,
    createdAt INTEGER NOT NULL, lastUsedAt INTEGER NOT NULL
);

CREATE TABLE transaction_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE
);

CREATE TABLE transaction_tag_cross_ref (
    transactionId INTEGER NOT NULL,
    tag TEXT NOT NULL,
    PRIMARY KEY (transactionId, tag),
    FOREIGN KEY (transactionId) REFERENCES transactions(id) ON DELETE CASCADE
);
-- 关联表两个外键列各建索引
```

- `@Database(..., version=3)`，**仅支持 v3**：已移除 `MIGRATION_1_2` / `MIGRATION_2_3`，旧版本库（v1/v2）打开时由 `fallbackToDestructiveMigration()` 重建为空库（不保留旧数据）；
- **迁移 SQL 列可空性必须与实体一致**：实体 String 未标 `@NonNull` 时，迁移列须为可空 `TEXT`（曾因误写 NOT NULL 导致旧库升级时 identity 校验失败、进首页闪退）；
- Room 实体带参构造需 `@Ignore`；复合主键中的 String 字段需 `@NonNull`；
- 交易与标签的写入由 `TransactionRepository.insertWithTags()` 在单个 `runInTransaction` 内完成。
- 导入 Excel 的去重键为毫秒级 `dateTime` 单列：先 `getAllDateTimes()` 查全表时间戳建 Set，再逐条判断，库内已存在或同文件内重复的跳过；与 PC 端保持同一语义，不依赖数据库唯一索引。

## 架构设计

### MVVM模式

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│   View      │◄──►│  ViewModel   │◄──►│ Repository  │
│ (Fragment)  │    │              │    │             │
└─────────────┘    └──────────────┘    └─────────────┘
                                              │
                                              ▼
                                         ┌─────────────┐
                                         │  Database   │
                                         │  (Room)     │
                                         └─────────────┘
```

### 数据流向

1. **UI层**：Fragment 观察 ViewModel 的 LiveData
2. **ViewModel**：处理业务逻辑，调用 Repository
3. **Repository**：封装数据操作，使用 ExecutorService 进行异步操作
4. **Database**：Room 数据库进行数据持久化

## 新增交易流程

### 流程说明

```
用户点击"新增支出/收入"
        │
        ▼
弹出交易对话框
        │
        ▼
用户输入金额和原因
        │
        ▼
┌─────────────────────────────┐
│ 系统自动匹配分类（仅当用户  │
│ 未手动选择时）：             │
│ 1. 匹配内置关键词规则        │
│ 2. 匹配用户学习的关键词      │
│ 3. 更新Spinner选中项        │
└─────────────────────────────┘
        │
        ▼
用户可手动选择分类
        │
        ▼
┌─────────────────────────────┐
│ 用户选择分类时：             │
│ 1. 设置手动选择标志          │
│ 2. 后续自动匹配不再覆盖       │
│ 3. 删除同类型中该关键词的旧规则 │
│ 4. 保存原因与分类的对应关系   │
│ 5. 下次输入相同原因自动匹配   │
└─────────────────────────────┘
        │
        ▼
点击确认按钮
        │
        ▼
验证输入 → 创建Transaction（dateTime 由 SimpleDateFormat 按 yyyy-MM-dd HH:mm:ss.SSS 生成，毫秒级） → 插入数据库
```

### 关键代码位置

**MainActivity.java** - 新增交易对话框逻辑：

```java
// 用户手动选择标志
final boolean[] userManuallySelected = {false};
final boolean[] isInitialSelection = {true};

// 监听原因输入，自动匹配分类（仅当用户未手动选择时）
etReason.addTextChangedListener(new TextWatcher() {
    @Override
    public void afterTextChanged(Editable s) {
        if (!userManuallySelected[0]) {
            // 自动匹配逻辑
        }
    }
});

// 监听分类选择，设置手动选择标志
spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (isInitialSelection[0]) {
            isInitialSelection[0] = false;
            return;
        }
        userManuallySelected[0] = true;
    }
});
```

## 分类管理功能

### 功能说明

用户可在设置页面管理已学习的分类规则：

1. **查看规则**：按支出/收入类型查看所有已学习的关键词-分类对应关系
2. **修改规则**：编辑关键词或分类（修改时会自动删除该关键词在其他分类中的旧规则）
3. **删除规则**：删除单条学习规则
4. **清空规则**：清空所有已学习的规则

### 关键特性

- **数据实时读取**：`getLearnedKeywords()` 方法每次都从 SharedPreferences 重新读取，确保显示最新数据
- **规则唯一性**：同一关键词在同类型（支出/收入）中只能对应一个分类
- **自动去重**：修改规则时，系统自动删除该关键词在其他分类中的旧规则
- **持久化保证**：所有修改直接保存到 SharedPreferences，应用重启后数据保持正确

### 关键代码位置

**SettingsFragment.java** - 分类管理对话框：

- `showCategoryManagementDialog()` - 显示分类管理对话框
- `showEditCategoryDialog()` - 显示编辑分类规则对话框

**布局文件**：

- `dialog_category_management.xml` - 分类管理对话框布局
- `item_learned_category.xml` - 已学习规则列表项布局
- `dialog_edit_category.xml` - 编辑分类规则对话框布局

## 图表集成

### MPAndroidChart 使用

**LineChart（月度趋势图）**：

```java
LineChart lineChart = findViewById(R.id.lineChart);
LineDataSet dataSet = new LineDataSet(entries, "标签");
LineData data = new LineData(dataSet);
lineChart.setData(data);
lineChart.invalidate();
```

**PieChart（分类占比图）**：

```java
PieChart pieChart = findViewById(R.id.pieChart);
PieDataSet dataSet = new PieDataSet(entries, "标签");
PieData data = new PieData(dataSet);
pieChart.setData(data);
pieChart.invalidate();
```

**BarChart（家庭/个人收支对比图）**：

统计页通过 `barChartComparison` 分组展示个人账与家庭账的支出、收入。`StatisticsFragment` 观察 `personalExpense`、`householdExpense`、`personalIncome`、`householdIncome` 四个 LiveData，任一数据变化后调用 `updateBarChart()` 重建两组 `BarEntry` 并刷新图表。

```java
BarDataSet expenseSet = new BarDataSet(expenseEntries, "支出");
BarDataSet incomeSet = new BarDataSet(incomeEntries, "收入");
BarData data = new BarData(expenseSet, incomeSet);
data.setBarWidth(0.35f);
barChartComparison.setData(data);
barChartComparison.groupBars(0f, 0.3f, 0.05f);
barChartComparison.invalidate();
```

**全屏饼图布局** (`dialog_chart_fullscreen.xml`)：

- 使用 RelativeLayout 布局
- 关闭按钮位于右上角，避免遮挡图例
- 饼图底部留有120dp边距，确保图例信息完整显示

## 依赖说明

### build.gradle (项目级)

```groovy
dependencies {
    classpath 'com.android.tools.build:gradle:8.2.2'
}
```

### build.gradle (模块级)

```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.multidex:multidex:2.0.1'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'org.apache.poi:poi-ooxml:5.2.5'
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
}
```

## 性能优化

| 优化项        | 实现方式                      |
| ---------- | ------------------------- |
| 异步数据库操作    | 使用 ExecutorService 进行后台操作 |
| LiveData观察 | 自动在主线程更新UI                |
| 分类匹配缓存     | SharedPreferences 持久化学习的关键词 |
| 标签关联渲染     | 全量关联一次查询后内存按 id 分组，避免 N+1 |
| 组合搜索       | Room 后台执行 + switchMap 切换，输入 300ms 防抖 |
| 图表懒加载      | 仅在需要时初始化图表                |

## 扩展开发

### 添加新分类

在 `CategoryManager.java` 的静态初始化块中添加：

```java
EXPENSE_CATEGORIES.put("新分类", new String[]{"[图标]", "#颜色代码"});
expenseRules.put("新分类", new String[]{"关键词1", "关键词2"});
```

### 添加新统计维度

1. 在 `TransactionDao.java` 添加查询方法
2. 在 `TransactionRepository.java` 添加异步封装
3. 在 `TransactionViewModel.java` 添加 LiveData
4. 在 `StatisticsFragment.java` 观察并展示

## 版本历史

| 版本    | 日期      | 更新内容        |
| ----- | ------- | ----------- |
| 1.0.0 | 2024-05 | 初始版本，基础功能实现 |
| 1.1.0 | 2026-05 | 优化分类匹配功能：支持自动匹配和用户学习 |
| 1.1.1 | 2026-05 | 修复分类匹配逻辑：用户手动选择后不会被自动匹配覆盖 |
| 1.1.2 | 2026-05 | 添加分类管理功能：查看、修改、删除已学习规则，支持清空所有规则 |
| 1.1.3 | 2026-05 | 修复饼图按钮遮挡问题：关闭按钮移至右上角，底部留出图例空间 |
| 1.1.4 | 2026-05 | 修复分类规则重复问题：同一关键词在同类型中只能对应一个分类 |
| 1.1.5 | 2026-05 | 修复数据持久化问题：getLearnedKeywords() 每次从存储读取，确保数据一致性 |
| 1.1.6 | 2026-10 | 导出路径迁移公共 `下载/FinanceManager/`（MediaStore 免权限）；首页关键词+多维高级筛选；资产总览（起始资金/当前资产/近12个月趋势） |
| 1.2.0 (versionCode 12) | 2026-10 | 新增快捷记账模板、交易标签多对多系统（Chip 多选/点标签组合筛选/标签管理页）、消费异常提醒（历史均值3倍+至少3笔兜底+设置开关）；统一导出/导入 Excel 列顺序；设置页版本号改为动态读取 |
| 1.2.1 (versionCode 13) | 2026-11 | 时间戳精度升级为毫秒级 `yyyy-MM-dd HH:mm:ss.SSS`（入库/导出/导入，与 PC 端对齐）；Room 升级 v3 且仅支持 v3（移除 v1→v2、v2→v3 迁移，旧库升级走破坏性重建）；修复首页搜索卡顿与无响应（结果渲染上限 200 条、日期展示改为字符串截取、关键词未变化不重复查询） |
| 1.2.3 (versionCode 15) | 2026-11 | 修复 PC 端导出文件导入时「家庭账目」「作废标记」丢失：`parseRow` 的布尔解析新增 `household_accounts` / `canceled` 识别，与 PC 端 `_parse_flag` 双向对齐 |
| 1.2.2 (versionCode 14) | 2026-11 | 导入 Excel 增加按毫秒级时间戳去重（`TransactionRepository.importWithDedup`）：先取 `getAllDateTimes()` 建 Set，库内已存在及同文件内重复的时间戳跳过，回调返回 added/skipped 并提示「新增 X 条，跳过重复 Y 条」；与 PC 端 `import_transactions` 语义一致 |
| 1.3.0 (versionCode 16) | 2026-11 | 统计页新增家庭/个人收支对比柱状图（`BarChart` + 四项收支 LiveData 联动）；完成与 PC 端功能对齐：PC 端同步新增记账模板、交易标签及全部记账详细视图，安卓端沿用已有模板、标签与三态账户筛选 |

## 开发环境

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 36
- Gradle 8.13（本地缓存）

## 构建说明

1. 使用 Android Studio 打开项目
2. 等待 Gradle 同步完成（使用本地缓存，无需下载）
3. 点击 Build > Make Project
4. 连接设备或启动模拟器
5. 点击 Run > Run 'app'

---

**作者**: FinanceManager Team
**创建日期**: 2024
**最后更新**: 2026-11-15