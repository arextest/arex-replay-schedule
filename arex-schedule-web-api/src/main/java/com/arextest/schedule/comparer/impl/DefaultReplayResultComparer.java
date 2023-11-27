package com.arextest.schedule.comparer.impl;

import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.CompareResult;
import com.arextest.diff.model.enumeration.DiffResultCode;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.comparer.EncodingUtils;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareModeType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.LogType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.config.ComparisonGlobalConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.service.MetricService;
import com.arextest.web.model.contract.contracts.config.SystemConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StopWatch;

@Slf4j
@Builder
public class DefaultReplayResultComparer implements ReplayResultComparer {

  private static final CompareSDK COMPARE_INSTANCE = new CompareSDK();
  private static final long MAX_TIME = Long.MAX_VALUE;
  private static final String APPLICATION_PROPERTIES_NAME = "/application.properties";
  private static final String IGNORE_TIME_PRECISION_MILLIS_KEY = "ignore.time.precision.millis";
  private static long ignoreTimePrecisionMillis = 2000;

  private final CompareConfigService compareConfigService;
  private final PrepareCompareSourceRemoteLoader sourceRemoteLoader;
  private final ProgressTracer progressTracer;
  private final ComparisonWriter comparisonOutputWriter;
  private final ReplayActionCaseItemRepository caseItemRepository;
  private final MetricService metricService;
  private final CustomComparisonConfigurationHandler configHandler;

  public DefaultReplayResultComparer(CompareConfigService compareConfigService,
      PrepareCompareSourceRemoteLoader sourceRemoteLoader,
      ProgressTracer progressTracer,
      ComparisonWriter comparisonOutputWriter,
      ReplayActionCaseItemRepository caseItemRepository,
      MetricService metricService,
      CustomComparisonConfigurationHandler configHandler) {
    this.compareConfigService = compareConfigService;
    this.sourceRemoteLoader = sourceRemoteLoader;
    this.progressTracer = progressTracer;
    this.comparisonOutputWriter = comparisonOutputWriter;
    this.caseItemRepository = caseItemRepository;
    this.metricService = metricService;
    this.configHandler = configHandler;
    addGlobalOptionToSDK(compareConfigService);
  }
  public void addGlobalOptionToSDK(CompareConfigService compareConfigService) {
    SystemConfig comparisonSystemConfig = compareConfigService.getComparisonSystemConfig();
    COMPARE_INSTANCE.getGlobalOptions()
        .putNameToLower(comparisonSystemConfig.getCompareNameToLower())
        .putNullEqualsEmpty(comparisonSystemConfig.getCompareNullEqualsEmpty())
        .putIgnoredTimePrecision(comparisonSystemConfig.getCompareIgnoreTimePrecisionMillis())
        .putIgnoreNodeSet(comparisonSystemConfig.getIgnoreNodeSet())
        .putSelectIgnoreCompare(comparisonSystemConfig.getSelectIgnoreCompare())
        .putOnlyCompareCoincidentColumn(comparisonSystemConfig.getOnlyCompareCoincidentColumn())
        .putUuidIgnore(comparisonSystemConfig.getUuidIgnore());
  }

  public static CompareSDK getCompareSDKInstance() {
    return COMPARE_INSTANCE;
  }

  @Override
  public boolean compare(ReplayActionCaseItem caseItem, boolean useReplayId) {
    StopWatch compareWatch = new StopWatch();
    compareWatch.start(LogType.COMPARE.getValue());
    String planId = caseItem.getParent().getPlanId();
    try {
      MDCTracer.addPlanId(planId);
      MDCTracer.addPlanItemId(caseItem.getPlanItemId());

      List<CategoryComparisonHolder> waitCompareMap =
          sourceRemoteLoader.buildWaitCompareList(caseItem, useReplayId);
      if (CollectionUtils.isEmpty(waitCompareMap)) {
        caseItemRepository.updateCompareStatus(caseItem.getId(),
            CompareProcessStatusType.ERROR.getValue());
        caseItem.setCompareStatus(CompareProcessStatusType.ERROR.getValue());
        comparisonOutputWriter.writeIncomparable(caseItem,
            CaseSendStatusType.REPLAY_RESULT_NOT_FOUND.name());
        return true;
      }

      List<ReplayCompareResult> replayCompareResults = this.doContentCompare(caseItem,
          waitCompareMap);

      if (CollectionUtils.isEmpty(replayCompareResults)
          && MockCategoryType.Q_MESSAGE_CONSUMER.getName()
          .equalsIgnoreCase(caseItem.getCaseType())) {
        caseItemRepository.updateCompareStatus(caseItem.getId(),
            CompareProcessStatusType.PASS.getValue());
        caseItem.setCompareStatus(CompareProcessStatusType.PASS.getValue());
        return comparisonOutputWriter.writeQmqCompareResult(caseItem);
      }

      CompareProcessStatusType compareStatus = CompareProcessStatusType.PASS;
      for (ReplayCompareResult replayCompareResult : replayCompareResults) {
        if (replayCompareResult.getDiffResultCode() == DiffResultCode.COMPARED_WITH_DIFFERENCE) {
          compareStatus = CompareProcessStatusType.HAS_DIFF;
          break;
        } else if (replayCompareResult.getDiffResultCode()
            == DiffResultCode.COMPARED_INTERNAL_EXCEPTION) {
          compareStatus = CompareProcessStatusType.ERROR;
          break;
        }
      }
      caseItemRepository.updateCompareStatus(caseItem.getId(), compareStatus.getValue());
      caseItem.setCompareStatus(compareStatus.getValue());
      return comparisonOutputWriter.write(replayCompareResults);
    } catch (Throwable throwable) {
      caseItemRepository.updateCompareStatus(caseItem.getId(),
          CompareProcessStatusType.ERROR.getValue());
      caseItem.setCompareStatus(CompareProcessStatusType.ERROR.getValue());
      comparisonOutputWriter.writeIncomparable(caseItem, throwable.getMessage());
      LOGGER.error("compare case result error:{} ,case item: {}", throwable.getMessage(), caseItem,
          throwable);
      MDCTracer.clear();
      // don't send again
      return true;
    } finally {
      progressTracer.finishOne(caseItem);
      compareWatch.stop();
      metricService.recordTimeEvent(LogType.COMPARE.getValue(), planId,
          caseItem.getParent().getAppId(), null,
          compareWatch.getTotalTimeMillis());
      long caseExecutionEndMills = System.currentTimeMillis();
      metricService.recordTimeEvent(LogType.CASE_EXECUTION_TIME.getValue(), planId,
          caseItem.getParent().getAppId(), null,
          caseExecutionEndMills - caseItem.getExecutionStartMillis());
      MDCTracer.clear();
    }
  }

