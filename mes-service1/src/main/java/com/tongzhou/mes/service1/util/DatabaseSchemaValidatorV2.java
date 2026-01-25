package com.tongzhou.mes.service1.util;

import java.sql.*;

/**
 * 数据库表结构验证工具 V2
 * 检查所有表是否存在，并验证表结构
 */
public class DatabaseSchemaValidatorV2 {

    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/mes?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "test123456";

    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("数据库表结构验证工具 V2");
        System.out.println("========================================\n");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("✓ 数据库连接成功\n");

            // 首先列出所有表
            listAllTables(conn);

            // 验证每个表的结构
            System.out.println("\n========================================");
            System.out.println("详细表结构验证");
            System.out.println("========================================\n");

            validateTable(conn, "mes_batch", "批次表");
            validateTable(conn, "mes_optimizing_file", "优化文件表");
            validateTable(conn, "mes_work_order", "工单表");
            validateTable(conn, "mes_prepackage_order", "预包装订单表");
            validateTable(conn, "mes_package", "包件表");
            validateTable(conn, "mes_box_code", "箱码表");
            validateTable(conn, "mes_board", "板件表");
            validateTable(conn, "mes_work_report", "报工表");
            validateTable(conn, "mes_work_order_correction_log", "工单修正日志表");

            System.out.println("\n========================================");
            System.out.println("验证完成");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void listAllTables(Connection conn) throws SQLException {
        System.out.println("数据库中的所有表:");
        System.out.println("----------------------------------------");
        
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
        
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            System.out.println("  - " + tableName);
        }
        System.out.println();
    }

    private static void validateTable(Connection conn, String tableName, String tableDesc) throws SQLException {
        System.out.println("验证表: " + tableName + " (" + tableDesc + ")");
        System.out.println("----------------------------------------");

        DatabaseMetaData metaData = conn.getMetaData();
        
        // 检查表是否存在
        ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
        if (!tables.next()) {
            System.out.println("❌ 表不存在！");
            System.out.println();
            return;
        }

        // 获取表字段
        ResultSet columns = metaData.getColumns(null, null, tableName, null);

        System.out.printf("%-35s %-20s %-10s\n", "字段名", "类型", "可空");
        System.out.println("---------------------------------------------------------");

        boolean hasColumns = false;
        while (columns.next()) {
            hasColumns = true;
            String columnName = columns.getString("COLUMN_NAME");
            String columnType = columns.getString("TYPE_NAME");
            int columnSize = columns.getInt("COLUMN_SIZE");
            String nullable = columns.getString("IS_NULLABLE").equals("1") ? "YES" : "NO";

            String typeDisplay = columnSize > 0 ? columnType + "(" + columnSize + ")" : columnType;
            System.out.printf("%-35s %-20s %-10s\n", columnName, typeDisplay, nullable);
        }

        if (!hasColumns) {
            System.out.println("⚠️  表存在但没有字段");
        }

        System.out.println();
    }
}
