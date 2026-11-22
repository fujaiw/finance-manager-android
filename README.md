# 财务管理应用 FinanceManager

一款基于 Android 平台的个人/家庭财务管理应用，支持收支记录、智能分类、数据统计图表、预算管理以及 Excel 导入导出。

- **当前版本**：1.3.0（versionCode 16）
- **适用系统**：Android 8.0（API 26）及以上，已适配到 Android 16（API 36）
- **架构模式**：MVVM + Room 数据库

---

# 第一部分：用户使用说明

## 一、功能总览

| 模块 | 功能 |
|------|------|
| 记账 | 记录日常收入/支出，支持备注、家庭账目标记、作废标记 |
| 快捷模板 | 把高频交易存为模板，记账时一键填充，支持新建/编辑/删除 |
| 交易标签 | 自定义多标签，一笔账可打多个标签，点标签即可筛选，支持与搜索组合 |
| 智能分类 | 输入消费原因时自动匹配分类，并持续学习你的使用习惯 |
| 搜索筛选 | 首页关键词搜索 + 日期/金额/类型/分类/账户/标签/作废多维组合筛选 |
| 首页统计 | 今日、本月、本年及全部收入/支出汇总，个人账与家庭账分开统计 |
| 资产总览 | 起始资金设置、当前净资产卡片、近 12 个月资产趋势折线图（可全屏） |
| 图表分析 | 月度收支趋势折线图、分类占比饼图、家庭/个人收支对比柱状图，支持全屏查看 |
| 异常提醒 | 支出显著高于该分类历史水平时提交前弹窗提醒，可在设置中开关 |
| 预算管理 | 设置月度预算，显示使用进度与每日可用额度 |
| 数据导出 | 将今日/本月/全部数据导出为 .xlsx 文件 |
| 数据导入 | 从 Excel 文件批量导入交易记录，按毫秒级时间戳自动去重 |
| 分类管理 | 查看、修改、删除、清空已学习的分类关键词规则 |

## 二、记账操作

1. 点击首页右下角 **“+”** 按钮，选择记一笔**支出**或**收入**。
2. 填写**金额**和**原因**（例如“打车回家”“工资”）。
3. 输入原因时，系统会**自动匹配分类**；如不准确，可手动下拉选择正确分类。
4. 可勾选**家庭账目**（不勾选则视为个人账）。
5. 可在**标签区**给这笔账勾选一个或多个标签，也可点“＋ 新标签”即时创建（详见第九章）。
6. 想用固定组合快速记账时，可从顶部**快捷模板**下拉选择，自动填好分类/金额/原因/家庭账，再按需修改（详见第八章）；也可勾选“保存为快捷模板”把本次内容存成新模板。
7. 点击确认保存。若这笔支出明显超出该分类日常水平，会先弹出**异常提醒**，可选择“仍要保存”或返回修改（详见第十章）。

**智能分类规则**：

- 手动选择过的分类会被系统记住，下次输入相同原因时自动套用；
- 自动匹配不会覆盖你本次手动做出的选择；
- 同一个关键词在同一类型（支出/收入）中只会对应一个分类。

**作废记录**：记录可标记为“作废”。作废记录仍保留在数据库中，但不计入统计，导出时也会被自动过滤。

## 三、统计与图表

- 底部切换到**统计**页面，查看月度收支趋势折线图、分类占比饼图与家庭/个人收支对比柱状图；
- 点击饼图可进入**全屏模式**查看明细（关闭按钮位于右上角）。

## 四、导出 Excel（重点说明）

### 1. 操作步骤

1. 底部切换到**导出**页面；
2. 按时间范围选择：**导出今日数据 / 导出本月数据 / 导出全部数据**；
3. 等待提示“导出成功”。

### 2. 文件保存在哪里

所有导出的 xlsx 文件统一保存在手机公共**下载**目录下的独立文件夹中：

```
下载/FinanceManager/finance_类型_时间戳.xlsx
```

完整路径通常为：

```
内部存储/Download/FinanceManager/finance_all_20261011_153000.xlsx
```

**如何在手机上找到文件**：

- 打开系统自带的**“文件管理”App → 下载（Download）分类 → FinanceManager 文件夹**；
- 或直接打开系统**“下载/下载管理”App** 查看；
- 文件也可以直接发送到微信、QQ、电脑，或用 WPS / Office 打开。