  public List<ReplayCompareResult> doContentCompare(ReplayActionCaseItem caseItem,
      List<CategoryComparisonHolder> waitCompareMap) {
    String planId = caseItem.getParent().getPlanId();

    ComparisonInterfaceConfig operationConfig = compareConfigService.loadInterfaceConfig(
        caseItem.getParent());
    ComparisonGlobalConfig globalConfig = compareConfigService.loadGlobalConfig(planId);
    List<String> ignoreCategoryList = operationConfig.getIgnoreCategoryTypes();

    List<ReplayCompareResult> replayCompareResults = new ArrayList<>();
    for (CategoryComparisonHolder bindHolder : waitCompareMap) {
      if (operationConfig.checkIgnoreMockMessageType(bindHolder.getCategoryName(),
          ignoreCategoryList)) {
        continue;
      }
      replayCompareResults.addAll(
          compareReplayResult(bindHolder, caseItem, operationConfig, globalConfig));
    }
    return replayCompareResults;
  }

  /**
   * compare recording and replay data. 1. record and replay data through compareKey.
   */
  private List<ReplayCompareResult> compareReplayResult(CategoryComparisonHolder bindHolder,
      ReplayActionCaseItem caseItem, ComparisonInterfaceConfig operationConfig,
      ComparisonGlobalConfig comparisonGlobalConfig) {
    Pair<ComparisonGlobalConfig, ComparisonInterfaceConfig> configPair =
        Pair.of(comparisonGlobalConfig, operationConfig);
    List<ReplayCompareResult> compareResults = new ArrayList<>();
    List<CompareItem> recordResults = bindHolder.getRecord();
    List<CompareItem> replayResults = bindHolder.getReplayResult();
    boolean sourceEmpty = CollectionUtils.isEmpty(recordResults);
    boolean targetEmpty = CollectionUtils.isEmpty(replayResults);
    if (sourceEmpty && targetEmpty) {
      return Collections.emptyList();
    }
    final String category = bindHolder.getCategoryName();
    if (sourceEmpty || targetEmpty) {
      compareResults.addAll(
          calculateMissResult(category, configPair, recordResults, caseItem, false));
      compareResults.addAll(
          calculateMissResult(category, configPair, replayResults, caseItem, true));
      return compareResults;
    }

    Map<String, List<CompareItem>> recordMap =
        recordResults.stream().filter(data -> StringUtils.isNotEmpty(data.getCompareKey()))
            .collect(Collectors.groupingBy(CompareItem::getCompareKey));

    Set<String> usedRecordKeys = new HashSet<>();
    for (CompareItem resultCompareItem : replayResults) {
      // config for operation if its entrypoint, dependency config otherwise
      String compareKey = resultCompareItem.getCompareKey();

      if (resultCompareItem.isEntryPointCategory()) {
        compareResults.add(
            compareRecordAndResult(operationConfig, caseItem, category, resultCompareItem,
                recordResults.get(0)));
        return compareResults;
      }

      if (StringUtils.isEmpty(compareKey)) {
        compareResults.addAll(calculateMissResult(category, configPair,
            Collections.singletonList(resultCompareItem), caseItem, true));
        continue;
      }

      if (recordMap.containsKey(compareKey)) {
        List<CompareItem> recordCompareItems = recordMap.get(compareKey);
        if (CollectionUtils.isEmpty(recordCompareItems)) {
          continue;
        }
        ReplayComparisonConfig compareConfig = configHandler.pickConfig(comparisonGlobalConfig,
            operationConfig,
            recordCompareItems.get(0), category);
        compareResults.add(
            compareRecordAndResult(compareConfig, caseItem, category, resultCompareItem,
                recordCompareItems.get(0)));
        usedRecordKeys.add(compareKey);
      } else {
        compareResults.addAll(calculateMissResult(category, configPair,
            Collections.singletonList(resultCompareItem), caseItem, true));
      }
    }

    recordMap.keySet().stream().filter(key -> !usedRecordKeys.contains(key)) // unused keys
        .forEach(key -> compareResults
            .addAll(
                calculateMissResult(category, configPair, recordMap.get(key), caseItem, false)));
    return compareResults;
  }

