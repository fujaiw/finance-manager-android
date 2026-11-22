package com.example.financemanager.model;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigFileManager {
    private static final String CONFIG_FILE_NAME = "categories.ini";

    public static File getConfigFile(Context context) {
        return new File(context.getFilesDir(), CONFIG_FILE_NAME);
    }

    public static void initDefaultConfig(Context context) {
        File configFile = getConfigFile(context);
        if (!configFile.exists()) {
            writeDefaultConfig(context);
        }
    }

    private static void writeDefaultConfig(Context context) {
        StringBuilder sb = new StringBuilder();

        sb.append("[ExpenseCategories]\n");
        sb.append("餐饮 = [吃],#FF6B6B\n");
        sb.append("交通 = [行],#4ECDC4\n");
        sb.append("购物 = [购],#45B7D1\n");
        sb.append("房租 = [住],#96CEB4\n");
        sb.append("日常 = [用],#FFEAA7\n");
        sb.append("娱乐 = [乐],#DDA0DD\n");
        sb.append("教育 = [教],#98D8C8\n");
        sb.append("医疗 = [医],#A29BFE\n");
        sb.append("理财亏损 = [损],#6C5CE7\n");
        sb.append("\n");

        sb.append("[IncomeCategories]\n");
        sb.append("工资 = [薪],#00B894\n");
        sb.append("兼职 = [兼],#00CEC9\n");
        sb.append("理财 = [财],#0984E3\n");
        sb.append("红包 = [红],#E17055\n");
        sb.append("奖金 = [奖],#6C5CE7\n");
        sb.append("其他 = [其],#B2BEC3\n");
        sb.append("\n");

        sb.append("[KeywordRules]\n");
        sb.append("expense:餐饮 = 烧烤,水果,零食,餐饮,午餐,晚餐,早餐,快餐,外卖,饭店,食堂,奶茶,饮料,超市,便利店,小卖部\n");
        sb.append("expense:交通 = 油费,车载,滴滴,打车,地铁,公交,出租车,火车,高铁,飞机,停车,过路费,保养,维修\n");
        sb.append("expense:日常 = 话费,流量,会员,宽带,水电费,物业费,日用品,洗漱用品,护肤,化妆品,理发,干洗\n");
        sb.append("expense:购物 = 购物,衣服,鞋子,包,京东,淘宝,天猫,拼多多,唯品会,电器,电子产品\n");
        sb.append("expense:医疗 = 保险,医疗,医院,药品,挂号,门诊,体检,药店,口罩,消毒\n");
        sb.append("expense:娱乐 = 电影,游戏,KTV,酒吧,咖啡,旅游,健身,游泳,球类,演唱会,话剧\n");
        sb.append("expense:教育 = 学费,培训,书籍,课程,考试,报名,教材,文具,补习,辅导\n");
        sb.append("expense:房租 = 房租,租金,房贷,住房,房租费,中介费\n");
        sb.append("expense:理财亏损 = 利息\n");
        sb.append("\n");
        sb.append("income:工资 = 工资,底薪,提成,补贴,餐补,交通补,全勤奖\n");
        sb.append("income:兼职 = 兼职,外快,副业,私单,接单,外包\n");
        sb.append("income:理财 = 理财,利息,证券,基金,股票,分红,股息,投资收益\n");
        sb.append("income:红包 = 红包,返现,代金券,优惠券,奖励金\n");
        sb.append("income:奖金 = 奖金,年终奖,项目奖,绩效,年中奖\n");
        sb.append("income:其他 = 退款,补偿,赔偿,礼金,礼尚往来\n");

        writeConfigFile(context, sb.toString());
    }

    public static void writeConfigFile(Context context, String content) {
        try {
            FileOutputStream fos = context.openFileOutput(CONFIG_FILE_NAME, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            writer.write(content);
            writer.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readConfigFile(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = context.openFileInput(CONFIG_FILE_NAME);
            InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            bufferedReader.close();
            reader.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static Map<String, String[]> parseCategories(String configContent, String section) {
        Map<String, String[]> categories = new HashMap<>();
        boolean inSection = false;

        for (String line : configContent.split("\n")) {
            line = line.trim();

            if (line.startsWith("[") && line.endsWith("]")) {
                inSection = line.equals("[" + section + "]");
                continue;
            }

            if (!inSection || line.isEmpty()) {
                continue;
            }

            int equalsIndex = line.indexOf("=");
            if (equalsIndex > 0) {
                String name = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();

                String[] parts = value.split(",");
                if (parts.length >= 2) {
                    categories.put(name, new String[]{parts[0], parts[1]});
                } else if (parts.length == 1) {
                    categories.put(name, new String[]{parts[0], "#B2BEC3"});
                }
            }
        }

        return categories;
    }

    public static Map<String, String[]> parseKeywordRules(String configContent, String categoryType) {
        Map<String, String[]> rules = new HashMap<>();
        boolean inSection = false;
        String prefix = categoryType.toLowerCase() + ":";

        for (String line : configContent.split("\n")) {
            line = line.trim();

            if (line.startsWith("[") && line.endsWith("]")) {
                inSection = line.equals("[KeywordRules]");
                continue;
            }

            if (!inSection || line.isEmpty()) {
                continue;
            }

            if (!line.startsWith(prefix)) {
                continue;
            }

            int equalsIndex = line.indexOf("=");
            if (equalsIndex > 0) {
                String category = line.substring(prefix.length(), equalsIndex).trim();
                String keywordsStr = line.substring(equalsIndex + 1).trim();

                String[] keywords = keywordsStr.split(",");
                rules.put(category, keywords);
            }
        }

        return rules;
    }

    public static String buildConfigContent(Map<String, String[]> expenseCategories,
                                            Map<String, String[]> incomeCategories,
                                            Map<String, String[]> expenseRules,
                                            Map<String, String[]> incomeRules) {
        StringBuilder sb = new StringBuilder();

        sb.append("[ExpenseCategories]\n");
        for (Map.Entry<String, String[]> entry : expenseCategories.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()[0])
                    .append(",").append(entry.getValue()[1]).append("\n");
        }
        sb.append("\n");

        sb.append("[IncomeCategories]\n");
        for (Map.Entry<String, String[]> entry : incomeCategories.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()[0])
                    .append(",").append(entry.getValue()[1]).append("\n");
        }
        sb.append("\n");

        sb.append("[KeywordRules]\n");
        for (Map.Entry<String, String[]> entry : expenseRules.entrySet()) {
            sb.append("expense:").append(entry.getKey()).append(" = ")
                    .append(String.join(",", entry.getValue())).append("\n");
        }
        sb.append("\n");
        for (Map.Entry<String, String[]> entry : incomeRules.entrySet()) {
            sb.append("income:").append(entry.getKey()).append(" = ")
                    .append(String.join(",", entry.getValue())).append("\n");
        }

        return sb.toString();
    }
}