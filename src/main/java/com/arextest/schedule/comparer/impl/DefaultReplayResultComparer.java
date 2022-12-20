package com.arextest.schedule.comparer.impl;


import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.CompareResult;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.arextest.schedule.progress.ProgressTracer;

import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @Override
    public boolean compare(ReplayActionCaseItem caseItem, boolean useReplayId) {
        try {
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
            return comparisonOutputWriter.write(replayCompareResults);
        } catch (Throwable throwable) {
            caseItemRepository.updateCompareStatus(caseItem.getId(), CompareProcessStatusType.ERROR.getValue());
            comparisonOutputWriter.writeIncomparable(caseItem, throwable.getMessage());
            LOGGER.error("compare case result error:{} ,case item: {}", throwable.getMessage(), caseItem, throwable);
            // don't send again
            return true;
        } finally {
            progressTracer.finishOne(caseItem);
        }
    }

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
            addMissReplayResult(category, compareConfig, recordList, caseItem, compareResultNewList);
            addMissRecordResult(category, compareConfig, replayResultList, caseItem, compareResultNewList);
            return;
        }
        Map<String, List<CompareItem>> recordMap =
                recordList.stream().collect(Collectors.groupingBy(CompareItem::getCompareOperation));
        Map<String, List<CompareItem>> resultMap =
                replayResultList.stream().collect(Collectors.groupingBy(CompareItem::getCompareOperation));
        // todo: use set instead of list
        final List<String> ignoreKeyList = compareConfig.getIgnoreKeyList();
        for (Map.Entry<String, List<CompareItem>> stringListEntry : recordMap.entrySet()) {
            String key = stringListEntry.getKey();
            boolean ignore = ignoreKeyList.contains(key);
            if (ignore) {
                continue;
            }
            List<CompareItem> recordContentList = stringListEntry.getValue();
            if (resultMap.containsKey(key)) {
                List<String> recordContentGroupList =
                        recordContentList.stream().map(CompareItem::getCompareContent).collect(Collectors.toList());
                List<String> resultContentGroupList =
                        resultMap.get(key).stream().map(CompareItem::getCompareContent).collect(Collectors.toList());
                CompareSDK.arraySort(recordContentGroupList, resultContentGroupList);
                for (int i = 0; i < recordContentGroupList.size(); i++) {
                    String source = recordContentGroupList.get(i);
                    String target = resultContentGroupList.get(i);
                    CompareResult comparedResult = compareProcess(source, target, compareConfig);
                    ReplayCompareResult resultNew = ReplayCompareResult.createFrom(caseItem);
                    mergeResult(key, category, resultNew, comparedResult);
                    compareResultNewList.add(resultNew);
                }
                continue;
            }
            addMissReplayResult(category, compareConfig, recordContentList, caseItem, compareResultNewList);
        }
    }

    private CompareResult compareProcess(String record, String result, ReplayComparisonConfig compareConfig) {
        CompareOptions options = buildCompareRequest(compareConfig);
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
        return sourceRemoteLoader.getReplayResult(recordId, targetResultId);
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
                             CompareResult sdkResult) {
        diffResult.setOperationName(operation);
        diffResult.setCategoryName(category);
        diffResult.setBaseMsg(sdkResult.getProcessedBaseMsg());
        diffResult.setTestMsg(sdkResult.getProcessedTestMsg());
        diffResult.setLogs(sdkResult.getLogs());
        diffResult.setDiffResultCode(sdkResult.getCode());
    }

    private void addMissReplayResult(String category, ReplayComparisonConfig compareConfig,
                                     List<CompareItem> recordList,
                                     ReplayActionCaseItem caseItem, List<ReplayCompareResult> resultList) {
        if (CollectionUtils.isEmpty(recordList)) {
            return;
        }
        String operation;
        final List<String> ignoreKeyList = compareConfig.getIgnoreKeyList();
        for (CompareItem item : recordList) {
            operation = item.getCompareOperation();
            if (ignoreKeyList.contains(operation)) {
                continue;
            }
            CompareResult comparedResult = compareProcess(item.getCompareContent(), null, compareConfig);
            ReplayCompareResult resultItem = ReplayCompareResult.createFrom(caseItem);
            mergeResult(operation, category, resultItem, comparedResult);
            resultItem.setServiceName(item.getCompareService());
            resultList.add(resultItem);
        }
    }

    private void addMissRecordResult(String category, ReplayComparisonConfig compareConfig,
                                     List<CompareItem> replayList,
                                     ReplayActionCaseItem caseItem, List<ReplayCompareResult> resultList) {
        if (CollectionUtils.isEmpty(replayList)) {
            return;
        }
        List<String> ignoreKeyList = compareConfig.getIgnoreKeyList();
        for (CompareItem item : replayList) {
            String operation = item.getCompareOperation();
            if (ignoreKeyList.contains(operation)) {
                continue;
            }
            CompareResult comparedResult = compareProcess(null, item.getCompareContent(), compareConfig);
            ReplayCompareResult resultItem = ReplayCompareResult.createFrom(caseItem);
            mergeResult(operation, category, resultItem, comparedResult);
            resultItem.setServiceName(item.getCompareService());
            resultList.add(resultItem);
        }
    }

    private ReplayComparisonConfig getCompareConfig(ReplayActionItem actionItem) {
        return compareConfigService.loadConfig(actionItem);
    }

    private CompareOptions buildCompareRequest(ReplayComparisonConfig compareConfig) {
        CompareOptions options = new CompareOptions();
        options.putExclusions(compareConfig.getExclusionList());
        options.putInclusions(compareConfig.getInclusionList());
        options.putListSortConfig(compareConfig.getListSortMap());
        options.putReferenceConfig(compareConfig.getReferenceMap());
        options.putDecompressConfig(compareConfig.getDecompressConfig());
        return options;
    }

}