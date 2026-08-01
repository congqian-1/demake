package com.tongzhou.mes.service1.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tongzhou.mes.service1.client.ThirdPartyMesClient;
import com.tongzhou.mes.service1.pojo.dto.BatchQueryResponseDTO;
import com.tongzhou.mes.service1.pojo.dto.PrepackageDataDTO;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * 测试环境下统一 mock 第三方MES客户端，避免真实网络调用。
 */
@Configuration
public class ThirdPartyMesClientMockConfiguration {

    private final ObjectMapper objectMapper;
    private final JsonNode mockTemplate;

    public ThirdPartyMesClientMockConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.mockTemplate = loadMockTemplate(objectMapper);
    }

    @Bean
    @Primary
    public ThirdPartyMesClient thirdPartyMesClientMock() throws Exception {
        ThirdPartyMesClient mock = Mockito.mock(ThirdPartyMesClient.class);

        Mockito.when(mock.getPrepackageInfo(Mockito.anyString(), Mockito.anyString()))
            .thenAnswer(invocation -> {
                String batchNum = invocation.getArgument(0);
                String workId = invocation.getArgument(1);
                return buildMockPrepackageData(batchNum, workId);
            });

        Mockito.when(mock.batchQueryProcess(Mockito.anyList()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                java.util.List<String> barcodes = invocation.getArgument(0);
                return buildMockBatchQueryResponse(barcodes);
            });

        return mock;
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSenderMock() {
        return Mockito.mock(JavaMailSender.class);
    }

    private PrepackageDataDTO buildMockPrepackageData(String batchNum, String workId) {
        String safeBatchNum = batchNum == null ? "BATCH" : batchNum;
        String safeWorkId = workId == null ? "WORK" : workId;
        String key = safeBatchNum + "-" + safeWorkId;

        ObjectNode root = mockTemplate.deepCopy();
        ObjectNode info = root.with("PrePackageInfo");

        info.put("OrderNum", "ORDER-" + key);
        info.put("ContractNo", "CONTRACT-" + key);
        info.put("WorkNum", safeWorkId);
        info.put("ShipBatch", "SHIP-" + safeBatchNum);
        info.put("ProductionNum", "PROD-" + key);
        info.put("type", "TYPE-" + safeBatchNum);
        info.put("FDD8", "FDD8-" + key);

        ArrayNode boxInfoDetails = info.withArray("BoxInfoDetails");
        int partIndex = 1;

        for (int i = 0; i < boxInfoDetails.size(); i++) {
            ObjectNode box = (ObjectNode) boxInfoDetails.get(i);
            String boxCode = key + "-BOX-" + (i + 1);
            box.put("BoxCode", boxCode);

            ArrayNode packageInfos = box.withArray("PackageInfos");
            for (int j = 0; j < packageInfos.size(); j++) {
                ObjectNode pkg = (ObjectNode) packageInfos.get(j);
                ArrayNode partInfos = pkg.withArray("PartInfos");
                pkg.put("PartCount", partInfos.size());

                for (int k = 0; k < partInfos.size(); k++) {
                    ObjectNode part = (ObjectNode) partInfos.get(k);
                    part.put("PartCode", key + "-PART-" + partIndex);
                    partIndex++;
                }
            }
        }

        return objectMapper.convertValue(root, PrepackageDataDTO.class);
    }

    private BatchQueryResponseDTO buildMockBatchQueryResponse(java.util.List<String> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) {
            return null;
        }
        BatchQueryResponseDTO response = new BatchQueryResponseDTO();
        response.setCode(0);
        response.setMsg("执行成功");
        java.util.List<BatchQueryResponseDTO.BatchQueryItem> items = new java.util.ArrayList<>();
        for (String barcode : barcodes) {
            BatchQueryResponseDTO.BatchQueryItem item = new BatchQueryResponseDTO.BatchQueryItem();
            item.setFtm(barcode);
            // 从 barcode 推断批次号：格式 WD000658348B1015 → PCJH-260506-0087
            if (barcode.startsWith("WD000658348")) {
                item.setFpjh("PCJH-260506-0087");
                item.setFgdh("WD000658348BBCP024");
            } else {
                item.setFpjh("PCJH-" + barcode.hashCode());
                item.setFgdh("WO-" + barcode.hashCode());
            }
            item.setFgnym("DW020:包装");
            item.setFsx(9);
            item.setRn(1);
            items.add(item);
        }
        response.setData(items);
        return response;
    }

    private JsonNode loadMockTemplate(ObjectMapper mapper) {
        ClassPathResource resource = new ClassPathResource("mock/prepackage.json");
        try (InputStream inputStream = resource.getInputStream()) {
            return mapper.readTree(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock prepackage JSON template", e);
        }
    }
}
