package com.arextest.schedule.service.exchange;

import com.arextest.common.model.response.GenericResponseType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.converter.ReplayConfigConverter;
import com.arextest.web.model.contract.contracts.config.replay.QueryCompareConfigRequestType;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig.ReplayComparisonItem;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportExchangeService {

  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;

  @Value("${arex.api.config.comparison.queryCompareConfig.url}")
  private String queryCompareConfigUrl;

  public Map<String, ComparisonInterfaceConfig> getReplayCompareConfig(String appId,
      String operationName) {

    QueryCompareConfigRequestType queryCompareConfigRequestType = new QueryCompareConfigRequestType();
    queryCompareConfigRequestType.setAppId(appId);
    queryCompareConfigRequestType.setOperationName(operationName);

    ResponseEntity<GenericResponseType<ReplayCompareConfig>> replayComparisonConfigEntity =
        httpWepServiceApiClient.retryJsonPost(
            queryCompareConfigUrl, queryCompareConfigRequestType,
            new ParameterizedTypeReference<GenericResponseType<ReplayCompareConfig>>() {
            });

    if (replayComparisonConfigEntity == null || replayComparisonConfigEntity.getBody() == null
        || replayComparisonConfigEntity.getBody().getBody() == null) {
      return Collections.emptyMap();
    }

    List<ReplayComparisonItem> operationConfigs = Optional.ofNullable(
            replayComparisonConfigEntity.getBody()).map(GenericResponseType::getBody)
        .map(ReplayCompareConfig::getReplayComparisonItems).orElse(Collections.emptyList());

    return convertOperationConfig(operationConfigs);
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
      res.put(operationId, converted);
    }
    return res;
  }

}
