package com.tongzhou.mes.service1.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tongzhou.mes.service1.client.ThirdPartyMesClient;
import com.tongzhou.mes.service1.mapper.MesBatchMapper;
import com.tongzhou.mes.service1.mapper.MesBoardMapper;
import com.tongzhou.mes.service1.mapper.MesBoxCodeMapper;
import com.tongzhou.mes.service1.mapper.MesPackageMapper;
import com.tongzhou.mes.service1.mapper.MesPrepackageOrderMapper;
import com.tongzhou.mes.service1.mapper.MesWorkOrderMapper;
import com.tongzhou.mes.service1.mapper.MesWorkReportMapper;
import com.tongzhou.mes.service1.pojo.dto.BatchPushRequest;
import com.tongzhou.mes.service1.pojo.dto.PrepackageDataDTO;
import com.tongzhou.mes.service1.pojo.dto.WorkReportRequest;
import com.tongzhou.mes.service1.pojo.entity.MesBatch;
import com.tongzhou.mes.service1.pojo.entity.MesBoard;
import com.tongzhou.mes.service1.pojo.entity.MesBoxCode;
import com.tongzhou.mes.service1.pojo.entity.MesPackage;
import com.tongzhou.mes.service1.pojo.entity.MesWorkOrder;
import com.tongzhou.mes.service1.pojo.entity.MesWorkReport;
import com.tongzhou.mes.service1.scheduled.PrePackagePullTask;
import com.tongzhou.mes.service1.service.impl.EmailNotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 按 specs/001-mes-integration/spec.md 的关键验收点进行集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.task.scheduling.enabled=false"
})
class MesIntegrationSpecTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrePackagePullTask prePackagePullTask;

    @Autowired
    private ThirdPartyMesClient thirdPartyMesClient;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MesBatchMapper batchMapper;

    @Autowired
    private MesWorkOrderMapper workOrderMapper;

    @Autowired
    private MesPrepackageOrderMapper prepackageOrderMapper;

    @Autowired
    private MesBoxCodeMapper boxCodeMapper;

    @Autowired
    private MesPackageMapper packageMapper;

    @Autowired
    private MesBoardMapper boardMapper;

    @Autowired
    private MesWorkReportMapper workReportMapper;

    @SpyBean
    private EmailNotificationServiceImpl emailNotificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanupDatabase() {
        // Clean in FK-safe order
        jdbcTemplate.execute("DELETE FROM mes_work_report");
        jdbcTemplate.execute("DELETE FROM mes_part");
        jdbcTemplate.execute("DELETE FROM mes_package");
        jdbcTemplate.execute("DELETE FROM mes_box");
        jdbcTemplate.execute("DELETE FROM mes_prepackage_order");
        jdbcTemplate.execute("DELETE FROM mes_email_notification_config");
        jdbcTemplate.execute("DELETE FROM mes_work_order_correction_log");
        jdbcTemplate.execute("DELETE FROM mes_work_order");
        jdbcTemplate.execute("DELETE FROM mes_optimizing_file");
        jdbcTemplate.execute("DELETE FROM mes_batch");
    }

    @Test
    void story1_batchPush_shouldPersistAndInitStatus() throws Exception {
        String batchNum = unique("BATCH");
        List<String> workIds = Arrays.asList(unique("WO"), unique("WO"));

        BatchPushRequest request = buildBatchRequest(batchNum, workIds);

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.workOrderCount").value(2));

        MesBatch batch = batchMapper.selectOne(
            new LambdaQueryWrapper<MesBatch>().eq(MesBatch::getBatchNum, batchNum));
        assertNotNull(batch);
        assertNotNull(batch.getProductTime());
        assertNotNull(batch.getNestingTime());
        assertEquals("云南线", batch.getYmba014());
        assertEquals("N", batch.getYmba016());

        long workOrderCount = workOrderMapper.selectCount(
            new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getBatchId, batch.getId()));
        assertEquals(2L, workOrderCount);

        List<MesWorkOrder> orders = workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getBatchId, batch.getId()));
        for (MesWorkOrder order : orders) {
            assertEquals("NOT_PULLED", order.getPrepackageStatus());
            assertEquals("RID-1", order.getRouteId());
            assertEquals("N04", order.getOrderType());
            assertNotNull(order.getDeliveryTime());
            assertNotNull(order.getNestingTime());
            assertEquals("云南线", order.getYmba014());
            assertEquals("SA001", order.getYmba015());
            assertEquals("N", order.getYmba016());
            assertEquals("PART-0", order.getPart0());
            assertEquals("COND-0", order.getCondition0());
            assertNotNull(order.getPartTime0());
            assertEquals(1, order.getZuz());
        }
    }

    @Test
    void story1_batchPush_shouldBeIdempotent() throws Exception {
        String batchNum = unique("BATCH");
        String oldWorkId = unique("WO");
        String newWorkId1 = unique("WO");
        String newWorkId2 = unique("WO");

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum, Arrays.asList(oldWorkId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum, Arrays.asList(newWorkId1, newWorkId2)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.workOrderCount").value(2));

        MesBatch batch = batchMapper.selectOne(
            new LambdaQueryWrapper<MesBatch>().eq(MesBatch::getBatchNum, batchNum));
        assertNotNull(batch);

        long countAfterUpdate = workOrderMapper.selectCount(
            new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getBatchId, batch.getId()));
        assertEquals(2L, countAfterUpdate);

        long oldWorkExists = workOrderMapper.selectCount(
            new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getWorkId, oldWorkId));
        assertEquals(0L, oldWorkExists);
    }

    @Test
    void story2_pullPending_shouldPersistAllLevels_andStandardList() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        pushBatch(batchNum, workId);

        prePackagePullTask.pullPrePackageData();

        MesWorkOrder workOrder = waitForWorkOrderStatus(workId, "PULLED", 5000);
        assertEquals("PULLED", workOrder.getPrepackageStatus());

        com.tongzhou.mes.service1.pojo.entity.MesPrepackageOrder order =
            prepackageOrderMapper.selectOne(
                new LambdaQueryWrapper<com.tongzhou.mes.service1.pojo.entity.MesPrepackageOrder>()
                    .eq(com.tongzhou.mes.service1.pojo.entity.MesPrepackageOrder::getWorkId, workId));
        assertNotNull(order);
        assertEquals("ORDER-" + batchNum + "-" + workId, order.getOrderNum());
        assertEquals("Mock Consignor", order.getConsignor());
        assertEquals("CONTRACT-" + batchNum + "-" + workId, order.getContractNo());
        assertEquals(workId, order.getWorkNum());
        assertEquals("Mock Receiver", order.getReceiver());
        assertEquals("13800000000", order.getPhone());
        assertEquals("SHIP-" + batchNum, order.getShipBatch());
        assertEquals("Mock Address", order.getInstallAddress());
        assertEquals("Mock Customer", order.getCustomer());
        assertEquals("Mock Region", order.getReceiveRegion());
        assertEquals("Mock Space", order.getSpace());
        assertEquals("Mock PackType", order.getPackType());
        assertEquals("Mock ProductType", order.getProductType());
        assertEquals(2, order.getPrepackageInfoSize());
        assertEquals(1, order.getTotalSet());
        assertEquals(2, order.getMaxPackageNo());
        assertEquals("PROD-" + batchNum + "-" + workId, order.getProductionNum());

        List<com.tongzhou.mes.service1.pojo.entity.MesBoxCode> boxes = boxCodeMapper.selectList(
            new LambdaQueryWrapper<com.tongzhou.mes.service1.pojo.entity.MesBoxCode>()
                .eq(com.tongzhou.mes.service1.pojo.entity.MesBoxCode::getWorkId, workId)
                .orderByAsc(com.tongzhou.mes.service1.pojo.entity.MesBoxCode::getBoxCode));
        assertEquals(2, boxes.size());
        com.tongzhou.mes.service1.pojo.entity.MesBoxCode box1 = boxes.get(0);
        com.tongzhou.mes.service1.pojo.entity.MesBoxCode box2 = boxes.get(1);
        assertEquals(batchNum + "-" + workId + "-BOX-1", box1.getBoxCode());
        assertEquals("1", box1.getBuilding());
        assertEquals("A", box1.getHouse());
        assertEquals("101", box1.getRoom());
        assertEquals(1, box1.getSetno());
        assertEquals("White", box1.getColor());
        assertEquals(0, box1.getIsDeleted());
        assertEquals(batchNum + "-" + workId + "-BOX-2", box2.getBoxCode());
        assertEquals("1", box2.getBuilding());
        assertEquals("A", box2.getHouse());
        assertEquals("102", box2.getRoom());
        assertEquals(1, box2.getSetno());
        assertEquals("Gray", box2.getColor());
        assertEquals(0, box2.getIsDeleted());

        List<com.tongzhou.mes.service1.pojo.entity.MesPackage> packages = packageMapper.selectList(
            new LambdaQueryWrapper<com.tongzhou.mes.service1.pojo.entity.MesPackage>()
                .eq(com.tongzhou.mes.service1.pojo.entity.MesPackage::getWorkId, workId)
                .orderByAsc(com.tongzhou.mes.service1.pojo.entity.MesPackage::getPackageNo));
        assertEquals(2, packages.size());
        com.tongzhou.mes.service1.pojo.entity.MesPackage pkg1 = packages.get(0);
        com.tongzhou.mes.service1.pojo.entity.MesPackage pkg2 = packages.get(1);
        assertEquals(1, pkg1.getPackageNo());
        assertEquals("地盖", pkg1.getBoxType());
        assertEquals(2, pkg1.getPartCount());
        assertNotNull(pkg1.getLength());
        assertNotNull(pkg1.getWidth());
        assertNotNull(pkg1.getDepth());
        assertNotNull(pkg1.getWeight());
        assertEquals(0, pkg1.getIsDeleted());
        assertEquals(2, pkg2.getPackageNo());
        assertEquals("天地盖", pkg2.getBoxType());
        assertEquals(1, pkg2.getPartCount());
        assertNotNull(pkg2.getLength());
        assertNotNull(pkg2.getWidth());
        assertNotNull(pkg2.getDepth());
        assertNotNull(pkg2.getWeight());
        assertEquals(0, pkg2.getIsDeleted());

        assertEquals(2L, boxCodeMapper.selectCount(
            new LambdaQueryWrapper<com.tongzhou.mes.service1.pojo.entity.MesBoxCode>()
                .eq(com.tongzhou.mes.service1.pojo.entity.MesBoxCode::getWorkId, workId)));
        assertEquals(2L, packageMapper.selectCount(
            new LambdaQueryWrapper<com.tongzhou.mes.service1.pojo.entity.MesPackage>()
                .eq(com.tongzhou.mes.service1.pojo.entity.MesPackage::getWorkId, workId)));
        assertEquals(3L, boardMapper.selectCount(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0)));

        MesBoard board = boardMapper.selectOne(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0)
                .last("LIMIT 1"));
        assertNotNull(board);
        assertNotNull(board.getStandardList());
        assertTrue(board.getStandardList().contains("STD"));
        assertNotNull(board.getItemCode());
        assertNotNull(board.getItemName());
        assertNotNull(board.getMatName());
        assertNotNull(board.getItemLength());
        assertNotNull(board.getItemWidth());
        assertNotNull(board.getItemDepth());
        assertNotNull(board.getXAxis());
        assertNotNull(board.getYAxis());
        assertNotNull(board.getZAxis());
        assertNotNull(board.getSortOrder());
        assertEquals(0, board.getIsDeleted());
    }

    @Test
    void story2_pullPending_shouldMarkNoData_whenPrepackageMissing() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        pushBatch(batchNum, workId);

        PrepackageDataDTO emptyDto = new PrepackageDataDTO();
        Mockito.doReturn(emptyDto)
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum, workId);

        prePackagePullTask.pullPrePackageData();

        MesWorkOrder workOrder = getWorkOrder(workId);
        assertEquals("NO_DATA", workOrder.getPrepackageStatus());
    }

    @Test
    void story2_pullPending_shouldFailAfterRetries_andNotify() throws Exception {
        Mockito.reset(mailSender);
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        insertEmailConfig();
        pushBatch(batchNum, workId);

        Mockito.doThrow(new RuntimeException("mock failure"))
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum, workId);

        prePackagePullTask.pullPrePackageData();

        MesWorkOrder workOrder = getWorkOrder(workId);
        assertEquals("FAILED", workOrder.getPrepackageStatus());
        assertEquals(3, workOrder.getRetryCount());
        Mockito.verify(emailNotificationService, Mockito.times(1))
            .sendPrepackagePullFailureNotification(Mockito.eq(batchNum), Mockito.eq(workId), Mockito.anyString(), Mockito.eq(3));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender, Mockito.atLeastOnce()).send(mailCaptor.capture());
        boolean failureMailSentToMe = mailCaptor.getAllValues().stream()
            .filter(msg -> msg.getSubject() != null && msg.getSubject().contains("预包装数据拉取失败"))
            .anyMatch(msg -> msg.getTo() != null && Arrays.asList(msg.getTo()).contains("243219169@qq.com"));
        assertTrue(failureMailSentToMe);
    }

    private void insertEmailConfig() {
        jdbcTemplate.update(
            "INSERT INTO mes_email_notification_config " +
                "(smtp_host, smtp_port, username, password, from_address, to_addresses, enabled, is_deleted, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            "smtp.qq.com", 587, "243219169@qq.com", "mock-pass",
            "243219169@qq.com", "243219169@qq.com", 1, 0, "TEST"
        );
    }

    @Test
    void story3to5_partQueries_shouldReturnData_andRespectUpdatingStatus() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        pushBatch(batchNum, workId);
        prePackagePullTask.pullPrePackageData();

        MesBoard board = boardMapper.selectOne(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0)
                .last("LIMIT 1"));
        assertNotNull(board);
        String partCode = board.getPartCode();

        MesPackage pkg = packageMapper.selectById(board.getPackageId());
        assertNotNull(pkg);
        MesBoxCode box = boxCodeMapper.selectById(pkg.getBoxId());
        assertNotNull(box);

        mockMvc.perform(get("/api/v1/production/part/{partCode}/work-order-and-batch", partCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.optimizingFiles[0].workOrders[0].workId").value(workId))
            .andExpect(jsonPath("$.data.batch.batchNum").value(batchNum));

        mockMvc.perform(get("/api/v1/production/part/{partCode}/package", partCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.prepackageOrder.boxes[0].boxCode").value(box.getBoxCode()));

        mockMvc.perform(get("/api/v1/production/part/{partCode}/detail", partCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.partCode").value(partCode))
            .andExpect(jsonPath("$.standardListRaw").isNotEmpty());

        // 状态为 UPDATING 时返回 409
        MesWorkOrder workOrder = getWorkOrder(workId);
        workOrder.setPrepackageStatus("UPDATING");
        workOrderMapper.updateById(workOrder);

        mockMvc.perform(get("/api/v1/production/part/{partCode}/work-order-and-batch", partCode))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/production/part/{partCode}/package", partCode))
            .andExpect(status().isConflict());
    }

    @Test
    void story6_workReport_shouldBeIdempotentByStationAndStatus() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        pushBatch(batchNum, workId);
        prePackagePullTask.pullPrePackageData();

        MesBoard board = boardMapper.selectOne(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0)
                .last("LIMIT 1"));
        assertNotNull(board);

        WorkReportRequest request = WorkReportRequest.builder()
            .partCode(board.getPartCode())
            .partStatus("DONE")
            .stationCode("C1A001")
            .stationName("开料")
            .operatorId("OP-1")
            .operatorName("测试员")
            .isCompleted(1)
            .realPackageNo("PKG-REAL-001")
            .build();

        mockMvc.perform(post("/api/v1/production/work-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/production/part/{partCode}/detail", board.getPartCode()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.realPackageNo").value("PKG-REAL-001"));

        mockMvc.perform(post("/api/v1/production/work-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());

        // 状态变化后允许再次报工
        WorkReportRequest changed = WorkReportRequest.builder()
            .partCode(board.getPartCode())
            .partStatus("CHECKED")
            .stationCode("C1A001")
            .stationName("开料")
            .operatorId("OP-1")
            .operatorName("测试员")
            .isCompleted(1)
            .build();

        mockMvc.perform(post("/api/v1/production/work-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changed)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/production/part/{partCode}/detail", board.getPartCode()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.realPackageNo").value("PKG-REAL-001"));

        long reportCount = workReportMapper.selectCount(
            new LambdaQueryWrapper<MesWorkReport>().eq(MesWorkReport::getPartCode, board.getPartCode()));
        assertEquals(2L, reportCount);
    }

    @Test
    void story7_repull_shouldOverwritePrepackage_andPreserveReports() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        pushBatch(batchNum, workId);
        prePackagePullTask.pullPrePackageData();

        List<MesBoard> originalBoards = boardMapper.selectList(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0)
                .orderByAsc(MesBoard::getPartCode));
        assertEquals(3, originalBoards.size());

        String partWithReport = originalBoards.get(0).getPartCode();
        WorkReportRequest report = WorkReportRequest.builder()
            .partCode(partWithReport)
            .partStatus("DONE")
            .stationCode("C1A001")
            .stationName("开料")
            .operatorId("OP-1")
            .operatorName("测试员")
            .isCompleted(1)
            .build();

        mockMvc.perform(post("/api/v1/production/work-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(report)))
            .andExpect(status().isOk());

        // 重新拉取：只保留2个板件（模拟上游删除1个板件）
        List<String> partCodesToKeep = Arrays.asList(originalBoards.get(0).getPartCode(), originalBoards.get(1).getPartCode());
        PrepackageDataDTO repullDto = buildDtoWithPartCodes(batchNum, workId, partCodesToKeep);
        Mockito.doReturn(repullDto)
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum, workId);

        mockMvc.perform(post("/api/v1/admin/work-order/{workId}/repull", workId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"tester\",\"reason\":\"spec-test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        long activeBoards = boardMapper.selectCount(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0));
        long deletedBoards = boardMapper.countDeletedByWorkId(workId);
        assertEquals(2L, activeBoards);
        assertTrue(deletedBoards >= 1);

        long reports = workReportMapper.selectCount(
            new LambdaQueryWrapper<MesWorkReport>().eq(MesWorkReport::getPartCode, partWithReport));
        assertEquals(1L, reports);
    }

    private void pushBatch(String batchNum, String workId) throws Exception {
        BatchPushRequest request = buildBatchRequest(batchNum, Arrays.asList(workId));
        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    private MesWorkOrder getWorkOrder(String workId) {
        MesWorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getWorkId, workId));
        assertNotNull(workOrder);
        return workOrder;
    }

    private MesWorkOrder waitForWorkOrderStatus(String workId, String expectedStatus, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        MesWorkOrder workOrder = null;
        while (System.currentTimeMillis() < deadline) {
            workOrder = getWorkOrder(workId);
            if (expectedStatus.equals(workOrder.getPrepackageStatus())) {
                return workOrder;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return workOrder;
    }

    private BatchPushRequest buildBatchRequest(String batchNum, List<String> workIds) {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        BatchPushRequest request = new BatchPushRequest();
        request.setBatchNum(batchNum);
        request.setBatchType("1");
        request.setProductTime(today);
        request.setSimpleBatchNum(batchNum);
        request.setNestingTime(today);
        request.setYmba014("云南线");
        request.setYmba016("N");

        BatchPushRequest.WorkOrderInfo workOrderTemplate = new BatchPushRequest.WorkOrderInfo();
        workOrderTemplate.setRoute("LINE-A");
        workOrderTemplate.setRouteId("RID-1");
        workOrderTemplate.setOrderType("N04");
        workOrderTemplate.setDeliveryTime(tomorrow);
        workOrderTemplate.setNestingTime(today);
        workOrderTemplate.setYmba014("云南线");
        workOrderTemplate.setYmba015("SA001");
        workOrderTemplate.setYmba016("N");
        workOrderTemplate.setPart0("PART-0");
        workOrderTemplate.setCondition0("COND-0");
        workOrderTemplate.setPartTime0(today);
        workOrderTemplate.setZuz(1);

        List<BatchPushRequest.WorkOrderInfo> workOrders = new ArrayList<>();
        for (String workId : workIds) {
            BatchPushRequest.WorkOrderInfo info = new BatchPushRequest.WorkOrderInfo();
            info.setWorkId(workId);
            info.setRoute(workOrderTemplate.getRoute());
            info.setRouteId(workOrderTemplate.getRouteId());
            info.setOrderType(workOrderTemplate.getOrderType());
            info.setDeliveryTime(workOrderTemplate.getDeliveryTime());
            info.setNestingTime(workOrderTemplate.getNestingTime());
            info.setYmba014(workOrderTemplate.getYmba014());
            info.setYmba015(workOrderTemplate.getYmba015());
            info.setYmba016(workOrderTemplate.getYmba016());
            info.setPart0(workOrderTemplate.getPart0());
            info.setCondition0(workOrderTemplate.getCondition0());
            info.setPartTime0(workOrderTemplate.getPartTime0());
            info.setZuz(workOrderTemplate.getZuz());
            workOrders.add(info);
        }

        BatchPushRequest.OptimizingFileInfo fileInfo = new BatchPushRequest.OptimizingFileInfo();
        fileInfo.setOptimizingFileName("OPT-" + batchNum + ".txt");
        fileInfo.setStationCode("C1A001");
        fileInfo.setUrgency(0);
        fileInfo.setWorkOrders(workOrders);

        request.setOptimizingFiles(Arrays.asList(fileInfo));
        return request;
    }

    private PrepackageDataDTO buildDtoWithPartCodes(String batchNum, String workId, List<String> partCodes) throws Exception {
        JsonNode template = loadTemplate();
        ObjectNode root = template.deepCopy();
        ObjectNode info = root.with("PrePackageInfo");

        info.put("OrderNum", "ORDER-" + batchNum + "-" + workId);
        info.put("ContractNo", "CONTRACT-" + batchNum + "-" + workId);
        info.put("WorkNum", workId);
        info.put("ShipBatch", "SHIP-" + batchNum);
        info.put("ProductionNum", "PROD-" + batchNum + "-" + workId);

        ArrayNode boxDetails = info.withArray("BoxInfoDetails");
        Map<ArrayNode, List<Integer>> removals = new HashMap<>();
        int globalIndex = 0;

        for (int i = 0; i < boxDetails.size(); i++) {
            ObjectNode box = (ObjectNode) boxDetails.get(i);
            box.put("BoxCode", batchNum + "-" + workId + "-BOX-" + (i + 1));

            ArrayNode packages = box.withArray("PackageInfos");
            for (int j = 0; j < packages.size(); j++) {
                ObjectNode pkg = (ObjectNode) packages.get(j);
                ArrayNode parts = pkg.withArray("PartInfos");

                for (int k = 0; k < parts.size(); k++) {
                    ObjectNode part = (ObjectNode) parts.get(k);
                    if (globalIndex < partCodes.size()) {
                        part.put("PartCode", partCodes.get(globalIndex));
                        globalIndex++;
                    } else {
                        removals.computeIfAbsent(parts, key -> new ArrayList<>()).add(k);
                    }
                }
            }
        }

        for (Map.Entry<ArrayNode, List<Integer>> entry : removals.entrySet()) {
            List<Integer> indexes = entry.getValue();
            indexes.sort((a, b) -> Integer.compare(b, a));
            for (Integer index : indexes) {
                entry.getKey().remove(index.intValue());
            }
        }

        // 更新 partCount
        for (int i = 0; i < boxDetails.size(); i++) {
            ObjectNode box = (ObjectNode) boxDetails.get(i);
            ArrayNode packages = box.withArray("PackageInfos");
            for (int j = 0; j < packages.size(); j++) {
                ObjectNode pkg = (ObjectNode) packages.get(j);
                ArrayNode parts = pkg.withArray("PartInfos");
                pkg.put("PartCount", parts.size());
            }
        }

        return objectMapper.convertValue(root, PrepackageDataDTO.class);
    }

    private JsonNode loadTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource("mock/prepackage.json");
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        }
    }

    private String unique(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    @TestConfiguration
    static class NoScheduleConfig {
        @Bean
        @Primary
        public TaskScheduler taskScheduler() {
            return new TaskScheduler() {
                @Override
                public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
                    return new NoOpScheduledFuture();
                }

                @Override
                public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
                    return new NoOpScheduledFuture();
                }

                @Override
                public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
                    return new NoOpScheduledFuture();
                }

                @Override
                public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
                    return new NoOpScheduledFuture();
                }

                @Override
                public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
                    return new NoOpScheduledFuture();
                }

                @Override
                public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
                    return new NoOpScheduledFuture();
                }

                @Override
                public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
                    return new NoOpScheduledFuture();
                }
            };
        }
    }

    static class NoOpScheduledFuture implements ScheduledFuture<Object> {
        @Override
        public long getDelay(java.util.concurrent.TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed o) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return true;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, java.util.concurrent.TimeUnit unit) {
            return null;
        }
    }
}
