package com.arextest.schedule.comparer;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.config.CompareExclusionsConfig;
import com.arextest.schedule.model.config.CompareInclusionsConfig;
import com.arextest.schedule.model.config.CompareListSortConfig;
import com.arextest.schedule.model.config.CompareReferenceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Value("${arex.report.config.comparison.exclusions.url}")
    private String configComparisonExclusionsUrl;
    @Value("${arex.report.config.comparison.inclusions.url}")
    private String configComparisonInclusionsUrl;
    @Value("${arex.report.config.comparison.listsort.url}")
    private String configComparisonListSortUrl;
    @Value("${arex.report.config.comparison.reference.url}")
    private String configComparisonReferenceUrl;

    @Resource
    private ObjectMapper objectMapper;

    public void preload(ReplayPlan plan) {
        Map<String, ReplayComparisonConfig> operationCompareConfig = new HashMap<>();

        if (!getExclusions(operationCompareConfig, plan) || !getInclusions(operationCompareConfig, plan) ||
                !getListSort(operationCompareConfig, plan) || !getReference(operationCompareConfig, plan)) {
            LOGGER.warn("prepare load appId comparison config empty");
            return;
        }

        for (ReplayActionItem actionItem : plan.getReplayActionItemList()) {
            String operationId = actionItem.getOperationId();
            ReplayComparisonConfig config = build(operationCompareConfig, operationId);
            String redisKey = key(actionItem.getId());
            String json = objectToJsonString(config);
            redisCacheProvider.put(redisKey.getBytes(StandardCharsets.UTF_8),
                    7200L, json.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("prepare load compare config, action id:{} ,result: {}", actionItem.getId(), json);
        }
    }

    private ReplayComparisonConfig build(Map<String, ReplayComparisonConfig> operationCompareConfig, String operationId) {
        ReplayComparisonConfig replayComparisonConfig = new ReplayComparisonConfig();
        ReplayComparisonConfig globalConfig = operationCompareConfig.getOrDefault(null, new ReplayComparisonConfig());
        ReplayComparisonConfig operationConfig = operationCompareConfig.getOrDefault(operationId, new ReplayComparisonConfig());

        List<String> ignoreTypeList = new ArrayList<>();
        ignoreTypeList.addAll(globalConfig.getIgnoreTypeList() == null ? Collections.emptyList() : globalConfig.getIgnoreTypeList());
        ignoreTypeList.addAll(operationConfig.getIgnoreTypeList() == null ? Collections.emptyList() : operationConfig.getIgnoreTypeList());
        replayComparisonConfig.setIgnoreTypeList(ignoreTypeList);

        List<String> ignoreKeyList = new ArrayList<>();
        ignoreKeyList.addAll(globalConfig.getIgnoreKeyList() == null ? Collections.emptyList() : globalConfig.getIgnoreKeyList());
        ignoreKeyList.addAll(operationConfig.getIgnoreKeyList() == null ? Collections.emptyList() : operationConfig.getIgnoreKeyList());
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

    private boolean getExclusions(Map<String, ReplayComparisonConfig> operationCompareConfig, ReplayPlan plan) {

        Map<String, String> urlVariables = Collections.singletonMap("appId", plan.getAppId());
        ResponseEntity<GenericResponseType<CompareExclusionsConfig>> compareExclusionsEntity =
                httpWepServiceApiClient.get(configComparisonExclusionsUrl, urlVariables,
                        new ParameterizedTypeReference<GenericResponseType<CompareExclusionsConfig>>() {
                        });

        if (compareExclusionsEntity == null) {
            return false;
        }
        GenericResponseType<CompareExclusionsConfig> body = compareExclusionsEntity.getBody();
        if (body != null && body.getBody() != null) {
            List<CompareExclusionsConfig> details = body.getBody();
            for (CompareExclusionsConfig compareExclusionsConfig : details) {
                String operationId = compareExclusionsConfig.getOperationId();
                List<String> exclusions = compareExclusionsConfig.getExclusions();

                ReplayComparisonConfig tempReplayComparisonConfig = operationCompareConfig.getOrDefault(operationId,
                        new ReplayComparisonConfig());
                Set<List<String>> tempExclusionList = tempReplayComparisonConfig.getExclusionList() ==
                        null ? new HashSet<>() : tempReplayComparisonConfig.getExclusionList();

                if (CollectionUtils.isNotEmpty(exclusions)) {
                    tempExclusionList.add(exclusions);
                    tempReplayComparisonConfig.setExclusionList(tempExclusionList);
                }
                operationCompareConfig.put(operationId, tempReplayComparisonConfig);
            }
        }
        return true;
    }

    private boolean getInclusions(Map<String, ReplayComparisonConfig> operationCompareConfig, ReplayPlan plan) {
        Map<String, String> urlVariables = Collections.singletonMap("appId", plan.getAppId());
        ResponseEntity<GenericResponseType<CompareInclusionsConfig>> responseEntity =
                httpWepServiceApiClient.get(configComparisonInclusionsUrl, urlVariables,
                        new ParameterizedTypeReference<GenericResponseType<CompareInclusionsConfig>>() {
                        });
        if (responseEntity == null) {
            return false;
        }
        GenericResponseType<CompareInclusionsConfig> body = responseEntity.getBody();
        if (body != null && body.getBody() != null) {
            List<CompareInclusionsConfig> details = body.getBody();
            for (CompareInclusionsConfig compareInclusionsConfig : details) {
                String operationId = compareInclusionsConfig.getOperationId();
                List<String> inclusions = compareInclusionsConfig.getInclusions();

                ReplayComparisonConfig tempReplayComparisonConfig = operationCompareConfig.getOrDefault(operationId,
                        new ReplayComparisonConfig());
                Set<List<String>> tempInclusionList = tempReplayComparisonConfig.getInclusionList() ==
                        null ? new HashSet<>() : tempReplayComparisonConfig.getInclusionList();

                if (CollectionUtils.isNotEmpty(inclusions)) {
                    tempInclusionList.add(inclusions);
                    tempReplayComparisonConfig.setInclusionList(tempInclusionList);
                    operationCompareConfig.put(operationId, tempReplayComparisonConfig);
                }
            }
        }
        return true;
    }

    private boolean getListSort(Map<String, ReplayComparisonConfig> operationCompareConfig, ReplayPlan plan) {
        Map<String, String> urlVariables = Collections.singletonMap("appId", plan.getAppId());
        ResponseEntity<GenericResponseType<CompareListSortConfig>> responseEntity =
                httpWepServiceApiClient.get(configComparisonListSortUrl, urlVariables,
                        new ParameterizedTypeReference<GenericResponseType<CompareListSortConfig>>() {
                        });
        if (responseEntity == null) {
            return false;
        }
        GenericResponseType<CompareListSortConfig> body = responseEntity.getBody();
        if (body != null && body.getBody() != null) {
            List<CompareListSortConfig> details = body.getBody();
            for (CompareListSortConfig compareListSortConfig : details) {
                String operationId = compareListSortConfig.getOperationId();
                List<String> listPath = compareListSortConfig.getListPath();
                List<List<String>> keys = compareListSortConfig.getKeys();

                ReplayComparisonConfig tempReplayComparisonConfig = operationCompareConfig.getOrDefault(operationId, new ReplayComparisonConfig());
                Map<List<String>, List<List<String>>> tempListSortMap = tempReplayComparisonConfig.getListSortMap() ==
                        null ? new HashMap<>() : tempReplayComparisonConfig.getListSortMap();

                if (CollectionUtils.isNotEmpty(listPath) && CollectionUtils.isNotEmpty(keys)) {
                    tempListSortMap.put(listPath, keys);
                    tempReplayComparisonConfig.setListSortMap(tempListSortMap);
                    operationCompareConfig.put(operationId, tempReplayComparisonConfig);
                }
            }
        }
        return true;
    }

    private boolean getReference(Map<String, ReplayComparisonConfig> operationCompareConfig, ReplayPlan plan) {
        Map<String, String> urlVariables = Collections.singletonMap("appId", plan.getAppId());
        ResponseEntity<GenericResponseType<CompareReferenceConfig>> responseEntity =
                httpWepServiceApiClient.get(configComparisonReferenceUrl, urlVariables,
                        new ParameterizedTypeReference<GenericResponseType<CompareReferenceConfig>>() {
                        });
        if (responseEntity == null) {
            return false;
        }
        GenericResponseType<CompareReferenceConfig> body = responseEntity.getBody();
        if (body != null && body.getBody() != null) {
            List<CompareReferenceConfig> details = body.getBody();
            for (CompareReferenceConfig compareReferenceConfig : details) {
                String operationId = compareReferenceConfig.getOperationId();
                List<String> fkPath = compareReferenceConfig.getFkPath();
                List<String> pkPath = compareReferenceConfig.getPkPath();

                ReplayComparisonConfig tempReplayComparisonConfig = operationCompareConfig.getOrDefault(operationId, new ReplayComparisonConfig());
                Map<List<String>, List<String>> tempReferenceMap = tempReplayComparisonConfig.getReferenceMap() ==
                        null ? new HashMap<>() : tempReplayComparisonConfig.getReferenceMap();

                if (CollectionUtils.isNotEmpty(fkPath) && CollectionUtils.isNotEmpty(pkPath)) {
                    tempReferenceMap.put(fkPath, pkPath);
                    tempReplayComparisonConfig.setReferenceMap(tempReferenceMap);
                    operationCompareConfig.put(operationId, tempReplayComparisonConfig);
                }
            }
        }
        return true;
    }


    @Data
    private final static class GenericResponseType<T> {
        private List<T> body;
    }

    public ReplayComparisonConfig loadConfig(ReplayActionItem actionItem) {
        try {
            String redisKey = key(actionItem.getId());
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
        replayComparisonConfig.setIgnoreKeyList(Collections.emptyList());
        replayComparisonConfig.setIgnoreTypeList(Collections.emptyList());
        replayComparisonConfig.setExclusionList(Collections.emptySet());
        replayComparisonConfig.setInclusionList(Collections.emptySet());
        replayComparisonConfig.setListSortMap(Collections.emptyMap());
        replayComparisonConfig.setReferenceMap(Collections.emptyMap());
        return replayComparisonConfig;
    }
}