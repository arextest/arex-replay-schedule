package com.arextest.schedule.comparer;

import com.arextest.common.cache.CacheProvider;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.config.ComparisonDependencyConfig;
import com.arextest.schedule.model.config.ComparisonGlobalConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.arextest.schedule.model.converter.ReplayConfigConverter;
import com.arextest.schedule.progress.ProgressEvent;
import com.arextest.schedule.utils.MapUtils;
import com.arextest.web.model.contract.contracts.config.SystemConfig;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig.DependencyComparisonItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Created by wang_yc on 2021/10/14
 */
@Slf4j
@Component
public final class CompareConfigService {

  @Resource
  CustomComparisonConfigurationHandler customComparisonConfigurationHandler;
  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;
  @Resource
  private CacheProvider redisCacheProvider;
  @Value("${arex.api.config.comparison.summary.url}")
  private String summaryConfigUrl;
  @Value("${arex.api.config.system.url}")
  private String systemConfigUrl;
  @Resource
  private ProgressEvent progressEvent;
  @Resource
  private ObjectMapper objectMapper;

  private static final RetryTemplate RETRY_TEMPLATE = RetryTemplate.builder().maxAttempts(3)
      .fixedBackoff(200L)
      .build();
  private static final long DEFAULT_COMPARE_IGNORE_TIME_PRECISION_MILLIS = 2000;
  private static final boolean DEFAULT_COMPARE_NAME_TO_LOWER = true;
  private static final boolean DEFAULT_COMPARE_NULL_EQUALS_EMPTY = true;
  private static final boolean DEFAULT_COMPARE_ONLY_COMPARE_COINCIDENT_COLUMN = true;
  private static final boolean DEFAULT_COMPARE_SELECT_IGNORE_COMPARE = true;
  private static final boolean DEFAULT_COMPARE_UUID_IGNORE = true;

  private static SystemConfig systemConfig = null;

  public SystemConfig getComparisonSystemConfig() {

    if (systemConfig != null) {
      return systemConfig;
    }

    ResponseEntity<GenericResponseType<SystemConfig>> response = RETRY_TEMPLATE.execute(context -> {
      ResponseEntity<GenericResponseType<SystemConfig>> temp = httpWepServiceApiClient.get(
          systemConfigUrl, Collections.emptyMap(),
          new ParameterizedTypeReference<GenericResponseType<SystemConfig>>() {
          });

      if (temp == null || temp.getBody() == null || temp.getBody().getBody() == null) {
        throw new RuntimeException("get compare system config failed");
      } else {
        return temp;
      }
    }, retryContext -> null);

    if (response == null || response.getBody() == null || response.getBody().getBody() == null) {
      LOGGER.error("get compare system config failed");
      SystemConfig defaultConfig = new SystemConfig();
      defaultConfig.setCompareIgnoreTimePrecisionMillis(
          DEFAULT_COMPARE_IGNORE_TIME_PRECISION_MILLIS);
      defaultConfig.setCompareNameToLower(DEFAULT_COMPARE_NAME_TO_LOWER);
      defaultConfig.setCompareNullEqualsEmpty(DEFAULT_COMPARE_NULL_EQUALS_EMPTY);
      defaultConfig.setOnlyCompareCoincidentColumn(DEFAULT_COMPARE_ONLY_COMPARE_COINCIDENT_COLUMN);
      defaultConfig.setSelectIgnoreCompare(DEFAULT_COMPARE_SELECT_IGNORE_COMPARE);
      defaultConfig.setUuidIgnore(DEFAULT_COMPARE_UUID_IGNORE);
      systemConfig = defaultConfig;
    } else {
      systemConfig = response.getBody().getBody();
    }

    return systemConfig;
  }

  public void preload(ReplayPlan plan) {
    progressEvent.onCompareConfigBeforeLoading(plan);
    Map<String, ComparisonInterfaceConfig> operationCompareConfig = getReplayComparisonConfig(plan);

    if (operationCompareConfig.isEmpty()) {
      LOGGER.warn("no compare config found, plan id:{}", plan.getId());
      return;
    }

    for (ReplayActionItem actionItem : plan.getReplayActionItemList()) {
      if (actionItem.getReplayCaseCount() == 0) {
        continue;
      }
      String operationId = actionItem.getOperationId();

      ReplayComparisonConfig config = operationCompareConfig.getOrDefault(operationId,
          new ComparisonInterfaceConfig());

      customComparisonConfigurationHandler.build(config, actionItem);

      redisCacheProvider.put(ComparisonInterfaceConfig.dependencyKey(actionItem.getId())
              .getBytes(StandardCharsets.UTF_8),
          4 * 24 * 60 * 60L,
          objectToJsonString(config).getBytes(StandardCharsets.UTF_8));

      LOGGER.info("prepare load compare config, action id:{}", actionItem.getId());
    }
    progressEvent.onCompareConfigLoaded(plan);
  }

  public ComparisonInterfaceConfig loadInterfaceConfig(ReplayActionItem actionItem) {
    return this.loadInterfaceConfig(actionItem.getId());
  }

