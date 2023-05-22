package com.arextest.schedule.comparer.impl;


import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.CompareResult;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.comparer.*;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.*;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.service.MetricService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class DefaultReplayResultComparer implements ReplayResultComparer {
    private final CompareConfigService compareConfigService;
    private final PrepareCompareSourceRemoteLoader sourceRemoteLoader;
    private final ProgressTracer progressTracer;
    private final ComparisonWriter comparisonOutputWriter;
    private final ReplayActionCaseItemRepository caseItemRepository;
    private static final int INDEX_NOT_FOUND = -1;
    private static final CompareSDK COMPARE_INSTANCE = new CompareSDK();
    private final MetricService metricService;
    private static List<String> ignoreInDataBaseMocker = Collections.singletonList("body");

    private static long MAX_TIME = Long.MAX_VALUE;

    static {
        COMPARE_INSTANCE.getGlobalOptions().putNameToLower(true).putNullEqualsEmpty(true).putIgnoredTimePrecision(1000);
    }

    @Override
    public boolean compare(ReplayActionCaseItem caseItem, boolean useReplayId) {
        StopWatch compareWatch = new StopWatch();
        compareWatch.start(LogType.COMPARE.getValue());
        String planId = caseItem.getParent().getPlanId();
        try {
            MDCTracer.addPlanId(planId);
            MDCTracer.addPlanItemId(caseItem.getPlanItemId());
            ReplayComparisonConfig compareConfig = getCompareConfig(caseItem.getParent());
            List<ReplayCompareResult> replayCompareResults = new ArrayList<>();
            List<CategoryComparisonHolder> waitCompareMap = buildWaitCompareList(caseItem, useReplayId);
            if (CollectionUtils.isEmpty(waitCompareMap)) {
                caseItemRepository.updateCompareStatus(caseItem.getId(), CompareProcessStatusType.ERROR.getValue());
                comparisonOutputWriter.writeIncomparable(caseItem, CaseSendStatusType.REPLAY_RESULT_NOT_FOUND.name());
                return true;
            }
            for (CategoryComparisonHolder bindHolder : waitCompareMap) {
                if (compareConfig.checkIgnoreMockMessageType(bindHolder.getCategoryName())) {
                    continue;
                }
                compareReplayResult(bindHolder, compareConfig, caseItem, replayCompareResults);
            }
            if (CollectionUtils.isEmpty(replayCompareResults) && !useReplayId) {
                return comparisonOutputWriter.writeQmqCompareResult(caseItem);
            }
            return comparisonOutputWriter.write(replayCompareResults);
        } catch (Throwable throwable) {
            caseItemRepository.updateCompareStatus(caseItem.getId(), CompareProcessStatusType.ERROR.getValue());
            comparisonOutputWriter.writeIncomparable(caseItem, throwable.getMessage());
            LOGGER.error("compare case result error:{} ,case item: {}", throwable.getMessage(), caseItem, throwable);
            MDCTracer.clear();
            // don't send again
            return true;
        } finally {
            caseItemRepository.updateCompareStatus(caseItem.getId(), CompareProcessStatusType.PASS.getValue());
            progressTracer.finishOne(caseItem);
            compareWatch.stop();
            metricService.recordTimeEvent(LogType.COMPARE.getValue(), planId, caseItem.getParent().getAppId(), null,
                    compareWatch.getTotalTimeMillis());
            long caseExecutionEndMills = System.currentTimeMillis();
            metricService.recordTimeEvent(LogType.CASE_EXECUTION_TIME.getValue(), planId, caseItem.getParent().getAppId(), null,
                    caseExecutionEndMills - caseItem.getExecutionStartMillis());
            MDCTracer.clear();
        }
    }

    /**
     * compare recording and replay data.
     * 1. record and replay data through compareKey.
     */
    private void compareReplayResult(CategoryComparisonHolder bindHolder,
                                     ReplayComparisonConfig compareConfig, ReplayActionCaseItem caseItem,
                                     List<ReplayCompareResult> compareResultNewList) {
        List<CompareItem> recordList = bindHolder.getRecord();
        List<CompareItem> replayResultList = bindHolder.getReplayResult();
        boolean sourceEmpty = CollectionUtils.isEmpty(recordList);
        boolean targetEmpty = CollectionUtils.isEmpty(replayResultList);
        if (sourceEmpty && targetEmpty) {
            return;
        }
        final String category = bindHolder.getCategoryName();
        if (sourceEmpty || targetEmpty) {
            addMissResult(category, compareConfig, recordList, caseItem, compareResultNewList, false);
            addMissResult(category, compareConfig, replayResultList, caseItem, compareResultNewList, true);
            return;
        }

        final Set<String> ignoreKeyList = compareConfig.getIgnoreKeyList();

        Map<String, List<CompareItem>> recordMap =
                recordList.stream().filter(data -> StringUtils.isNotEmpty(data.getCompareKey()))
                        .collect(Collectors.groupingBy(CompareItem::getCompareKey));
        Set<String> usedRecordKeys = new HashSet<>();
        for (CompareItem resultCompareItem: replayResultList) {
            String compareKey = resultCompareItem.getCompareKey();
            boolean ignore = ignoreKeyList.contains(resultCompareItem.getCompareOperation());
            if (ignore) {
                continue;
            }

            if (StringUtils.isEmpty(compareKey)) {
                compareRecordAndResult(compareConfig, caseItem, compareResultNewList, category, resultCompareItem, recordList.get(0));
                return;
            }

            if (recordMap.containsKey(compareKey)) {
                List<CompareItem> recordCompareItems = recordMap.get(compareKey);
                if (CollectionUtils.isEmpty(recordCompareItems)) {
                    continue;
                }
                compareRecordAndResult(compareConfig, caseItem, compareResultNewList, category, resultCompareItem, recordCompareItems.get(0));
                usedRecordKeys.add(compareKey);
            } else {
                addMissResult(category, compareConfig, Collections.singletonList(resultCompareItem), caseItem, compareResultNewList, true);
            }
        }

        Set<String> recordKeys = recordMap.keySet();
        for (String usedCompareKey : usedRecordKeys) {
            if (recordKeys.contains(usedCompareKey)) {
                recordKeys.remove(usedCompareKey);
            }
        }

        if (CollectionUtils.isEmpty(recordKeys)) {
            return;
        }

        for (String recordKey : recordKeys) {
            addMissResult(category, compareConfig, recordMap.get(recordKey), caseItem, compareResultNewList, false);
        }
    }

    private void compareRecordAndResult(ReplayComparisonConfig compareConfig, ReplayActionCaseItem caseItem,
                                        List<ReplayCompareResult> compareResultNewList, String category,
                                        CompareItem target, CompareItem source) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start(LogType.COMPARE_SDK.getValue());
        CompareResult comparedResult = compareProcess(category, source.getCompareContent(), target.getCompareContent(), compareConfig);
        stopWatch.stop();
        metricService.recordTimeEvent(LogType.COMPARE_SDK.getValue(), caseItem.getParent().getPlanId(),
                caseItem.getParent().getAppId(), source.getCompareContent(), stopWatch.getTotalTimeMillis());
        ReplayCompareResult resultNew = ReplayCompareResult.createFrom(caseItem);
        mergeResult(source.getCompareOperation(), category, resultNew, comparedResult, source.getCompareCreateTime(), target.getCompareCreateTime(),
                target.getCompareKey());
        compareResultNewList.add(resultNew);
    }

    private CompareResult compareProcess(String category, String record, String result,
                                         ReplayComparisonConfig compareConfig) {
        CompareOptions options = buildCompareRequest(category, compareConfig);
        try {
            return COMPARE_INSTANCE.compare(record, result, options);
        } catch (Throwable e) {
            LOGGER.error("run compare sdk process error:{} ,source: {} ,target:{}", e.getMessage(), record, result);
            return CompareSDK.fromException(record, result, e.getMessage());
        }
    }

    private List<CategoryComparisonHolder> buildWaitCompareList(ReplayActionCaseItem caseItem, boolean useReplayId) {
        String targetResultId = null;
        String sourceResultId = null;
        if (useReplayId) {
            targetResultId = caseItem.getTargetResultId();
            sourceResultId = caseItem.getSourceResultId();
        }
        final String recordId = caseItem.getRecordId();

        if (StringUtils.isNotBlank(sourceResultId)) {

            List<CategoryComparisonHolder> sourceResponse = sourceRemoteLoader.getReplayResult(recordId,
                    sourceResultId);
            List<CategoryComparisonHolder> targetResponse = sourceRemoteLoader.getReplayResult(recordId,
                    targetResultId);
            if (CollectionUtils.isEmpty(sourceResponse) || CollectionUtils.isEmpty(targetResponse)) {
                LOGGER.warn("replay recordId:{} invalid response,source replayId:{} size:{},target replayId:{} size:{}",
                        recordId, sourceResultId, sourceResponse.size(), targetResultId, targetResponse.size());
                return Collections.emptyList();
            }
            return buildWaitCompareList(sourceResponse, targetResponse);
        }
        List<CategoryComparisonHolder> replayResult = sourceRemoteLoader.getReplayResult(recordId, targetResultId);
        //todo record the QMessage replay log,  which will be optimized for removal later.
        metricService.recordTraceIdEvent(caseItem, replayResult);
        return replayResult;
    }

    private List<CategoryComparisonHolder> buildWaitCompareList(List<CategoryComparisonHolder> sourceResult,
                                                                List<CategoryComparisonHolder> targetResultList) {
        for (CategoryComparisonHolder sourceResultHolder : sourceResult) {
            int targetIndex = findResultByCategory(targetResultList, sourceResultHolder.getCategoryName());
            sourceResultHolder.setRecord(sourceResultHolder.getReplayResult());
            if (targetIndex == INDEX_NOT_FOUND) {
                continue;
            }
            CategoryComparisonHolder targetResult = targetResultList.get(targetIndex);
            sourceResultHolder.setReplayResult(targetResult.getReplayResult());
            targetResultList.remove(targetIndex);
        }
        if (CollectionUtils.isNotEmpty(targetResultList)) {
            for (CategoryComparisonHolder resultHolder : targetResultList) {
                resultHolder.setRecord(Collections.emptyList());
                sourceResult.add(resultHolder);
            }
        }
        return sourceResult;
    }

    private int findResultByCategory(List<CategoryComparisonHolder> source, String category) {
        for (int i = 0; i < source.size(); i++) {
            CategoryComparisonHolder resultHolder = source.get(i);
            if (StringUtils.equals(resultHolder.getCategoryName(), category)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    private void mergeResult(String operation, String category, ReplayCompareResult diffResult,
                             CompareResult sdkResult, long recordTime, long replayTime, String instanceId) {
        diffResult.setOperationName(operation);
        diffResult.setCategoryName(category);
        diffResult.setBaseMsg(sdkResult.getProcessedBaseMsg());
        diffResult.setTestMsg(sdkResult.getProcessedTestMsg());
        diffResult.setLogs(sdkResult.getLogs());
        diffResult.setDiffResultCode(sdkResult.getCode());
        diffResult.setRecordTime(recordTime);
        diffResult.setReplayTime(replayTime);
        diffResult.setInstanceId(instanceId);
    }

    private void addMissResult(String category, ReplayComparisonConfig compareConfig,
                                     List<CompareItem> compareItems,
                                     ReplayActionCaseItem caseItem, List<ReplayCompareResult> resultList, boolean missRecord) {
        if (CollectionUtils.isEmpty(compareItems)) {
            return;
        }
        String operation;
        final Set<String> ignoreKeyList = compareConfig.getIgnoreKeyList();
        for (CompareItem item : compareItems) {
            operation = item.getCompareOperation();
            if (ignoreKeyList.contains(operation)) {
                continue;
            }
            CompareResult comparedResult = null;
            if (missRecord) {
                comparedResult = compareProcess(category, null, item.getCompareContent(), compareConfig);
            } else {
                comparedResult = compareProcess(category, item.getCompareContent(), null, compareConfig);
            }

            ReplayCompareResult resultItem = ReplayCompareResult.createFrom(caseItem);

            if (missRecord) {
                mergeResult(operation, category, resultItem, comparedResult, MAX_TIME, item.getCompareCreateTime(), item.getCompareKey());
            } else {
                mergeResult(operation, category, resultItem, comparedResult, item.getCompareCreateTime(), MAX_TIME, item.getCompareKey());
            }
            resultItem.setServiceName(item.getCompareService());
            resultList.add(resultItem);
        }
    }

    private ReplayComparisonConfig getCompareConfig(ReplayActionItem actionItem) {
        return compareConfigService.loadConfig(actionItem);
    }

    private CompareOptions buildCompareRequest(String category, ReplayComparisonConfig compareConfig) {
        CompareOptions options = new CompareOptions();
        options.putCategoryType(category);
        // todo: the switch of "sqlBodyParse" and "onlyCompareCoincidentColumn"
        //  need get from ReplayComparisonConfig
        options.putSelectIgnoreCompare(true);
        options.putOnlyCompareCoincidentColumn(true);
        options.putExclusions(compareConfig.getExclusionList());
        options.putInclusions(compareConfig.getInclusionList());
        options.putListSortConfig(compareConfig.getListSortMap());
        options.putReferenceConfig(compareConfig.getReferenceMap());
        options.putDecompressConfig(compareConfig.getDecompressConfig());

        if (Objects.equals(category, MockCategoryType.DATABASE.getName())) {
            options.putExclusions(ignoreInDataBaseMocker);
        }
        return options;
    }

}