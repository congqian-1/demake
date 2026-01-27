/*
 * Copyright (c) 2022 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tongzhou.mes.service1.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.tongzhou.mes.service1.converter.ThirdPartyPrepackageMapper;
import com.tongzhou.mes.service1.pojo.dto.PrepackageDataDTO;
import com.tongzhou.mes.service1.pojo.dto.ThirdPartyPrepackageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 第三方MES系统API客户端
 * 
 * @author MES Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThirdPartyMesClient {

    @Value("${mes.third-party.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper;
    private final ThirdPartyPrepackageMapper prepackageMapper;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ThreadLocal<LastCall> lastCallHolder = new ThreadLocal<>();

    /**
     * 拉取预包装数据（根据批次号和工单号）
     * 
     * @param batchNum 批次号
     * @param workId 工单号
     * @return 预包装数据
     * @throws IOException 网络异常
     */
    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public PrepackageDataDTO getPrepackageInfo(String batchNum, String workId) throws IOException {
        String url = baseUrl;

        java.util.Map<String, Object> serviceMap = new java.util.LinkedHashMap<>();
        serviceMap.put("name", "getPrepackageInfo");

        java.util.Map<String, Object> payloadMap = new java.util.LinkedHashMap<>();
        payloadMap.put("batchNum", batchNum);
        payloadMap.put("workId", workId);

        java.util.Map<String, Object> requestBodyMap = new java.util.LinkedHashMap<>();
        requestBodyMap.put("service", serviceMap);
        requestBodyMap.put("payload", payloadMap);

        String requestBody = objectMapper.writeValueAsString(requestBodyMap);

        LastCall lastCall = new LastCall();
        lastCall.url = url;
        lastCall.requestBody = requestBody;
        lastCallHolder.set(lastCall);

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                requestBody,
                okhttp3.MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                lastCall.httpStatus = response.code();
                lastCall.responseBody = response.body() != null ? response.body().string() : null;
                log.error("Failed to pull prepackage data for batch: {}, workId: {}, status: {}", 
                        batchNum, workId, response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : null;
            lastCall.httpStatus = response.code();
            lastCall.responseBody = responseBody;
            if (responseBody == null) {
                log.error("Empty response body for batch: {}, workId: {}", batchNum, workId);
                throw new IOException("Empty response body");
            }

            log.info("Successfully pulled prepackage data for batch: {}, workId: {}", batchNum, workId);
            return parseResponse(responseBody, batchNum, workId);
        } catch (IOException e) {
            lastCall.errorMessage = e.getMessage();
            throw e;
        } catch (RuntimeException e) {
            lastCall.errorMessage = e.getMessage();
            throw e;
        }
    }

    private PrepackageDataDTO parseResponse(String responseBody, String batchNum, String workId) throws IOException {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.has("PrePackageInfo")) {
            return objectMapper.treeToValue(root, PrepackageDataDTO.class);
        }
        ThirdPartyPrepackageResponseDTO dto = objectMapper.treeToValue(root, ThirdPartyPrepackageResponseDTO.class);
        return prepackageMapper.toPrepackageData(dto, batchNum, workId);
    }

    public LastCallSnapshot getLastCallSnapshot() {
        LastCall lastCall = lastCallHolder.get();
        if (lastCall == null) {
            return null;
        }
        return new LastCallSnapshot(lastCall);
    }

    public static class LastCallSnapshot {
        private final String url;
        private final String requestBody;
        private final Integer httpStatus;
        private final String responseBody;
        private final String errorMessage;

        private LastCallSnapshot(LastCall lastCall) {
            this.url = lastCall.url;
            this.requestBody = lastCall.requestBody;
            this.httpStatus = lastCall.httpStatus;
            this.responseBody = lastCall.responseBody;
            this.errorMessage = lastCall.errorMessage;
        }

        public String getUrl() {
            return url;
        }

        public String getRequestBody() {
            return requestBody;
        }

        public Integer getHttpStatus() {
            return httpStatus;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private static class LastCall {
        private String url;
        private String requestBody;
        private Integer httpStatus;
        private String responseBody;
        private String errorMessage;
    }
}
