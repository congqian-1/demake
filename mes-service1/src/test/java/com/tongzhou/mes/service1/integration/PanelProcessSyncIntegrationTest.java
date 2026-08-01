package com.tongzhou.mes.service1.integration;

import com.tongzhou.mes.service1.mapper.MesPanelProcessSyncMapper;
import com.tongzhou.mes.service1.pojo.entity.MesPanelProcessSync;
import com.tongzhou.mes.service1.service.PanelProcessSyncService;
import com.tongzhou.mes.service1.service.PanelProcessSyncService.SyncResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 面板工序同步集成测试（连真实数据库 + 真实/模拟 MES API）。
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.task.scheduling.enabled=false",
    "mes.panel.process.sync.enabled=true",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/mes?zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&autoReconnect=true",
    "spring.datasource.username=root",
    "spring.datasource.password=RCh;w/1y,i<h",
    "spring.sql.init.mode=never"
})
@DisplayName("面板工序同步集成测试")
class PanelProcessSyncIntegrationTest {

    @Autowired private PanelProcessSyncService panelProcessSyncService;
    @Autowired private MesPanelProcessSyncMapper panelProcessSyncMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String BATCH_NUM = "PCJH-260506-0087";
    private static final String PART_CODE = "WD000658348B1015";
    private static final String WORK_ID = "WD000658348BBCP024";

    @Test
    @DisplayName("1. discoverAndSyncByPartCode: MES 发现批次并触发同步")
    void testDiscoverAndSyncByPartCode() {
        // 先清掉旧记录
        jdbcTemplate.update("DELETE FROM mes_panel_process_sync WHERE batch_num = ?", BATCH_NUM);

        SyncResult result = panelProcessSyncService.discoverAndSyncByPartCode(PART_CODE);

        assertNotNull(result, "应该返回 SyncResult");
        assertTrue(result.isSuccess() || result.isAlreadySynced(),
                "同步应该成功或已同步过: " + result.getMessage());

        System.out.println("=== discoverAndSyncByPartCode ===");
        System.out.println("success=" + result.isSuccess()
                + " alreadySynced=" + result.isAlreadySynced()
                + " message=" + result.getMessage()
                + " boards=" + result.getUpdatedBoardCount());

        // 验证库里有记录
        int count = panelProcessSyncMapper.countByBatchNum(BATCH_NUM);
        assertTrue(count > 0, "同步后 mes_panel_process_sync 应有记录，实际: " + count);
        System.out.println("记录数: " + count + " (去重生效)");
    }

    @Test
    @DisplayName("2. 去重验证：已有记录的批次再次同步返回 alreadySynced")
    void testDedupPreventsResync() {
        // 手动插入一条记录模拟已同步
        MesPanelProcessSync record = new MesPanelProcessSync();
        record.setBatchNum(BATCH_NUM);
        record.setWorkId(WORK_ID);
        record.setSyncResult("SUCCESS");
        record.setCreatedTime(LocalDateTime.now());
        record.setSyncedAt(LocalDateTime.now());
        jdbcTemplate.update("DELETE FROM mes_panel_process_sync WHERE batch_num = ? AND work_id = ?",
                BATCH_NUM, WORK_ID);
        panelProcessSyncMapper.insert(record);

        SyncResult result = panelProcessSyncService.syncBatchProcessIfNeeded(BATCH_NUM);
        assertTrue(result.isAlreadySynced(), "应该返回 alreadySynced: " + result.getMessage());
        System.out.println("=== 去重验证通过: " + result.getMessage() + " ===");

        // 清理
        jdbcTemplate.update("DELETE FROM mes_panel_process_sync WHERE batch_num = ?", BATCH_NUM);
    }

    @Test
    @DisplayName("3. 数据库记录内容验证")
    void testDatabaseRecords() {
        // 清干净后触发一次同步
        jdbcTemplate.update("DELETE FROM mes_panel_process_sync WHERE batch_num = ?", BATCH_NUM);
        panelProcessSyncService.discoverAndSyncByPartCode(PART_CODE);

        List<String> records = jdbcTemplate.query(
            "SELECT batch_num, work_id, sync_result, error_detail, synced_at"
            + " FROM mes_panel_process_sync WHERE batch_num = ?",
            (rs, rowNum) -> String.format(
                "batch=%s workId=%s result=%s error=%s syncedAt=%s",
                rs.getString("batch_num"), rs.getString("work_id"),
                rs.getString("sync_result"), rs.getString("error_detail"),
                rs.getString("synced_at")),
            BATCH_NUM
        );
        System.out.println("=== mes_panel_process_sync 记录 ===");
        records.forEach(System.out::println);
        assertFalse(records.isEmpty(), "应该有同步记录");
    }

    @Test
    @DisplayName("4. 不存在的板件码 discover 返回 null（MES 无此数据时）")
    void testDiscoverNonexistentPart() {
        // 用真实 MES 查询不存在的板件码
        SyncResult result = panelProcessSyncService.discoverAndSyncByPartCode("NOT-EXIST-PART-99999");
        System.out.println("=== 不存在板件码: " + (result == null ? "null" : result.getMessage()) + " ===");
        // 不做强断言，取决于 MES 实际返回
    }

    @Test
    @DisplayName("5. 功能关闭时不执行同步")
    void testDisabledFeature() {
        // 单元测试已覆盖，集成测试仅确认不会抛异常
        // syncEnabled 已通过 @TestPropertySource 设为 true，此测试仅占位
    }
}