  private ReplayCompareResult compareRecordAndResult(ReplayComparisonConfig compareConfig,
      ReplayActionCaseItem caseItem, String category, CompareItem target, CompareItem source) {

    StopWatch stopWatch = new StopWatch();
    stopWatch.start(LogType.COMPARE_SDK.getValue());
    CompareResult comparedResult = compareProcess(category, source.getCompareContent(),
        target.getCompareContent(),
        compareConfig, caseItem.getCompareMode().getValue());
    stopWatch.stop();
    metricService.recordTimeEvent(LogType.COMPARE_SDK.getValue(), caseItem.getParent().getPlanId(),
        caseItem.getParent().getAppId(), source.getCompareContent(),
        stopWatch.getTotalTimeMillis());
    ReplayCompareResult resultNew = ReplayCompareResult.createFrom(caseItem);
    mergeResult(source.getCompareOperation(), category, resultNew, comparedResult,
        source.getCreateTime(),
        target.getCreateTime(), target.getCompareKey());
    return resultNew;
  }

  /**
   * Call if one of the compare item is missed
   */
  private List<ReplayCompareResult> calculateMissResult(String category,
      Pair<ComparisonGlobalConfig, ComparisonInterfaceConfig> configPair,
      List<CompareItem> compareItems,
      ReplayActionCaseItem caseItem, boolean missRecord) {
    if (CollectionUtils.isEmpty(compareItems)) {
      return Collections.emptyList();
    }
    List<ReplayCompareResult> resultList = new ArrayList<>();

    String operation;
    for (CompareItem item : compareItems) {
      ReplayComparisonConfig itemConfig =
          configHandler.pickConfig(configPair.getLeft(), configPair.getRight(), item, category);
      operation = item.getCompareOperation();
      CompareResult comparedResult;
      if (missRecord) {
        comparedResult = compareProcess(category, null, item.getCompareContent(), itemConfig,
            caseItem.getCompareMode().getValue());
      } else {
        comparedResult = compareProcess(category, item.getCompareContent(), null, itemConfig,
            caseItem.getCompareMode().getValue());
      }

      ReplayCompareResult resultItem = ReplayCompareResult.createFrom(caseItem);

      if (missRecord) {
        mergeResult(operation, category, resultItem, comparedResult, MAX_TIME, item.getCreateTime(),
            item.getCompareKey());
      } else {
        mergeResult(operation, category, resultItem, comparedResult, item.getCreateTime(), MAX_TIME,
            item.getCompareKey());
      }
      resultItem.setServiceName(item.getCompareService());
      resultList.add(resultItem);
    }
    return resultList;
  }

  private CompareResult compareProcess(String category, String record, String result,
      ReplayComparisonConfig compareConfig, int compareMode) {
    CompareOptions options = configHandler.buildSkdOption(category, compareConfig);
    try {
      // to-do: 64base extract record and result
      String decodedRecord = EncodingUtils.tryBase64Decode(record);
      String decodedResult = EncodingUtils.tryBase64Decode(result);
      if (compareMode == CompareModeType.FULL.getValue()) {
        return COMPARE_INSTANCE.compare(decodedRecord, decodedResult, options);
      }
      return COMPARE_INSTANCE.quickCompare(decodedRecord, decodedResult, options);

    } catch (Throwable e) {
      LOGGER.error("run compare sdk process error:{} ,source: {} ,target:{}", e.getMessage(),
          record, result);
      return CompareSDK.fromException(record, result, e.getMessage());
    }
  }

  private void mergeResult(String operation, String category, ReplayCompareResult diffResult,
      CompareResult sdkResult,
      long recordTime, long replayTime, String instanceId) {
    diffResult.setOperationName(operation);
    diffResult.setCategoryName(category);
    diffResult.setBaseMsg(sdkResult.getProcessedBaseMsg());
    diffResult.setTestMsg(sdkResult.getProcessedTestMsg());
    diffResult.setLogs(sdkResult.getLogs());
    diffResult.setMsgInfo(sdkResult.getMsgInfo());
    diffResult.setDiffResultCode(sdkResult.getCode());
    diffResult.setRecordTime(recordTime);
    diffResult.setReplayTime(replayTime);
    diffResult.setInstanceId(instanceId);
  }
}