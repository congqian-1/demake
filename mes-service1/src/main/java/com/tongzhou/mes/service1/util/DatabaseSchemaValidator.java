package com.tongzhou.mes.service1.util;

import java.sql.*;

/**
 * 数据库表结构验证工具
 * 用于验证数据库表结构与实体类是否匹配
 */
public class DatabaseSchemaValidator {

    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/mes?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "test123456";

    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("数据库表结构验证工具");
        System.out.println("========================================\n");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("✓ 数据库连接成功\n");

            // 验证所有表的结构
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
            System.out.println("表结构验证完成");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void validateTable(Connection conn, String tableName, String tableDesc) throws SQLException {
        System.out.println("验证表: " + tableName + " (" + tableDesc + ")");
        System.out.println("----------------------------------------");

        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, tableName, null);

        System.out.printf("%-35s %-15s %-10s\n", "字段名", "类型", "可空");
        System.out.println("------------------------------------------------------");

        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String columnType = columns.getString("TYPE_NAME");
            int columnSize = columns.getInt("COLUMN_SIZE");
            String nullable = columns.getString("IS_NULLABLE").equals("1") ? "YES" : "NO";

            String typeDisplay = columnSize > 0 ? columnType + "(" + columnSize + ")" : columnType;
            System.out.printf("%-35s %-15s %-10s\n", columnName, typeDisplay, nullable);
        }
        System.out.println();
    }
}