> 说明：文件保存在公共下载目录，**不再保存在 `Android/data` 应用私有目录中**，因此不会再出现“文件在手机里但找不到”的问题。

### 3. 是否需要授权

- **Android 10 及以上（绝大多数手机）**：通过系统标准接口写入下载目录，**无需授予任何存储权限**，点击导出即可完成；
- **Android 8 / Android 9 老设备**：需要授予“存储”权限。首次导出时若未授权，应用会自动跳转到权限设置页，允许后返回重新导出即可。

### 4. 文件格式

- 格式：`.xlsx`（Excel 2007+）
- 文件名：`finance_` + 类型（today/month/all）+ `_` + 年月日_时分秒
- 表头列：日期时间、金额、类型、原因、家庭账目、二级分类、作废标记（与导入格式完全一致）
- 已作废的记录不会被导出。

## 五、导入 Excel

1. 进入**设置**页面，点击**导入数据**；
2. 在系统文件选择器中选择要导入的 `.xlsx` 文件；
3. 应用解析后按毫秒级时间戳去重，提示「新增 X 条，跳过重复 Y 条」。

导入文件需包含以下列（顺序与导出文件一致）：

| 列 | 内容 | 说明 |
|----|------|------|
| 1 | 日期时间 | 格式 `yyyy-MM-dd HH:mm:ss.SSS`（毫秒级，与 PC 端对齐）；兼容分钟级旧格式导入，自动补毫秒序号 |
| 2 | 金额 | 数字，可带 ¥ 或千分位逗号 |
| 3 | 类型 | 支出/收入（或 Expense/Income） |
| 4 | 原因/备注 | 文本 |
| 5 | 家庭账目 | 是/否、household_accounts/空、true/false、1/0 |
| 6 | 二级分类 | 如 餐饮、交通、工资 |
| 7 | 作废标记 | 是/否、canceled/空、true/false、1/0 |

> 去重规则：以毫秒级 `日期时间` 为唯一键。库里已存在相同时间戳、或同一文件内出现相同时间戳的行会被跳过，不会重复入库，与 PC 端导入逻辑完全一致。

> 小技巧：先使用“导出”功能导出一份文件，按其格式填写后再导入，可避免格式不匹配。

## 六、预算管理

在设置中设置**月度预算**后，首页会显示预算使用进度；跨月后应用会提醒为新月份设置预算。

## 七、分类规则管理

设置页 → **管理分类规则**：

- 查看所有已学习的“关键词 → 分类”对应关系；
- **修改**某条规则的关键词或所属分类（修改后会自动清除该关键词在其他分类下的旧规则）；
- **删除**单条规则；
- **清空**全部已学习规则（内置默认关键词不受影响）。

## 八、快捷记账模板

适合每天重复的开销（如午餐、地铁、咖啡）：

- **保存模板**：记账对话框中填好内容后勾选“保存为快捷模板”，或进入 **设置 → 快捷记账模板 → ＋ 新建模板** 手动维护；
- 模板包含：名称、收支类型、分类、默认金额（可留空不预设）、默认原因（可留空）、是否家庭账目；
- **使用模板**：记账对话框顶部“快捷模板”下拉中选择，即自动填充，填充后仍可任意修改再提交；最近使用的模板排在最前；
- **管理模板**：设置页入口可查看、编辑、删除全部模板。删除模板不影响已经记好的账单。

## 九、交易标签

标签是独立于“分类”的自定义维度，可按场景/用途（如“出差报销”“孩子”“双十一”）组织账单：

- **打标签**：记账对话框标签区可多选，点“＋ 新标签”创建（1–12 个字，重名会自动选中已有标签）；一笔账可挂多个标签；
- **看标签**：首页每条明细下方以青色小标签显示；
- **按标签筛选**：点击明细上的任意标签，立即筛出所有带该标签的记录；该筛选可与顶部关键词搜索、“筛选”按钮里的日期/金额/类型/分类/账户等条件**叠加取交集**，点“清除”恢复；
- **管理标签**：**设置 → 管理标签** 支持新建、重命名、删除。重命名会同步更新历史账单上的标签；删除标签只移除标签，不删除账单。

## 十、消费异常提醒

帮助你察觉“这笔支出不太寻常”：