  public ComparisonInterfaceConfig loadInterfaceConfig(String actionItemId) {
    try {
      String redisKey = ComparisonInterfaceConfig.dependencyKey(actionItemId);
      byte[] json = redisCacheProvider.get(redisKey.getBytes(StandardCharsets.UTF_8));
      if (json == null) {
        return ComparisonInterfaceConfig.empty();
      }
      ComparisonInterfaceConfig config = byteToObject(json, ComparisonInterfaceConfig.class);
      if (config == null) {
        return ComparisonInterfaceConfig.empty();
      }
      return config;
    } catch (Throwable throwable) {
      LOGGER.error(throwable.getMessage(), throwable);
    }
    return ComparisonInterfaceConfig.empty();
  }

  @Deprecated
  public ComparisonGlobalConfig loadGlobalConfig(String planId) {
    try {
      String redisKey = ComparisonGlobalConfig.dependencyKey(planId);
      byte[] json = redisCacheProvider.get(redisKey.getBytes(StandardCharsets.UTF_8));
      if (json == null) {
        return ComparisonGlobalConfig.empty();
      }
      ComparisonGlobalConfig config = byteToObject(json, ComparisonGlobalConfig.class);
      if (config == null) {
        return ComparisonGlobalConfig.empty();
      }
      return config;
    } catch (Throwable throwable) {
      LOGGER.error(throwable.getMessage(), throwable);
    }
    return ComparisonGlobalConfig.empty();
  }

  private Map<String, ComparisonInterfaceConfig> getReplayComparisonConfig(
      ReplayPlan plan) {
    Map<String, String> urlVariables = Collections.singletonMap("appId", plan.getAppId());

    ResponseEntity<GenericResponseType<ReplayCompareConfig>> replayComparisonConfigEntity =
        httpWepServiceApiClient.get(summaryConfigUrl, urlVariables,
            new ParameterizedTypeReference<GenericResponseType<ReplayCompareConfig>>() {
            });

    if (replayComparisonConfigEntity == null) {
      return new HashMap<>();
    }

    List<ReplayCompareConfig.ReplayComparisonItem> operationConfigs = Optional.ofNullable(
            replayComparisonConfigEntity.getBody())
        .map(GenericResponseType::getBody)
        .map(ReplayCompareConfig::getReplayComparisonItems)
        .orElse(Collections.emptyList());
    // TODO: add the log of the ComparisonConfig

    // converts
    Map<String, ComparisonInterfaceConfig> opConverted = convertOperationConfig(operationConfigs);
    this.setContractChangeFlag(opConverted,
        replayComparisonConfigEntity.getBody().getBody().getSkipAssemble());
    return opConverted;
  }

  private static Map<String, ComparisonInterfaceConfig> convertOperationConfig(
      List<ReplayCompareConfig.ReplayComparisonItem> operationConfigs) {
    Map<String, ComparisonInterfaceConfig> res = new HashMap<>();

    for (ReplayCompareConfig.ReplayComparisonItem source : operationConfigs) {
      String operationId = source.getOperationId();
      if (StringUtils.isBlank(operationId)) {
        LOGGER.warn("operation id is blank, operationId: {}", operationId);
        continue;
      }

      ComparisonInterfaceConfig converted = ReplayConfigConverter.INSTANCE.interfaceDaoFromDto(
          source);
      List<ReplayCompareConfig.DependencyComparisonItem> sourceDependencyConfigs = source.getDependencyComparisonItems();
      converted.setDependencyConfigMap(convertDependencyConfig(sourceDependencyConfigs));
      DependencyComparisonItem defaultDependencyComparisonItem = source.getDefaultDependencyComparisonItem();
      converted.setDefaultDependencyConfig(
          ReplayConfigConverter.INSTANCE.dependencyDaoFromDto(defaultDependencyComparisonItem));
      res.put(operationId, converted);
    }

    return res;
  }

  private static Map<String, ComparisonDependencyConfig> convertDependencyConfig(
      List<ReplayCompareConfig.DependencyComparisonItem> dependencyConfigs) {
    Map<String, ComparisonDependencyConfig> res = new HashMap<>();

    for (ReplayCompareConfig.DependencyComparisonItem source : dependencyConfigs) {
      if (CollectionUtils.isEmpty(source.getOperationTypes()) || StringUtils.isBlank(
          source.getOperationName())) {
        LOGGER.warn("dependency type or name is blank, dependencyId: {}", source.getDependencyId());
        continue;
      }

      String dependencyKey = ComparisonDependencyConfig.dependencyKey(source);
      ComparisonDependencyConfig converted = ReplayConfigConverter.INSTANCE.dependencyDaoFromDto(
          source);
      res.put(dependencyKey, converted);
    }

    return res;
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

  @Data
  private final static class GenericResponseType<T> {

    private T body;
  }

  private void setContractChangeFlag(Map<String, ComparisonInterfaceConfig> opConverted,
      boolean skipAssemble) {
    if (MapUtils.isNotEmpty(opConverted)) {
      opConverted.forEach((k, v) -> v.setSkipAssemble(skipAssemble));
    }
  }

}