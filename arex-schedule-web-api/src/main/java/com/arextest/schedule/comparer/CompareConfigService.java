package com.arextest.schedule.comparer;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.arextest.web.model.contract.contracts.config.replay.ComparisonSummaryConfiguration;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig;
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
        Map<String, ReplayComparisonConfig> operationCompareConfig = getReplayComparisonConfig(plan);

        if (operationCompareConfig.isEmpty()) {
            LOGGER.warn("no compare config found, plan id:{}", plan.getId());
            return;
        }

        for (ReplayActionItem actionItem : plan.getReplayActionItemList()) {
            String operationId = actionItem.getOperationId();

            ReplayComparisonConfig config = operationCompareConfig.getOrDefault(operationId, new ReplayComparisonConfig());

            customComparisonConfigurationHandler.build(config, actionItem);

            redisCacheProvider.put(key(actionItem.getId()).getBytes(StandardCharsets.UTF_8),
                    4 * 24 * 60 * 60L,
                    objectToJsonString(config).getBytes(StandardCharsets.UTF_8));

            LOGGER.info("prepare load compare config, action id:{}", actionItem.getId());
        }
    }

    private Map<String, ReplayComparisonConfig> getReplayComparisonConfig(ReplayPlan plan) {
        Map<String, String> urlVariables = Collections.singletonMap("appId", plan.getAppId());

        ResponseEntity<GenericResponseType<ReplayCompareConfig>> replayComparisonConfigEntity =
                httpWepServiceApiClient.get(summaryConfigUrl, urlVariables,
                        new ParameterizedTypeReference<GenericResponseType<ReplayCompareConfig>>() {
                        });

        if (replayComparisonConfigEntity == null) return Collections.emptyMap();
        List<ReplayCompareConfig.ReplayComparisonItem> operationConfigs = Optional.ofNullable(replayComparisonConfigEntity.getBody())
                .map(GenericResponseType::getBody)
                .map(ReplayCompareConfig::getReplayComparisonItems)
                .orElse(Collections.emptyList());

        return convertOperationConfig(operationConfigs);
    }

    private static Map<String, ReplayComparisonConfig> convertOperationConfig(List<ReplayCompareConfig.ReplayComparisonItem> operationConfigs) {
        Map<String, ReplayComparisonConfig> res = new HashMap<>();

        for (ReplayCompareConfig.ReplayComparisonItem source : operationConfigs) {
            String operationId = source.getOperationId();
            if (StringUtils.isBlank(operationId)) {
                LOGGER.warn("operation id is blank, operationId: {}", operationId);
                continue;
            }

            ReplayComparisonConfig converted = convertCompareItem(source);
            List<ReplayCompareConfig.DependencyComparisonItem> sourceDependencyConfigs = source.getDependencyComparisonItems();
            converted.setDependencyConfigMap(convertDependencyConfig(sourceDependencyConfigs));
            res.put(operationId, converted);
        }

        return res;
    }

    private static Map<String, ReplayComparisonConfig> convertDependencyConfig(List<ReplayCompareConfig.DependencyComparisonItem> dependencyConfigs) {
        Map<String, ReplayComparisonConfig> res = new HashMap<>();

        for (ReplayCompareConfig.DependencyComparisonItem source : dependencyConfigs) {
            if (CollectionUtils.isEmpty(source.getOperationType()) || StringUtils.isBlank(source.getOperationName())) {
                LOGGER.warn("dependency type or name is blank, dependencyId: {}", source.getDependencyId());
                continue;
            }

            String dependencyKey = dependencyKey(source);
            ReplayComparisonConfig converted = convertCompareItem(source);
            res.put(dependencyKey, converted);
        }

        return res;
    }

    private static ReplayComparisonConfig convertCompareItem(ComparisonSummaryConfiguration source) {
        ReplayComparisonConfig converted = new ReplayComparisonConfig();
        converted.setOperationType(source.getOperationType());
        converted.setOperationName(source.getOperationName());
        converted.setExclusionList(source.getExclusionList());
        converted.setInclusionList(source.getInclusionList());
        converted.setReferenceMap(source.getReferenceMap());
        converted.setListSortMap(source.getListSortMap());
        return converted;
    }

    public static String dependencyKey(ReplayCompareConfig.DependencyComparisonItem dependencyConfig) {
        return dependencyKey(dependencyConfig.getOperationType().get(0), dependencyConfig.getOperationName());
    }

    public static String dependencyKey(String type, String name) {
        return type + "_" + name;
    }

    public static ReplayComparisonConfig pickConfig(CompareItem compareItem, ReplayComparisonConfig operationConfig, String category) {
        if (compareItem.isEntryPointCategory()) {
            return operationConfig;
        }

        String depKey = CompareConfigService.dependencyKey(category, compareItem.getCompareOperation());
        return Optional.ofNullable(operationConfig.getDependencyConfigMap())
                .map(dependencyConfig -> dependencyConfig.get(depKey))
                .orElse(new ReplayComparisonConfig());
    }

    public static ReplayComparisonConfig pickConfig(ReplayComparisonConfig operationConfig, String category, String operationName) {
        boolean mainEntryType = Optional.ofNullable(operationConfig.getOperationType())
                .map(types -> types.contains(category)).orElse(false);
        boolean mainEntryNameMatched = Optional.ofNullable(operationConfig.getOperationName())
                .map(name -> name.equals(operationName)).orElse(false);

        if (mainEntryType && mainEntryNameMatched) {
            return operationConfig;
        }

        String depKey = CompareConfigService.dependencyKey(category, operationName);
        return Optional.ofNullable(operationConfig.getDependencyConfigMap())
                .map(dependencyConfig -> dependencyConfig.get(depKey))
                .orElse(new ReplayComparisonConfig());
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
        replayComparisonConfig.setExclusionList(Collections.emptySet());
        replayComparisonConfig.setInclusionList(Collections.emptySet());
        replayComparisonConfig.setListSortMap(Collections.emptyMap());
        replayComparisonConfig.setReferenceMap(Collections.emptyMap());
        return replayComparisonConfig;
    }
}