- 提交一笔**支出**时，系统自动对比该**二级分类**的历史平均金额；
- 当该分类历史记录**至少 3 笔**、且本笔金额达到历史均值的 **3 倍**时，提交前弹窗提示均值、样本数和倍数，可选择“仍要保存”或“再想想”；
- 历史样本不足 3 笔、收入记录、或功能关闭时均不会提醒，避免误报；
- 开关位置：**设置 → 记账提醒 → 消费异常提醒**（默认开启）。

## 十一、常见问题（FAQ）

**Q1：导出后在文件管理器里看不到文件？**
请进入“下载（Download）”分类下的 **FinanceManager** 文件夹查看，而不是 `Android/data` 目录。部分机型文件管理器有几秒的索引延迟，稍等或下拉刷新即可。

**Q2：微信/QQ 发送文件时找不到？**
在发送界面选择“文件 → 手机存储/Download/FinanceManager”即可；也可以在系统文件管理器中长按文件分享。

**Q3：导出时跳转到了设置页面？**
说明当前是 Android 8/9 设备且尚未授予存储权限，在设置页允许存储权限后返回应用重新导出。

**Q4：卸载应用后导出的文件还在吗？**
在。文件保存在公共下载目录，卸载应用不会删除。

**Q5：数据会上传到云端吗？**
不会。所有数据仅保存在本机 Room 数据库中。

**Q6：升级新版本后账单/模板/标签会丢吗？**
不会——前提是设备上已是当前 v3 版本。数据库当前仅支持 v3，已移除 v1/v2 的历史迁移：从 v1/v2 旧版本升级时会重建数据库，旧账单不会被保留。v3 内的时间戳为毫秒级 `yyyy-MM-dd HH:mm:ss.SSS`，模板和标签同样存于数据库，重启不丢失。

**Q7：从旧 Excel 备份导入，为什么历史账单没有标签？**
标签是新版本新增维度，旧备份文件中没有标签列，导入的历史记录默认不带标签，可之后在应用内补充。

## 十二、厂商兼容性

已针对主流国产 ROM 的存储策略做了适配：小米 HyperOS/MIUI、华为 HarmonyOS/EMUI、OPPO ColorOS、vivo OriginOS、荣耀 MagicOS、一加 OxygenOS、realme UI、iQOO UI、魅族 Flyme、三星 OneUI 及 Google 原生系统。

---

# 第二部分：开发者说明

## 一、开发环境

| 项 | 要求 |
|----|------|
| Android Studio | Hedgehog 或更高版本 |
| JDK | 17 |
| compileSdk / targetSdk | 36 |
| minSdk | 26（Android 8.0） |
| Gradle | 8.13（优先使用本地缓存） |
| Android Gradle Plugin | 8.2.2 |

构建（请在 Android Studio 中执行 Make/Run；本项目统一使用本地 Gradle 8.13，`GRADLE_USER_HOME=D:\DevHome\.gradle`）：

```bash
# APK 产物位置（由 AS 构建生成）
app/build/outputs/apk/debug/app-debug.apk
```

> 若在 AS 中遇到 `Could not read workspace metadata from ...\.gradle\caches\...\metadata.bin` 错误，属于 Gradle 转换缓存损坏：在 AS 中执行 **Build → Clean Project** 后重新构建即可；必要时再删除 `D:\DevHome\.gradle\caches\8.13\transforms` 下的损坏条目。注意不要在命令行直接用其他 GRADLE_USER_HOME 跑 gradlew，以免走错缓存目录。

## 二、技术栈与依赖

