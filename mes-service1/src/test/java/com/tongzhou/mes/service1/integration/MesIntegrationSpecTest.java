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
import org.springframework.test.web.servlet.MvcResult;
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
    void story1_batchPush_shouldResetExistingWorkOrder_whenSameBatchFileAndWorkOrderRepeated() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum, Arrays.asList(workId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        MesWorkOrder existing = getWorkOrder(workId);
        existing.setPrepackageStatus("FAILED");
        existing.setRetryCount(2);
        existing.setErrorMessage("mock-error");
        workOrderMapper.updateById(existing);

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum, Arrays.asList(workId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.workOrderCount").value(1));

        MesBatch batch = batchMapper.selectOne(
            new LambdaQueryWrapper<MesBatch>().eq(MesBatch::getBatchNum, batchNum));
        assertNotNull(batch);

        long countAfterUpdate = workOrderMapper.selectCount(
            new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getBatchId, batch.getId()));
        assertEquals(1L, countAfterUpdate);

        MesWorkOrder refreshed = getWorkOrder(workId);
        assertEquals("NOT_PULLED", refreshed.getPrepackageStatus());
        assertEquals(0, refreshed.getRetryCount());
        assertEquals(null, refreshed.getErrorMessage());
    }

    @Test
    void story1_batchPush_shouldAllowNewFileAndNewWorkOrderWithinSameBatch() throws Exception {
        String batchNum = unique("BATCH");
        String workId1 = unique("WO");
        String workId2 = unique("WO");

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum, "OPT-A.txt", Arrays.asList(workId1)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum, "OPT-B.txt", Arrays.asList(workId2)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        MesBatch batch = batchMapper.selectOne(
            new LambdaQueryWrapper<MesBatch>().eq(MesBatch::getBatchNum, batchNum));
        assertNotNull(batch);
        assertEquals(1L, batchMapper.selectCount(new LambdaQueryWrapper<MesBatch>().eq(MesBatch::getBatchNum, batchNum)));
        assertEquals(2L, workOrderMapper.selectCount(new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getBatchId, batch.getId())));
    }

    @Test
    void story1_batchPush_shouldAllowSameWorkIdAcrossDifferentBatches() throws Exception {
        String workId = unique("WO");
        String batchNum1 = unique("BATCH");
        String batchNum2 = unique("BATCH");

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum1, Arrays.asList(workId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum2, Arrays.asList(workId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertEquals(2L, workOrderMapper.selectCount(
            new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getWorkId, workId)));
        assertEquals(batchNum1, getWorkOrder(batchNum1, workId).getBatchNum());
        assertEquals(batchNum2, getWorkOrder(batchNum2, workId).getBatchNum());
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
        assertEquals("TYPE-" + batchNum, order.getType());
        assertEquals("FDD8-" + batchNum + "-" + workId, order.getFdd8());
        assertEquals(2, order.getPrepackageInfoSize());
        assertEquals(1, order.getTotalSet());
        assertEquals(2, order.getMaxPackageNo());
        assertEquals("PROD-" + batchNum + "-" + workId, order.getProductionNum());
        assertEquals(1, order.getIsProject());
        assertEquals("Mock CustomerName", order.getCustomerName());
        assertEquals("FNUMBER-TEMPLATE", order.getFnumber());
        assertEquals("DOB-TEMPLATE", order.getDob());
        assertEquals("Mock Detailed Address", order.getDetailedAddress());

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
        assertEquals("UNIT-1", box1.getUnit());
        assertEquals(0, box1.getIsDeleted());
        assertEquals(batchNum + "-" + workId + "-BOX-2", box2.getBoxCode());
        assertEquals("1", box2.getBuilding());
        assertEquals("A", box2.getHouse());
        assertEquals("102", box2.getRoom());
        assertEquals(1, box2.getSetno());
        assertEquals("Gray", box2.getColor());
        assertEquals("UNIT-2", box2.getUnit());
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
        assertEquals("0410", pkg1.getBoxType2());
        assertEquals(2, pkg1.getPartCount());
        assertNotNull(pkg1.getLength());
        assertNotNull(pkg1.getWidth());
        assertNotNull(pkg1.getDepth());
        assertNotNull(pkg1.getWeight());
        assertEquals(0, pkg1.getIsDeleted());
        assertEquals(2, pkg2.getPackageNo());
        assertEquals("天地盖", pkg2.getBoxType());
        assertEquals("0910", pkg2.getBoxType2());
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
                .orderByAsc(MesBoard::getPartCode)
                .last("LIMIT 1"));
        assertNotNull(board);
        assertNotNull(board.getStandardList());
        assertTrue(board.getStandardList().contains("STD"));
        assertNotNull(board.getStandardCode());
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
        assertNotNull(board.getRotate());
        assertNotNull(board.getProcessCode());
        assertEquals(0, board.getIsDeleted());

        List<MesBoard> boards = boardMapper.selectList(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0)
                .orderByAsc(MesBoard::getPartCode));
        assertEquals(3, boards.size());
        assertEquals("0", boards.get(0).getRotate());
        assertEquals("PROC-A", boards.get(0).getProcessCode());
        assertEquals("1", boards.get(1).getRotate());
        assertEquals("PROC-B", boards.get(1).getProcessCode());
        assertEquals("0", boards.get(2).getRotate());
        assertEquals("PROC-C", boards.get(2).getProcessCode());
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
            .andExpect(jsonPath("$.data.batch.batchNum").value(batchNum))
            .andExpect(jsonPath("$.data.optimizingFiles[0].workOrders[0].prepackageOrder.type").value("TYPE-" + batchNum))
            .andExpect(jsonPath("$.data.optimizingFiles[0].workOrders[0].prepackageOrder.fdd8").value("FDD8-" + batchNum + "-" + workId))
            .andExpect(jsonPath("$.data.optimizingFiles[0].workOrders[0].prepackageOrder.boxes[0].packages[0].parts[0].rotate").value("0"))
            .andExpect(jsonPath("$.data.optimizingFiles[0].workOrders[0].prepackageOrder.boxes[0].packages[0].parts[0].processCode").value("PROC-A"));

        mockMvc.perform(get("/api/v1/production/part/{partCode}/package", partCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.prepackageOrder.boxes[0].boxCode").value(box.getBoxCode()))
            .andExpect(jsonPath("$.data.prepackageOrder.type").value("TYPE-" + batchNum))
            .andExpect(jsonPath("$.data.prepackageOrder.fdd8").value("FDD8-" + batchNum + "-" + workId))
            .andExpect(jsonPath("$.data.prepackageOrder.boxes[0].packages[0].parts[0].rotate").value("0"))
            .andExpect(jsonPath("$.data.prepackageOrder.boxes[0].packages[0].parts[0].processCode").value("PROC-A"));

        mockMvc.perform(get("/api/v1/production/part/{partCode}/detail", partCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.partCode").value(partCode))
            .andExpect(jsonPath("$.standardListRaw").isNotEmpty())
            .andExpect(jsonPath("$.prepackageOrder.type").value("TYPE-" + batchNum))
            .andExpect(jsonPath("$.prepackageOrder.fdd8").value("FDD8-" + batchNum + "-" + workId))
            .andExpect(jsonPath("$.rotate").value("0"))
            .andExpect(jsonPath("$.processCode").value("PROC-A"));

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
    void story7_repull_shouldResetStatusOnly_thenSchedulerOverwritesData() throws Exception {
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

            .build();

        mockMvc.perform(post("/api/v1/production/work-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(report)))
            .andExpect(status().isOk());

        // 重新拉取：只保留2个板件（模拟上游删除1个板件）
        List<String> partCodesToKeep = Arrays.asList(originalBoards.get(0).getPartCode(), originalBoards.get(1).getPartCode());
        PrepackageDataDTO repullDto = buildDtoWithPartCodes(batchNum, workId, partCodesToKeep);
        repullDto.getPrePackageInfo().setType("TYPE-REFRESH");
        repullDto.getPrePackageInfo().setFdd8("FDD8-REFRESH");
        repullDto.getPrePackageInfo().getBoxInfoDetails().get(0).getPackageInfos().get(0).getPartInfos().get(0).setRotate("9");
        repullDto.getPrePackageInfo().getBoxInfoDetails().get(0).getPackageInfos().get(0).getPartInfos().get(0).setProcessCode("PROC-REFRESH-1");
        repullDto.getPrePackageInfo().getBoxInfoDetails().get(0).getPackageInfos().get(0).getPartInfos().get(1).setRotate("8");
        repullDto.getPrePackageInfo().getBoxInfoDetails().get(0).getPackageInfos().get(0).getPartInfos().get(1).setProcessCode("PROC-REFRESH-2");
        Mockito.doReturn(repullDto)
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum, workId);

        mockMvc.perform(post("/api/v1/admin/work-order/{workId}/repull", workId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"tester\",\"reason\":\"spec-test\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("工单已重置为未拉取"));

        MesWorkOrder resetWorkOrder = getWorkOrder(workId);
        assertEquals("NOT_PULLED", resetWorkOrder.getPrepackageStatus());

        prePackagePullTask.pullPrePackageData();
        MesWorkOrder pulledAgain = waitForWorkOrderStatus(workId, "PULLED", 5000);
        assertEquals("PULLED", pulledAgain.getPrepackageStatus());

        long activeBoards = boardMapper.selectCount(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0));
        long deletedBoards = boardMapper.countDeletedByWorkId(workId);
        assertEquals(2L, activeBoards);
        assertEquals(0L, deletedBoards);

        MesBoard refreshedBoard = boardMapper.selectOne(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getPartCode, partCodesToKeep.get(0))
                .eq(MesBoard::getIsDeleted, 0));
        assertNotNull(refreshedBoard);
        assertEquals("9", refreshedBoard.getRotate());
        assertEquals("PROC-REFRESH-1", refreshedBoard.getProcessCode());

        com.tongzhou.mes.service1.pojo.entity.MesPrepackageOrder refreshedOrder =
            prepackageOrderMapper.selectOne(
                new LambdaQueryWrapper<com.tongzhou.mes.service1.pojo.entity.MesPrepackageOrder>()
                    .eq(com.tongzhou.mes.service1.pojo.entity.MesPrepackageOrder::getWorkId, workId));
        assertNotNull(refreshedOrder);
        assertEquals("TYPE-REFRESH", refreshedOrder.getType());
        assertEquals("FDD8-REFRESH", refreshedOrder.getFdd8());

        long reports = workReportMapper.selectCount(
            new LambdaQueryWrapper<MesWorkReport>().eq(MesWorkReport::getPartCode, partWithReport));
        assertEquals(1L, reports);
    }

    @Test
    void story7_overwriteShouldKeepDeletedBoardsHiddenFromQueries() throws Exception {
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

        String deletedPartCode = originalBoards.get(2).getPartCode();
        List<String> partCodesToKeep = Arrays.asList(originalBoards.get(0).getPartCode(), originalBoards.get(1).getPartCode());
        Mockito.doReturn(buildDtoWithPartCodes(batchNum, workId, partCodesToKeep))
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum, workId);

        mockMvc.perform(post("/api/v1/admin/work-order/{workId}/repull", workId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"tester\",\"reason\":\"hide-deleted\"}"))
            .andExpect(status().isOk());

        prePackagePullTask.pullPrePackageData();

        mockMvc.perform(get("/api/v1/production/part/{partCode}/detail", deletedPartCode))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/production/part/{partCode}/package", partCodesToKeep.get(0)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.prepackageOrder.boxes[0].packages[0].parts.length()").value(2))
            .andExpect(jsonPath("$.data.prepackageOrder.boxes[0].packages[0].parts[0].rotate").isNotEmpty())
            .andExpect(jsonPath("$.data.prepackageOrder.boxes[0].packages[0].parts[0].processCode").isNotEmpty());
    }

    @Test
    void story7_overwriteShouldRevivePreviouslyDeletedPartCodeWithoutUniqueConflict() throws Exception {
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

        List<String> allPartCodes = Arrays.asList(
            originalBoards.get(0).getPartCode(),
            originalBoards.get(1).getPartCode(),
            originalBoards.get(2).getPartCode()
        );
        List<String> reducedPartCodes = Arrays.asList(allPartCodes.get(0), allPartCodes.get(1));
        String revivedPartCode = allPartCodes.get(2);

        Mockito.doReturn(buildDtoWithPartCodes(batchNum, workId, reducedPartCodes))
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum, workId);

        mockMvc.perform(post("/api/v1/admin/work-order/{workId}/repull", workId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"tester\",\"reason\":\"remove-part\"}"))
            .andExpect(status().isOk());
        prePackagePullTask.pullPrePackageData();
        assertEquals("PULLED", waitForWorkOrderStatus(workId, "PULLED", 5000).getPrepackageStatus());

        long afterRemoveActiveCount = boardMapper.selectCount(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0));
        assertEquals(2L, afterRemoveActiveCount);

        Mockito.doReturn(buildDtoWithPartCodes(batchNum, workId, allPartCodes))
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum, workId);

        mockMvc.perform(post("/api/v1/admin/work-order/{workId}/repull", workId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"tester\",\"reason\":\"restore-part\"}"))
            .andExpect(status().isOk());
        prePackagePullTask.pullPrePackageData();
        assertEquals("PULLED", waitForWorkOrderStatus(workId, "PULLED", 5000).getPrepackageStatus());

        long afterRestoreActiveCount = boardMapper.selectCount(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getWorkId, workId)
                .eq(MesBoard::getIsDeleted, 0));
        assertEquals(3L, afterRestoreActiveCount);

        MesBoard revivedBoard = boardMapper.selectOne(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getPartCode, revivedPartCode)
                .eq(MesBoard::getIsDeleted, 0));
        assertNotNull(revivedBoard);
    }

    @Test
    void story7_pullShouldAllowSameBoxCodeAcrossDifferentBatchWork() throws Exception {
        String sharedWorkId = unique("WO");
        String batchNum1 = unique("BATCH");
        String batchNum2 = unique("BATCH");

        pushBatch(batchNum1, sharedWorkId);
        pushBatch(batchNum2, sharedWorkId);

        PrepackageDataDTO dto1 = buildDtoWithPartCodes(batchNum1, sharedWorkId, Arrays.asList(
            batchNum1 + "-" + sharedWorkId + "-PART-1",
            batchNum1 + "-" + sharedWorkId + "-PART-2",
            batchNum1 + "-" + sharedWorkId + "-PART-3"
        ));
        PrepackageDataDTO dto2 = buildDtoWithPartCodes(batchNum2, sharedWorkId, Arrays.asList(
            batchNum2 + "-" + sharedWorkId + "-PART-1",
            batchNum2 + "-" + sharedWorkId + "-PART-2",
            batchNum2 + "-" + sharedWorkId + "-PART-3"
        ));

        applySharedBoxCodePrefix(dto1, "SHARED-BOX");
        applySharedBoxCodePrefix(dto2, "SHARED-BOX");

        Mockito.doReturn(dto1)
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum1, sharedWorkId);
        Mockito.doReturn(dto2)
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum2, sharedWorkId);

        prePackagePullTask.pullPrePackageData();

        assertEquals("PULLED", getWorkOrder(batchNum1, sharedWorkId).getPrepackageStatus());
        assertEquals("PULLED", getWorkOrder(batchNum2, sharedWorkId).getPrepackageStatus());

        long boxCountInBatch1 = boxCodeMapper.selectCount(
            new LambdaQueryWrapper<MesBoxCode>()
                .eq(MesBoxCode::getBatchNum, batchNum1)
                .eq(MesBoxCode::getWorkId, sharedWorkId)
                .eq(MesBoxCode::getIsDeleted, 0));
        long boxCountInBatch2 = boxCodeMapper.selectCount(
            new LambdaQueryWrapper<MesBoxCode>()
                .eq(MesBoxCode::getBatchNum, batchNum2)
                .eq(MesBoxCode::getWorkId, sharedWorkId)
                .eq(MesBoxCode::getIsDeleted, 0));

        assertTrue(boxCountInBatch1 > 0);
        assertTrue(boxCountInBatch2 > 0);
    }

    @Test
    void story7_batchRepull_shouldResetAllWorkOrdersToNotPulled() throws Exception {
        String batchNum = unique("BATCH");
        String workId1 = unique("WO");
        String workId2 = unique("WO");

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum, Arrays.asList(workId1, workId2)))))
            .andExpect(status().isOk());

        prePackagePullTask.pullPrePackageData();
        assertEquals("PULLED", waitForWorkOrderStatus(workId1, "PULLED", 5000).getPrepackageStatus());
        assertEquals("PULLED", waitForWorkOrderStatus(workId2, "PULLED", 5000).getPrepackageStatus());

        mockMvc.perform(post("/api/v1/admin/work-order/batch/{batchNum}/repull", batchNum)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"tester\",\"reason\":\"batch-reset\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.resetCount").value(2));

        assertEquals("NOT_PULLED", getWorkOrder(workId1).getPrepackageStatus());
        assertEquals("NOT_PULLED", getWorkOrder(workId2).getPrepackageStatus());
    }

    @Test
    void story8_pushSync_shouldPullImmediatelyAndReturnSummary() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");

        mockMvc.perform(post("/api/v1/third-party/batch/push-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSyncPayload(batchNum, Arrays.asList(workId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchNum").value(batchNum))
            .andExpect(jsonPath("$.totalWorkOrders").value(1))
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.failedCount").value(0))
            .andExpect(jsonPath("$.processingCount").value(0))
            .andExpect(jsonPath("$.workOrders[0].workId").value(workId))
            .andExpect(jsonPath("$.workOrders[0].status").value("PULLED"));

        MesWorkOrder workOrder = getWorkOrder(batchNum, workId);
        assertEquals("PULLED", workOrder.getPrepackageStatus());
    }

    @Test
    void story8_pushSync_shouldAllowRepeatSubmissionWithoutDuplicateWorkOrderRecords() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        String payload = objectMapper.writeValueAsString(buildSyncPayload(batchNum, Arrays.asList(workId)));

        mockMvc.perform(post("/api/v1/third-party/batch/push-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1));

        mockMvc.perform(post("/api/v1/third-party/batch/push-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.totalWorkOrders").value(1));

        long duplicated = workOrderMapper.selectCount(
            new LambdaQueryWrapper<MesWorkOrder>()
                .eq(MesWorkOrder::getBatchNum, batchNum)
                .eq(MesWorkOrder::getWorkId, workId)
                .eq(MesWorkOrder::getIsDeleted, 0));
        assertEquals(1L, duplicated);
    }

    @Test
    void story8_pushSync_shouldReturnProcessingWhenWorkOrderIsUpdating() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        pushBatch(batchNum, workId);
        MesWorkOrder workOrder = getWorkOrder(batchNum, workId);
        workOrder.setPrepackageStatus("UPDATING");
        workOrder.setReprocessPending(0);
        workOrderMapper.updateById(workOrder);

        mockMvc.perform(post("/api/v1/third-party/batch/push-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSyncPayload(batchNum, Arrays.asList(workId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processingCount").value(1))
            .andExpect(jsonPath("$.workOrders[0].status").value("PROCESSING"));

        MesWorkOrder refreshed = getWorkOrder(batchNum, workId);
        assertEquals("UPDATING", refreshed.getPrepackageStatus());
        assertEquals(1, refreshed.getReprocessPending());
    }

    @Test
    void story8_pushSync_shouldReturnChineseDuplicateReasonForInsertConflict() throws Exception {
        String batchNum1 = unique("BATCH");
        String batchNum2 = unique("BATCH");
        String workId1 = unique("WO");
        String workId2 = unique("WO");

        pushBatch(batchNum1, workId1);
        prePackagePullTask.pullPrePackageData();
        assertEquals("PULLED", waitForWorkOrderStatus(workId1, "PULLED", 5000).getPrepackageStatus());

        List<MesBoard> boards = boardMapper.selectList(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getBatchNum, batchNum1)
                .eq(MesBoard::getWorkId, workId1)
                .eq(MesBoard::getIsDeleted, 0)
                .orderByAsc(MesBoard::getPartCode));
        assertTrue(!boards.isEmpty());

        List<String> duplicatedPartCodes = new ArrayList<>();
        for (MesBoard board : boards) {
            duplicatedPartCodes.add(board.getPartCode());
        }

        PrepackageDataDTO duplicatedDto = buildDtoWithPartCodes(batchNum2, workId2, duplicatedPartCodes);
        Mockito.doReturn(duplicatedDto)
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum2, workId2);

        mockMvc.perform(post("/api/v1/third-party/batch/push-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSyncPayload(batchNum2, Arrays.asList(workId2)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedCount").value(1))
            .andExpect(jsonPath("$.workOrders[0].status").value("FAILED"))
            .andExpect(jsonPath("$.workOrders[0].errorMessage").value("板件重复：板件编码已存在，无法重复新增"));
    }

    @Test
    void story8_pushSync_shouldMarkFailedWithoutResetWhenSyncSaveFails() throws Exception {
        String batchNum = unique("BATCH");
        String workId = unique("WO");
        PrepackageDataDTO malformed = buildDtoWithPartCodes(batchNum, workId, Arrays.asList(
            batchNum + "-" + workId + "-PART-1",
            batchNum + "-" + workId + "-PART-2",
            batchNum + "-" + workId + "-PART-3"
        ));
        malformed.getPrePackageInfo().getBoxInfoDetails().get(0).getPackageInfos().add(null);
        Mockito.doReturn(malformed)
            .when(thirdPartyMesClient)
            .getPrepackageInfo(batchNum, workId);

        mockMvc.perform(post("/api/v1/third-party/batch/push-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSyncPayload(batchNum, Arrays.asList(workId)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failedCount").value(1))
            .andExpect(jsonPath("$.workOrders[0].status").value("FAILED"));

        MesWorkOrder refreshed = getWorkOrder(batchNum, workId);
        assertEquals("FAILED", refreshed.getPrepackageStatus());
        assertTrue(refreshed.getErrorMessage() != null && refreshed.getErrorMessage().contains("EXCEPTION"));
    }

    @Test
    void story8_pushSync_shouldReportPartialFailureAndContinueOtherWorkOrders() throws Exception {
        String sourceBatch = unique("BATCH");
        String sourceWork = unique("WO");
        pushBatch(sourceBatch, sourceWork);
        prePackagePullTask.pullPrePackageData();
        assertEquals("PULLED", waitForWorkOrderStatus(sourceWork, "PULLED", 5000).getPrepackageStatus());

        List<MesBoard> sourceBoards = boardMapper.selectList(
            new LambdaQueryWrapper<MesBoard>()
                .eq(MesBoard::getBatchNum, sourceBatch)
                .eq(MesBoard::getWorkId, sourceWork)
                .eq(MesBoard::getIsDeleted, 0)
                .orderByAsc(MesBoard::getPartCode));
        assertTrue(!sourceBoards.isEmpty());

        List<String> duplicatedPartCodes = new ArrayList<>();
        for (MesBoard board : sourceBoards) {
            duplicatedPartCodes.add(board.getPartCode());
        }

        String targetBatch = unique("BATCH");
        String failedWork = unique("WO");
        String successWork = unique("WO");
        PrepackageDataDTO duplicatedDto = buildDtoWithPartCodes(targetBatch, failedWork, duplicatedPartCodes);
        Mockito.doReturn(duplicatedDto)
            .when(thirdPartyMesClient)
            .getPrepackageInfo(targetBatch, failedWork);

        MvcResult result = mockMvc.perform(post("/api/v1/third-party/batch/push-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    buildSyncPayload(targetBatch, Arrays.asList(failedWork, successWork)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalWorkOrders").value(2))
            .andExpect(jsonPath("$.failedCount").value(1))
            .andExpect(jsonPath("$.successCount").value(1))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode details = body.get("workOrders");
        String failedStatus = null;
        String failedError = null;
        String successStatus = null;
        for (JsonNode detail : details) {
            String workId = detail.get("workId").asText();
            if (failedWork.equals(workId)) {
                failedStatus = detail.get("status").asText();
                failedError = detail.path("errorMessage").asText();
            }
            if (successWork.equals(workId)) {
                successStatus = detail.get("status").asText();
            }
        }

        assertEquals("FAILED", failedStatus);
        assertTrue(failedError.contains("重复"));
        assertTrue("PULLED".equals(successStatus) || "NO_DATA".equals(successStatus));
    }

    @Test
    void story8_pushSync_shouldCoexistWithAsyncPushAndSchedulerPull() throws Exception {
        String batchNum = unique("BATCH");
        String syncWork = unique("WO");
        String asyncWork = unique("WO");

        mockMvc.perform(post("/api/v1/third-party/batch/push-sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSyncPayload(batchNum, Arrays.asList(syncWork)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1));
        assertEquals("PULLED", getWorkOrder(batchNum, syncWork).getPrepackageStatus());

        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildBatchRequest(batchNum, Arrays.asList(asyncWork)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertEquals("NOT_PULLED", getWorkOrder(batchNum, asyncWork).getPrepackageStatus());

        prePackagePullTask.pullPrePackageData();
        assertEquals("PULLED", waitForWorkOrderStatus(asyncWork, "PULLED", 5000).getPrepackageStatus());
        assertEquals("PULLED", getWorkOrder(batchNum, syncWork).getPrepackageStatus());
    }

    private void pushBatch(String batchNum, String workId) throws Exception {
        BatchPushRequest request = buildBatchRequest(batchNum, Arrays.asList(workId));
        mockMvc.perform(post("/api/v1/third-party/batch/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    private ObjectNode buildSyncPayload(String batchNum, List<String> workIds) {
        return objectMapper.valueToTree(buildBatchRequest(batchNum, workIds));
    }

    private MesWorkOrder getWorkOrder(String workId) {
        MesWorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<MesWorkOrder>().eq(MesWorkOrder::getWorkId, workId));
        assertNotNull(workOrder);
        return workOrder;
    }

    private MesWorkOrder getWorkOrder(String batchNum, String workId) {
        MesWorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<MesWorkOrder>()
                .eq(MesWorkOrder::getBatchNum, batchNum)
                .eq(MesWorkOrder::getWorkId, workId));
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
        return buildBatchRequest(batchNum, "OPT-" + batchNum + ".txt", workIds);
    }

    private BatchPushRequest buildBatchRequest(String batchNum, String optimizingFileName, List<String> workIds) {
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
        fileInfo.setOptimizingFileName(optimizingFileName);
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

    private void applySharedBoxCodePrefix(PrepackageDataDTO dto, String prefix) {
        List<PrepackageDataDTO.BoxInfoDetail> boxInfos = dto.getPrePackageInfo().getBoxInfoDetails();
        for (int i = 0; i < boxInfos.size(); i++) {
            boxInfos.get(i).setBoxCode(prefix + "-" + (i + 1));
        }
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
