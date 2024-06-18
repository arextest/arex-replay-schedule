package com.arextest.schedule.service.collection;

import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.CompareResult;
import com.arextest.diff.model.enumeration.DiffResultCode;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.diff.model.log.NodeEntity;
import com.arextest.diff.model.log.UnmatchedPairEntity;
import com.arextest.schedule.comparer.CompareService;
import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.model.collection.CompareMsgRequestType;
import com.arextest.schedule.model.collection.CompareMsgResponseType;
import com.arextest.schedule.model.collection.CompareMsgResponseType.LogDetail;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.service.exchange.ReportExchangeService;
import com.arextest.schedule.utils.ListUtils;
import com.arextest.schedule.utils.MapUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CollectionReportService {

  @Resource
  CompareService compareService;

  @Resource
  ReportExchangeService reportExchangeService;

  @Resource
  CustomComparisonConfigurationHandler configHandler;

  public CompareMsgResponseType compareMsg(CompareMsgRequestType request) {
    CompareMsgResponseType result = new CompareMsgResponseType();
    String baseMsg = request.getBaseMsg();
    String testMsg = request.getTestMsg();
    CompareOptions compareOptions = buildSkdOption(request.getAppId(), request.getOperationName());
    CompareResult compareResult = null;
    try {
      compareResult = compareService.compare(baseMsg, testMsg, compareOptions);
    } catch (RuntimeException e) {
      LOGGER.error("compareMsg error, msg:{}, exceptionMsg:{}", request, e.getMessage());
      result.setDiffResultCode(DiffResultCode.COMPARED_INTERNAL_EXCEPTION);
      result.setExceptionMsg(e.getMessage());
    }
    if (compareResult == null) {
      LOGGER.error("compareResult is null, msg:{}", request);
      return result;
    }
    fillCompareResult(result, compareResult);
    return result;
  }

  private CompareOptions buildSkdOption(String appId, String operationName) {
    Map<String, ComparisonInterfaceConfig> replayCompareConfig =
        reportExchangeService.getReplayCompareConfig(appId, operationName);
    if (MapUtils.isNotEmpty(replayCompareConfig)) {
      List<Entry<String, ComparisonInterfaceConfig>> entries = new ArrayList<>(
          replayCompareConfig.entrySet());
      ComparisonInterfaceConfig comparisonInterfaceConfig = entries.get(0).getValue();
      List<String> operationTypes = comparisonInterfaceConfig.getOperationTypes();
      String operationType =
          CollectionUtils.isEmpty(operationTypes) ? StringUtils.EMPTY : operationTypes.get(0);
      return configHandler.buildSkdOption(operationType, comparisonInterfaceConfig);
    }
    return CompareOptions.options();
  }


  private void fillCompareResult(CompareMsgResponseType compareResultDetail,
      CompareResult compareResult) {
    compareResultDetail.setDiffResultCode(compareResult.getCode());
    compareResultDetail.setBaseMsg(compareResult.getProcessedBaseMsg());
    compareResultDetail.setTestMsg(compareResult.getProcessedTestMsg());

    List<LogEntity> logEntities = compareResult.getLogs();
    if (CollectionUtils.isEmpty(logEntities)) {
      return;
    }

    if (compareResult.getCode() == DiffResultCode.COMPARED_INTERNAL_EXCEPTION) {
      LogEntity logEntity = logEntities.get(0);
      LogDetail logDetail = new LogDetail();
      logDetail.setNodePath(Collections.emptyList());
      logDetail.setLogEntity(logEntity);
      compareResultDetail.setLogDetails(Collections.singletonList(logDetail));
      compareResultDetail.setExceptionMsg(logEntity.getLogInfo());
    } else {
      Map<MutablePair<String, Integer>, LogDetail> logDetailMap = new HashMap<>();

      logEntities.forEach(logEntity -> {
        UnmatchedPairEntity pathPair = logEntity.getPathPair();
        int unmatchedType = pathPair.getUnmatchedType();
        List<NodeEntity> leftUnmatchedPath = pathPair.getLeftUnmatchedPath();
        List<NodeEntity> rightUnmatchedPath = pathPair.getRightUnmatchedPath();
        int leftUnmatchedPathSize = leftUnmatchedPath == null ? 0 : leftUnmatchedPath.size();
        int rightUnmatchedPathSize = rightUnmatchedPath == null ? 0 : rightUnmatchedPath.size();
        List<NodeEntity> nodePath =
            leftUnmatchedPathSize >= rightUnmatchedPathSize ? leftUnmatchedPath
                : rightUnmatchedPath;
        MutablePair<String, Integer> tempPair =
            new MutablePair<>(ListUtils.getFuzzyPathStr(nodePath), unmatchedType);

        logDetailMap.compute(tempPair, (key, logDetail) -> {
          if (logDetail == null) {
            logDetail = new LogDetail();
            logDetail.setNodePath(nodePath);
            logDetail.setLogEntity(logEntity);
          }
          logDetail.setCount(logDetail.getCount() + 1);
          return logDetail;
        });
      });
      compareResultDetail.setLogDetails(new ArrayList<>(logDetailMap.values()));
    }
  }


}