```groovy
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.multidex:multidex:2.0.1'
implementation 'androidx.room:room-runtime:2.6.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1'
implementation 'org.apache.poi:poi-ooxml:5.2.5'
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

| 库 | 用途 |
|----|------|
| Room 2.6.1 | 本地数据库 ORM |
| Apache POI 5.2.5 | xlsx 导入导出（纯 Java 实现） |
| MPAndroidChart v3.1.0 | 折线图、饼图、柱状图 |
| Material 1.12.0 | UI 组件 |
| multidex | 突破 64K 方法数限制（POI 体积较大） |

## 三、代码结构

```
app/src/main/java/com/example/financemanager/
├── MainActivity.java              # 主界面：底部导航 + 新增交易对话框（模板/标签/异常提醒）
├── HomeFragment.java              # 首页：汇总卡片 + 搜索筛选 + 标签筛选 + 明细 + 资产趋势
├── StatisticsFragment.java        # 统计页：趋势/饼图/家庭个人收支对比柱状图
├── ExportFragment.java            # 导出页：时间范围筛选 + 后台导出
├── SettingsFragment.java          # 设置页：导入、预算、模板/标签入口、异常提醒开关
├── CategoryManagerActivity.java   # 分类管理独立页面
├── TemplateManagerActivity.java   # 快捷记账模板管理页
├── TagManagerActivity.java        # 交易标签管理页
├── ChartFullscreenActivity.java   # 图表全屏查看（资产趋势自行查库绘图）
├── ExcelExporter.java             # Excel 导出器（MediaStore 方案）
├── ExcelImporter.java             # Excel 导入器（SAF Uri 读取）
├── TestBuild.java                 # 构建/调试辅助类
├── adapter/
│   └── TransactionAdapter.java    # 明细列表适配器
├── database/
│   ├── Transaction.java              # 交易记录实体
│   ├── TransactionDao.java           # 交易 DAO（搜索/聚合/异常均值）
│   ├── TransactionTemplate.java      # 记账模板实体
│   ├── TemplateDao.java              # 模板 DAO
│   ├── TransactionTag.java           # 标签实体
│   ├── TransactionTagCrossRef.java   # 交易-标签多对多关联实体
│   ├── TagLink.java                  # 关联投影（id+tag）
│   ├── TagDao.java                   # 标签/关联 DAO
│   ├── CategoryLevel.java            # 分类历史均值投影（异常提醒）
│   └── AppDatabase.java              # Room 数据库（v3，仅支持 v3，无历史迁移）
├── repository/
│   └── TransactionRepository.java # 数据仓库（ExecutorService 异步 + 事务）
├── viewmodel/
│   └── TransactionViewModel.java  # ViewModel（LiveData/switchMap 搜索）
├── model/
│   ├── CategoryManager.java       # 分类与关键词学习规则（SharedPreferences）
│   ├── BudgetManager.java         # 月度预算 + 起始资金 + 异常提醒开关
│   └── ConfigFileManager.java     # 分类配置文件读写
└── util/
    ├── AssetChartHelper.java      # 近12个月资产趋势数据补全与绘图
    └── StoragePermissionUtil.java # 厂商适配与存储权限工具
```

## 四、架构设计

```
┌─────────────┐   observe   ┌──────────────┐            ┌─────────────┐
│ View        │ ──────────► │ ViewModel    │ ─────────► │ Repository  │
│ (Activity/  │ ◄────────── │ (LiveData)   │            │ (异步 Executor)
│  Fragment)  │             └──────────────┘            └─────────────┘
└─────────────┘                                                 │
                                                                ▼
                                                         ┌─────────────┐
                                                         │ Room        │
                                                         │ transactions│
                                                         └─────────────┘
```

- **View**：观察 LiveData，点击事件调用 ViewModel；
- **ViewModel**：持有 UI 数据与统计 LiveData，不持有 Context；
- **Repository**：所有数据库操作通过单线程 `ExecutorService` 后台执行；
- **Room**：表 `transactions`，在 `dateTime/category/subCategory` 上建有索引。

## 五、数据库设计

```sql
CREATE TABLE transactions (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    dateTime          TEXT    NOT NULL,   -- yyyy-MM-dd HH:mm:ss.SSS（毫秒级，与 PC 端对齐）
    amount            REAL    NOT NULL,
    category          TEXT    NOT NULL,   -- Expense / Income
    subCategory       TEXT,
    reason            TEXT,
    householdAccounts INTEGER NOT NULL DEFAULT 0,
    isCanceled        INTEGER NOT NULL DEFAULT 0
);
```

DAO 提供全量查询（LiveData）、按日期收支汇总、月度统计 `MonthlyStats`、分类统计 `CategoryStats` 等聚合查询。统计优先使用 SQL 聚合完成，避免把全表数据拉到内存再分组。

### v2 新增表（模板 / 标签，1.2.0）

```sql
-- 记账模板
CREATE TABLE transaction_templates (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    name              TEXT,                 -- 模板名
    type              TEXT,                 -- Expense / Income
    subCategory       TEXT,
    amount            REAL NOT NULL,        -- 0 表示不预设金额
    reason            TEXT,                 -- 空串表示不预设
    householdAccounts INTEGER NOT NULL,
    createdAt         INTEGER NOT NULL,
    lastUsedAt        INTEGER NOT NULL
);

