package com.arextest.schedule.comparer;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.arextest.schedule.model.plan.PlanStageEnum;
import com.arextest.schedule.model.plan.StageStatusEnum;
import com.arextest.schedule.utils.StageUtils;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by wang_yc on 2021/10/14
 */
@Slf4j
@Component
public final class CompareConfigService {
    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;
    @Resource
    private CacheProvider redisCacheProvider;

    @Value("${arex.report.config.comparison.summary.url}")
    private String summaryConfigUrl;

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    CustomComparisonConfigurationHandler customComparisonConfigurationHandler;

    public void preload(ReplayPlan plan) {
        Map<String, ReplayComparisonConfig> operationCompareConfig = new HashMap<>();
        StageUtils.updateStage(PlanStageEnum.LOADING_CONFIG, System.currentTimeMillis(), null,
            StageStatusEnum.ONGOING, null, plan.getReplayPlanStageList());

        if (!getReplayComparisonConfig(operationCompareConfig, plan)) {
            LOGGER.warn("prepare load appId comparison config empty");
            StageUtils.updateStage(PlanStageEnum.LOADING_CONFIG, null, System.currentTimeMillis(),
                StageStatusEnum.FAILED, null, plan.getReplayPlanStageList());
            return;
        }
        StageUtils.updateStage(PlanStageEnum.LOADING_CONFIG, null, System.currentTimeMillis(),
            StageStatusEnum.SUCCEEDED, null, plan.getReplayPlanStageList());

        for (ReplayActionItem actionItem : plan.getReplayActionItemList()) {
            String operationId = actionItem.getOperationId();
            ReplayComparisonConfig config = build(operationCompareConfig, operationId);
            customComparisonConfigurationHandler.build(config, actionItem);
            String redisKey = key(actionItem.getId());
            String json = objectToJsonString(config);
            redisCacheProvider.put(redisKey.getBytes(StandardCharsets.UTF_8),
                    4 * 24 * 60 * 60L, json.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("prepare load compare config, action id:{} ,result: {}", actionItem.getId(), json);
        }
    }

    private ReplayComparisonConfig build(Map<String, ReplayComparisonConfig> operationCompareConfig, String operationId) {
        ReplayComparisonConfig replayComparisonConfig = new ReplayComparisonConfig();
        ReplayComparisonConfig globalConfig = operationCompareConfig.getOrDefault(null, new ReplayComparisonConfig());
        ReplayComparisonConfig operationConfig = operationCompareConfig.getOrDefault(operationId, new ReplayComparisonConfig());

        Set<String> ignoreTypeList = new HashSet<>();
        ignoreTypeList.addAll(globalConfig.getIgnoreTypeList() == null ? Collections.emptySet() : globalConfig.getIgnoreTypeList());
        ignoreTypeList.addAll(operationConfig.getIgnoreTypeList() == null ? Collections.emptySet() : operationConfig.getIgnoreTypeList());
        replayComparisonConfig.setIgnoreTypeList(ignoreTypeList);

        Set<String> ignoreKeyList = new HashSet<>();
        ignoreKeyList.addAll(globalConfig.getIgnoreKeyList() == null ? Collections.emptySet() : globalConfig.getIgnoreKeyList());
        ignoreKeyList.addAll(operationConfig.getIgnoreKeyList() == null ? Collections.emptySet() : operationConfig.getIgnoreKeyList());
        replayComparisonConfig.setIgnoreKeyList(ignoreKeyList);


        Set<List<String>> exclusions = new HashSet<>();
        exclusions.addAll(globalConfig.getExclusionList() == null ? Collections.emptySet() : globalConfig.getExclusionList());
        exclusions.addAll(operationConfig.getExclusionList() == null ? Collections.emptySet() : operationConfig.getExclusionList());
        replayComparisonConfig.setExclusionList(exclusions);

        Set<List<String>> inclusions = new HashSet<>();
        inclusions.addAll(globalConfig.getInclusionList() == null ? Collections.emptyList() : globalConfig.getInclusionList());
        inclusions.addAll(operationConfig.getInclusionList() == null ? Collections.emptyList() : operationConfig.getInclusionList());
        replayComparisonConfig.setInclusionList(inclusions);

        Map<List<String>, List<List<String>>> listSortMap = new HashMap<>();
        listSortMap.putAll(globalConfig.getListSortMap() == null ? Collections.emptyMap() : globalConfig.getListSortMap());
        listSortMap.putAll(operationConfig.getListSortMap() == null ? Collections.emptyMap() : operationConfig.getListSortMap());
        replayComparisonConfig.setListSortMap(listSortMap);

        Map<List<String>, List<String>> referenceMap = new HashMap<>();
        referenceMap.putAll(globalConfig.getReferenceMap() == null ? Collections.emptyMap() : globalConfig.getReferenceMap());
        referenceMap.putAll(operationConfig.getReferenceMap() == null ? Collections.emptyMap() : operationConfig.getReferenceMap());
        replayComparisonConfig.setReferenceMap(referenceMap);

        return replayComparisonConfig;
    }

    private boolean getReplayComparisonConfig(Map<String, ReplayComparisonConfig> operationCompareConfig, ReplayPlan plan) {
        Map<String, String> urlVariables = Collections.singletonMap("appId", plan.getAppId());

        ResponseEntity<GenericResponseType<ReplayCompareConfig>> replayComparisonConfigEntity =
                httpWepServiceApiClient.get(summaryConfigUrl, urlVariables,
                        new ParameterizedTypeReference<GenericResponseType<ReplayCompareConfig>>() {
                        });

        if (replayComparisonConfigEntity == null) return false;
        GenericResponseType<ReplayCompareConfig> body = replayComparisonConfigEntity.getBody();
        if (body != null && body.getBody() != null) {
            ReplayCompareConfig detail =  body.getBody();
            for(ReplayCompareConfig.ReplayComparisonItem replayComparisonItem : detail.getReplayComparisonItems()) {
                String operationId = replayComparisonItem.getOperationId();
                ReplayComparisonConfig tempReplayComparisonConfig = operationCompareConfig.getOrDefault(operationId,
                        new ReplayComparisonConfig());

                if (replayComparisonItem.getExclusionList() != null) {
                    Set<List<String>> exclusionList = Optional.ofNullable(tempReplayComparisonConfig.getExclusionList())
                            .orElse(new HashSet<>());
                    exclusionList.addAll(replayComparisonItem.getExclusionList());
                    tempReplayComparisonConfig.setExclusionList(exclusionList);
                }
                if (replayComparisonItem.getInclusionList() != null) {
                    Set<List<String>> inclusionList = Optional.ofNullable(tempReplayComparisonConfig.getInclusionList())
                            .orElse(new HashSet<>());
                    inclusionList.addAll(replayComparisonItem.getInclusionList());
                    tempReplayComparisonConfig.setInclusionList(inclusionList);
                }
                if (replayComparisonItem.getReferenceMap() != null) {
                    Map<List<String>, List<String>> referenceMap = Optional.ofNullable(
                            tempReplayComparisonConfig.getReferenceMap()).orElse(new HashMap<>());
                    referenceMap.putAll(replayComparisonItem.getReferenceMap());
                    tempReplayComparisonConfig.setReferenceMap(referenceMap);
                }
                if (replayComparisonItem.getListSortMap() != null) {
                    Map<List<String>, List<List<String>>> listSortMap = Optional.ofNullable(
                            tempReplayComparisonConfig.getListSortMap()).orElse(new HashMap<>());
                    listSortMap.putAll(replayComparisonItem.getListSortMap());
                    tempReplayComparisonConfig.setListSortMap(listSortMap);
                }
                operationCompareConfig.put(operationId, tempReplayComparisonConfig);
            }
        }
        return  true;
    }

    @Data
    private final static class GenericResponseType<T> {
        private T body;
    }

    public ReplayComparisonConfig loadConfig(ReplayActionItem actionItem) {
        return this.loadConfig(actionItem.getId());
    }


    public ReplayComparisonConfig loadConfig(String actionItemId) {
        try {
            String redisKey = key(actionItemId);
            byte[] json = redisCacheProvider.get(redisKey.getBytes(StandardCharsets.UTF_8));
            if (json == null) {
                return newEmptyComparisonConfig();
            }
            ReplayComparisonConfig config = byteToObject(json, ReplayComparisonConfig.class);
            if (config == null) {
                return newEmptyComparisonConfig();
            }
            return config;
        } catch (Throwable throwable) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return newEmptyComparisonConfig();
    }

    private <T> T byteToObject(byte[] bytes, Class<T> tClass) {
        try {
            return objectMapper.readValue(bytes, tClass);
        } catch (IOException e) {
            LOGGER.error("byteToObject error:{}", e.getMessage(), e);
        }
        return null;
    }

    private String objectToJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            LOGGER.error("byteToObject error:{}", e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private String key(String actionId) {
        return CommonConstant.COMPARE_CONFIG_REDIS_KEY + actionId;
    }

    private ReplayComparisonConfig newEmptyComparisonConfig() {
        ReplayComparisonConfig replayComparisonConfig = new ReplayComparisonConfig();
        replayComparisonConfig.setIgnoreKeyList(Collections.emptySet());
        replayComparisonConfig.setIgnoreTypeList(Collections.emptySet());
        replayComparisonConfig.setExclusionList(Collections.emptySet());
        replayComparisonConfig.setInclusionList(Collections.emptySet());
        replayComparisonConfig.setListSortMap(Collections.emptyMap());
        replayComparisonConfig.setReferenceMap(Collections.emptyMap());
        return replayComparisonConfig;
    }
}