package com.arextest.schedule.service.exchange;

import com.arextest.common.model.response.GenericResponseType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.converter.ReplayConfigConverter;
import com.arextest.web.model.contract.contracts.config.replay.QueryCompareConfigRequestType;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig.ReplayComparisonItem;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    List<ReplayComparisonItem> operationConfigs = replayComparisonConfigEntity.getBody()
        .getBody().getReplayComparisonItems();

    return operationConfigs.stream()
        .filter(source -> StringUtils.isNotBlank(source.getOperationId()))
        .collect(
            Collectors.toMap(
                ReplayCompareConfig.ReplayComparisonItem::getOperationId,
                ReplayConfigConverter.INSTANCE::interfaceDaoFromDto, (a, b) -> a)
        );
  }
}