-- 标签字典（名称唯一索引）
CREATE TABLE transaction_tags (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE
);

-- 交易-标签 多对多关联（复合主键；交易删除时外键级联）
CREATE TABLE transaction_tag_cross_ref (
    transactionId INTEGER NOT NULL,
    tag           TEXT    NOT NULL,
    PRIMARY KEY (transactionId, tag),
    FOREIGN KEY (transactionId) REFERENCES transactions(id) ON DELETE CASCADE
);
CREATE INDEX index_cross_ref_transactionId ON transaction_tag_cross_ref(transactionId);
CREATE INDEX index_cross_ref_tag           ON transaction_tag_cross_ref(tag);
```

**迁移要点（重要经验）**：

- `AppDatabase` 现为 `version = 3`，**仅支持 v3**：已移除 v1→v2、v2→v3 显式迁移，旧版本库（v1/v2）打开时由 `fallbackToDestructiveMigration()` 重建为空库，即放弃对旧版本数据的兼容。
- **v3 时间戳格式**：分钟级 `yyyy-MM-dd HH:mm` 已全面升级为毫秒级 `yyyy-MM-dd HH:mm:ss.SSS`，新记录入库、Excel 导出、Excel 导入均使用该格式，与 PC 端对齐，便于作为去重唯一标识。
- 迁移 SQL 的列可空性必须与实体完全一致：实体中未加 `@NonNull` 的 String 字段，迁移 SQL 必须写成可空 `TEXT`（不能写 `NOT NULL`），否则 Room 打开库做 identity hash 校验时抛异常闪退。
- Room 实体若同时存在无参和带参构造，带参构造必须标注 `@Ignore`；作为复合主键一部分的 String 字段必须 `@NonNull`。
- 一笔交易 + 多个标签的写入在 `Repository.insertWithTags()` 中用 `runInTransaction` 包裹（插交易→缺标签则建标签→批量插关联）。
- 标签改名用“改标签表 + 改关联表”两步事务同步；标签删除先清关联再删字典。

### 关键查询

- **组合搜索** `searchTransactions(...)`：九~十个可选参数，`:param IS NULL OR ...` 模式；标签条件用 `EXISTS (SELECT 1 FROM transaction_tag_cross_ref r WHERE r.transactionId=transactions.id AND r.tag=:tag)` 子查询，天然支持与关键词/日期/金额/分类等条件交集。
- **异常均值** `getCategoryLevel(subCategory)`：`SELECT COUNT(*), AVG(amount)`，返回投影 `CategoryLevel(count, avgAmount)`。

## 六、导出功能实现说明（本次修改重点）

### 1. 历史问题

旧版通过 `StoragePermissionUtil.getRecommendedStoragePath()` 取路径，首选为应用私有外部目录：

```
/storage/emulated/0/Android/data/com.example.financemanager/files/FinanceManager/
```

该目录在多数国产 ROM 的文件管理器中被隐藏，用户很难找到导出的文件。

### 2. 当前方案

导出逻辑集中在 `ExcelExporter`，按系统版本分两条路径：

| 系统版本 | 写入方式 | 目标位置 | 所需权限 |
|----------|----------|----------|----------|
| Android 10+（API 29+） | `MediaStore.Downloads` | 公共 `Download/FinanceManager/` | **无需任何权限** |
| Android 8/9（API 26-28） | `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)` + 直接写文件 | 公共 `Download/FinanceManager/` | WRITE_EXTERNAL_STORAGE |

**MediaStore 写入流程**（`saveViaMediaStore`）：

1. 组装 `ContentValues`：`DISPLAY_NAME`、MIME（`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`）、`RELATIVE_PATH = Download/FinanceManager`、`IS_PENDING=1`；
2. `ContentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ...)` 取得 Uri；
3. `openOutputStream(uri)` 写入 POI 工作簿；
4. 写成功后把 `IS_PENDING` 置 0，文件对用户与其他应用立即可见；
5. 通过查询 `DATA` 列取得真实绝对路径返回给 UI；查询失败时用兜底路径拼接；
6. 写入异常时删除残留的占位记录。

**低版本写入流程**（`saveToPublicDownloads`）：

1. 传统权限检查通过后，在公共 Download 下 `mkdirs()` 创建 `FinanceManager` 文件夹；
2. `FileOutputStream` 写入后调用 `MediaScannerConnection.scanFile(context, paths, mimeTypes, null)` 触发媒体扫描（注意 paths 与 mimeTypes 均为 `String[]`）。

### 3. 关键 API 约定

```java
// 返回文件绝对路径；权限缺失（仅Android 8/9）或写入失败时返回 null
String path = new ExcelExporter(context).exportToExcel(transactions, fileName);
```

- 调用方在**后台线程**执行（`ExportFragment` 使用单线程 ExecutorService）；
- `ExcelExporter.checkPermission()` 在 API 29+ 恒为 true；低版本委托 `StoragePermissionUtil` 检查；
- POI 写 xlsx 时**不要调用** `sheet.autoSizeColumn()`（会引用 AWT 类导致 `NoClassDefFoundError`），当前使用固定列宽 `20 * 256`；第一列为毫秒级时间戳（23 字符），单独加宽到 `25 * 256` 以免显示截断。

### 4. Manifest 权限现状

```xml
<!-- Android 12 及以下的传统存储权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"  android:maxSdkVersion="32"/>
<!-- 历史遗留：全盘文件管理（导出走 MediaStore 后非必需，可按需移除） -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage"/>
```

应用节点保留了 `requestLegacyExternalStorage="true"`，仅对 Android 10 生效；由于 API 29+ 已统一走 MediaStore，新代码不依赖传统存储模型。后续如确认无其他功能依赖全盘权限，可移除 `MANAGE_EXTERNAL_STORAGE`，降低上架审核风险。

## 七、导入功能实现说明

- 入口在 `SettingsFragment`，使用 `ActivityResultContracts` + `ActivityResultLauncher` 调起系统文件选择器（SAF）；
- `ExcelImporter.importFromUri(Uri)` 通过 `ContentResolver.openInputStream` 读取，**无需存储权限**；
- 使用 `WorkbookFactory.create(InputStream)` 自动识别 xls/xlsx；
- 支持金额带 `¥`、千分位逗号；支持日期单元格与文本日期（统一归一化为毫秒级 `yyyy-MM-dd HH:mm:ss.SSS`，分钟级旧数据按分钟顺序补 `.000/.001/...`，避免同分钟记录时间戳重复）；布尔型字段兼容“是/否、true/false、1/0”；
- 单行解析失败跳过该行并计入失败数，不中断整体导入。

## 八、智能分类实现要点

- 内置关键词规则定义在 `CategoryManager`；用户学习结果存于 **SharedPreferences**，按 `类型_分类`（如 `expense_餐饮`）分组；
- 匹配顺序：内置规则 → 已学习规则；
- `MainActivity` 的新增对话框中用 `userManuallySelected` 标志保证自动匹配不覆盖手动选择；用 `isInitialSelection` 过滤 Spinner 的首次回调；
- `getLearnedKeywords()` 每次实时从 SharedPreferences 读取，保证分类管理页与持久化数据一致；
- 规则唯一性：记忆新规则前先删除同类型下该关键词的旧归属。

## 九、v1.2.0 新功能实现要点

### 1. 快捷记账模板
- 实体 `TransactionTemplate` + `TemplateDao`（LiveData 列表、按类型查询、`updateLastUsedAt`）；
- `MainActivity` 用 Activity 级 `TransactionViewModel` 观察全部模板，打开记账对话框时按当前类型过滤并填充下拉；选中后回填金额/原因/家庭账/分类并更新最近使用时间；
- 勾选“保存为快捷模板”时随账单一并写入模板表；独立管理页 `TemplateManagerActivity` 复用一个“新建/编辑”对话框布局。

### 2. 交易标签（多对多）
- 采用“标签字典表 + 关联表”的标准多对多结构（而非在交易表存逗号字符串），便于按标签 SQL 筛选、改名与去重；
- 记账对话框使用 Material `ChipGroup`（多选 + 一个“＋ 新标签”Chip），**未引入任何新依赖**；
- 列表渲染：`TagDao.getAllTagLinks()` 一次性查全量关联到内存，按 transactionId 分组成 Map，避免逐条查询；明细项在 `item_transaction.xml` 中用自绘圆角小 TextView + `bg_tag_chip.xml` 展示；
- 点标签把 `SearchParams.tag` 注入既有 `switchMap` 搜索链路，与关键词/高级筛选取交集；
- 改名/删除均走事务并同步关联表，删除交易由外键 `ON DELETE CASCADE` 自动清关联。

### 3. 消费异常提醒
- `TransactionDao.getCategoryLevel()` 以 SQL 聚合返回该二级支出分类的笔数与均值；
- 判定常量集中在 ViewModel：`ANOMALY_MIN_SAMPLES=3`、`ANOMALY_MULTIPLIER=3.0`；
- 提交支出时先异步检测，命中则在主线程弹确认框（“仍要保存 / 再想想”），收入不检测；
- 开关持久化在 `BudgetManager`（复用 `BudgetSettings`，key=`anomaly_alert_enabled`，默认开），设置页用 `SwitchCompat` 绑定。

### 4. 版本号展示
设置页版本号改为运行时通过 `PackageManager` 读取 `versionName/versionCode`（API 28 以下回退 `PackageInfo.versionCode`），升级版本只需改 `build.gradle` 的 `versionCode/versionName` 一处。

## 十、扩展指南

**新增一个分类**：在 `CategoryManager` 的静态初始化中同时补充分类（图标、颜色）与默认关键词：

```java
EXPENSE_CATEGORIES.put("新分类", new String[]{"[图]", "#RRGGBB"});
expenseRules.put("新分类", new String[]{"关键词1", "关键词2"});
```

**新增统计维度**：DAO 加聚合查询 → Repository 加异步封装 → ViewModel 暴露 LiveData → Fragment 观察渲染。

## 十一、性能与内存注意事项

- 数据库操作一律走后台 Executor，UI 只观察 LiveData；
- 统计使用 Room/SQL 原生聚合，不在内存中全量分组求和；
- 标签关联一次性查全量后在内存按 id 分组渲染，避免明细列表逐条查标签（N+1）；
- Apache POI 体积与方法数较大，已开启 `multiDexEnabled` 并使用 `useLegacyPackaging true`；
- 导出采用懒触发（点击才在后台构建工作簿），不在启动时预加载。

## 十二、版本历史

| 版本 | 主要内容 |
|------|----------|
| 1.0.0 | 初始版本：记账、统计、图表、导出 |
| 1.1.0 | 智能分类匹配与用户学习；版本号沿用至今 |
| 1.1.1 | 手动选择分类后不被自动匹配覆盖 |
| 1.1.2 | 分类规则管理（查看/修改/删除/清空） |
| 1.1.3 | 全屏饼图关闭按钮移至右上角，修复遮挡 |
| 1.1.4 | 同类型下关键词与分类唯一对应 |
| 1.1.5 | 学习规则持久化一致性修复 |
| 1.1.6 | 导出路径改为公共 `下载/FinanceManager/`（MediaStore 免权限）；首页搜索与高级筛选；资产总览（起始资金+近一年趋势） |
| 1.2.0 | 快捷记账模板、交易标签多对多系统（Chip 多选/点标签筛选/标签管理）、消费异常提醒（3 倍均值+样本兜底+设置开关）；导出/导入 Excel 列顺序统一；设置页版本号动态读取（versionCode 12） |
| 1.2.1 | 时间戳精度升级为毫秒级（入库/导出/导入统一 `yyyy-MM-dd HH:mm:ss.SSS`，与 PC 端对齐）；Room 升级 v3 且仅支持 v3（移除 v1/v2 迁移，旧库升级会重建为空库）；修复首页搜索卡顿与无响应（结果渲染上限 200 条、日期展示改为字符串截取、关键词未变化不重复查询） |
| 1.2.2 | 导入 Excel 增加按毫秒级时间戳去重：库内已存在或同文件内重复的时间戳自动跳过，导入后提示「新增 X 条，跳过重复 Y 条」，与 PC 端导入逻辑一致 |
| 1.2.3 | 修复 PC 端导出文件导入安卓时「家庭账目」「作废标记」丢失：布尔值解析新增对 `household_accounts` / `canceled` 的兼容，与 PC 端 `_parse_flag` 双向对齐 |
| 1.3.0 | 统计页新增家庭/个人收支对比柱状图（BarChart），与 PC 端 `show_household_comparison_chart` 功能对齐；PC 端同步新增记账模板、标签系统、全部记账详细视图，完成功能双向对齐 |

---

**最后更新**: 2026-11-15