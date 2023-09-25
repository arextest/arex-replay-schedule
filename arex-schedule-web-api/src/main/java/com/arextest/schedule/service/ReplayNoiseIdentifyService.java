package com.arextest.schedule.service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;

import com.arextest.diff.model.enumeration.DiffResultCode;
import com.arextest.diff.model.enumeration.UnmatchedType;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.diff.model.log.NodeEntity;
import com.arextest.diff.model.log.UnmatchedPairEntity;
import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.ComparisonWriter;
import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.comparer.impl.DefaultReplayResultComparer;
import com.arextest.schedule.comparer.impl.PrepareCompareSourceRemoteLoader;
import com.arextest.schedule.dao.mongodb.ReplayActionCaseItemRepository;
import com.arextest.schedule.model.*;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseDto;
import com.arextest.schedule.progress.ProgressTracer;
import com.arextest.schedule.sender.ReplaySender;
import com.arextest.schedule.sender.ReplaySenderFactory;
import com.arextest.schedule.utils.ListUtils;
import com.arextest.schedule.utils.MapUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
public class ReplayNoiseIdentifyService {
    // private static List<PlanItemIdAndContextName> planItemIdAndContextNames = new ArrayList<>();
    private static final int CASE_COUNT_FOR_NOISE_IDENTIFY = 2;

    ReplaySenderFactory replaySenderFactory;

    ExecutorService executor = Executors.newFixedThreadPool(4);

    ExecutorService executor1 = Executors.newFixedThreadPool(4);

    private DefaultReplayResultComparer defaultResultComparer;

    private PrepareCompareSourceRemoteLoader sourceRemoteLoader;

    public ReplayNoiseIdentifyService(CompareConfigService compareConfigService,
        PrepareCompareSourceRemoteLoader sourceRemoteLoader, ProgressTracer progressTracer,
        ComparisonWriter comparisonOutputWriter, ReplayActionCaseItemRepository caseItemRepository,
        MetricService metricService, CustomComparisonConfigurationHandler customComparisonConfigurationHandler) {
        this.sourceRemoteLoader = sourceRemoteLoader;
        this.defaultResultComparer =
            new DefaultReplayResultComparer(compareConfigService, sourceRemoteLoader, progressTracer,
                comparisonOutputWriter, caseItemRepository, metricService, customComparisonConfigurationHandler);
    }

    public void noiseIdentify(Map<ReplayActionItem, List<ReplayActionCaseItem>> actionsOfBatch,
        PlanExecutionContext<?> executionContext) {

        List<MutablePair<ReplayActionItem, List<ReplayActionCaseItem>>> casesForNoise = new ArrayList<>();
        actionsOfBatch.forEach((action, cases) -> {

            int caseSize = cases.size();
            int tempCount = 0;
            List<ReplayActionCaseItem> tempCases = new ArrayList<>();

            ReplayActionItem targetAction = new ReplayActionItem();
            BeanUtils.copyProperties(action, targetAction);
            targetAction.setSourceInstance(targetAction.getTargetInstance());

            while (tempCount < CASE_COUNT_FOR_NOISE_IDENTIFY || tempCount < caseSize) {
                ReplayActionCaseItem sourceCase = cases.get(tempCount);
                ReplayActionCaseItem targetCase = new ReplayActionCaseItem();
                BeanUtils.copyProperties(sourceCase, targetCase);
                targetCase.setParent(targetAction);
                targetCase.setCompareMode(CompareModeType.FULL);
                tempCases.add(targetCase);
                tempCount++;
            }
            casesForNoise.add(new MutablePair<>(action, cases));
        });

        List<ReplayActionCaseItem> replayActionCaseItems = casesForNoise.stream().map(MutablePair::getRight)
            .flatMap(List::stream).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        List<Runnable> tasks = this.createTasks(replayActionCaseItems);
        CompletableFuture[] array = tasks.stream().map(executor::submit).toArray(CompletableFuture[]::new);

        // 计算完成，落库分析结果，并更新标志位
        CompletableFuture.allOf(array).join();

        for (MutablePair<ReplayActionItem, List<ReplayActionCaseItem>> itemPair : casesForNoise) {
            PlanItemIdAndContextName planItemIdAndContextName = new PlanItemIdAndContextName(itemPair.getLeft().getId(),
                executionContext.getContextName(), itemPair.getRight());
            this.analysisNoise(planItemIdAndContextName);
        }

    }

    public void analysisNoise(PlanItemIdAndContextName planItemIdAndContextName) {
        String planItemId = planItemIdAndContextName.getPlanItemId();
        String contextName = planItemIdAndContextName.getContextName();
        List<ReplayActionCaseItem> cases = planItemIdAndContextName.getCases();

        CompletableFuture.supplyAsync(() -> {

            // 比较
            List<ReplayCompareResult> compareResults = new ArrayList<>();
            for (ReplayActionCaseItem caseItem : cases) {
                if (StringUtils.isEmpty(caseItem.getSendErrorMessage())) {
                    continue;
                }
                List<CategoryComparisonHolder> categoryComparisonHolders =
                    sourceRemoteLoader.buildWaitCompareList(caseItem, true);
                List<ReplayCompareResult> replayCompareResults =
                    defaultResultComparer.doContentCompare(caseItem, categoryComparisonHolders);
                compareResults.addAll(replayCompareResults);
            }

            List<ReplayNoiseDto> result = new ArrayList<>();

            // 分析
            if (CollectionUtils.isEmpty(compareResults)) {
                return null;
            }

            Map<Pair<String, String>, List<ReplayCompareResult>> compareResultOfCategory = compareResults.stream()
                .filter(compareResult -> compareResult.getDiffResultCode() == DiffResultCode.COMPARED_WITH_DIFFERENCE)
                .collect(
                    Collectors.groupingBy(item -> new MutablePair(item.getCategoryName(), item.getOperationName())));
            for (Map.Entry<Pair<String, String>, List<ReplayCompareResult>> entry : compareResultOfCategory
                .entrySet()) {

                Pair<String, String> categoryAndOperation = entry.getKey();
                List<ReplayCompareResult> replayCompareResultList = entry.getValue();
                InferredReplayNoiseDto inferredReplayNoiseDto =
                    this.analysisNoiseFromCompareResult(replayCompareResultList);

                if (inferredReplayNoiseDto == null) {
                    continue;
                }

                ReplayNoiseDto replayNoiseDto = new ReplayNoiseDto();
                replayNoiseDto.setPlanId("planId");
                replayNoiseDto.setPlanItemId(planItemId);
                replayNoiseDto.setCategoryName(categoryAndOperation.getLeft());
                replayNoiseDto.setOperationName(categoryAndOperation.getRight());

                Map<List<NodeEntity>, NoiseNodeAddress> mayIgnoreNodes = inferredReplayNoiseDto.getMayIgnoreNodes();
                if (MapUtils.isNotEmpty(mayIgnoreNodes)) {
                    Map<String, ReplayNoiseDto.ReplayNoiseItem> mayIgnoreItems = new HashMap<>();
                    for (Map.Entry<List<NodeEntity>, NoiseNodeAddress> nodeEntry : mayIgnoreNodes.entrySet()) {
                        ReplayNoiseDto.ReplayNoiseItem replayNoiseItem = new ReplayNoiseDto.ReplayNoiseItem();
                        List<NodeEntity> nodePath = nodeEntry.getKey();
                        NoiseNodeAddress noiseNodeAddress = nodeEntry.getValue();
                        replayNoiseItem.setNodePath(nodePath);
                        replayNoiseItem.setCompareResult(noiseNodeAddress.getCompareResult());
                        replayNoiseItem.setLogIndexes(noiseNodeAddress.getLogIndexes());
                        replayNoiseItem.setSubPaths(noiseNodeAddress.getSubPaths());
                        replayNoiseItem.setPathCount(noiseNodeAddress.getPathCount());
                        replayNoiseItem.setCaseCount(noiseNodeAddress.getCaseCount());
                        mayIgnoreItems.put(ListUtils.getFuzzyPathStr(nodePath), replayNoiseItem);
                    }
                    replayNoiseDto.setMayIgnoreItems(mayIgnoreItems);
                }

                Map<List<NodeEntity>, NoiseNodeAddress> mayDisorderArray = inferredReplayNoiseDto.getMayDisorderArray();
                if (MapUtils.isNotEmpty(mayDisorderArray)) {
                    Map<String, ReplayNoiseDto.ReplayNoiseItem> mayDisorderItems = new HashMap<>();
                    for (Map.Entry<List<NodeEntity>, NoiseNodeAddress> nodeEntry : mayDisorderArray.entrySet()) {
                        ReplayNoiseDto.ReplayNoiseItem replayNoiseItem = new ReplayNoiseDto.ReplayNoiseItem();
                        List<NodeEntity> nodePath = nodeEntry.getKey();
                        NoiseNodeAddress noiseNodeAddress = nodeEntry.getValue();
                        replayNoiseItem.setNodePath(nodePath);
                        replayNoiseItem.setCompareResult(noiseNodeAddress.getCompareResult());
                        replayNoiseItem.setLogIndexes(noiseNodeAddress.getLogIndexes());
                        replayNoiseItem.setSubPaths(noiseNodeAddress.getSubPaths());
                        replayNoiseItem.setPathCount(noiseNodeAddress.getPathCount());
                        replayNoiseItem.setCaseCount(noiseNodeAddress.getCaseCount());
                        mayDisorderItems.put(ListUtils.getFuzzyPathStr(nodePath), replayNoiseItem);
                    }
                    replayNoiseDto.setMayDisorderItems(mayDisorderItems);
                }
                result.add(replayNoiseDto);
            }

            // 落库

            return null;
        }, executor1);
    }

    private List<Runnable> createTasks(List<ReplayActionCaseItem> cases) {
        List<Runnable> tasks = new ArrayList<>();
        for (ReplayActionCaseItem caseItem : cases) {
            ReplaySender replaySender = replaySenderFactory.findReplaySender(caseItem.getCaseType());
            AsyncNoiseCaseSendTaskRunnable taskRunnable = new AsyncNoiseCaseSendTaskRunnable(replaySender, caseItem);
            tasks.add(taskRunnable);
        }
        return tasks;
    }

    /**
     * 单个mocker类型的分析结果
     * 
     * @param compareResults
     * @return
     */
    private InferredReplayNoiseDto analysisNoiseFromCompareResult(List<ReplayCompareResult> compareResults) {
        if (CollectionUtils.isEmpty(compareResults)) {
            return null;
        }
        InferredReplayNoiseDto inferredReplayNoiseDto = new InferredReplayNoiseDto();
        // 叶子节点
        Map<List<NodeEntity>, NoiseNodeAddress> mayIgnoreNodes = new HashMap<>();
        // 数组节点
        Map<List<NodeEntity>, NoiseNodeAddress> mayDisorderArray = new HashMap<>();

        for (ReplayCompareResult compareResult : compareResults) {

            // 叶子节点
            Map<List<NodeEntity>, NoiseNodeAddress> mayIgnoreNodesOfSingle = new HashMap<>();
            // 数组节点
            Map<List<NodeEntity>, NoiseNodeAddress> mayDisorderArrayOfSingle = new HashMap<>();

            List<LogEntity> logs = compareResult.getLogs();
            if (CollectionUtils.isEmpty(logs)) {
                continue;
            }

            int size = logs.size();
            for (int i = 0; i < size; i++) {
                LogEntity log = logs.get(i);
                MutablePair<List<NodeEntity>, Boolean> judgeResult = logFilterAndArrayJudge(log);
                if (judgeResult == null) {
                    continue;
                }

                List<NodeEntity> upperArrayNodePath = judgeResult.getLeft();
                boolean ifFindArray = judgeResult.getRight();
                if (!ifFindArray) {
                    this.setSingleAnalysisContent(mayIgnoreNodesOfSingle, upperArrayNodePath, compareResult, log, i);
                } else {
                    this.setSingleAnalysisContent(mayDisorderArrayOfSingle, upperArrayNodePath, compareResult, log, i);
                }
            }

            this.aggSingleAnalysis(mayIgnoreNodes, mayIgnoreNodesOfSingle);
            this.aggSingleAnalysis(mayDisorderArray, mayDisorderArrayOfSingle);
        }
        inferredReplayNoiseDto.setMayIgnoreNodes(mayIgnoreNodes);
        inferredReplayNoiseDto.setMayDisorderArray(mayDisorderArray);
        return inferredReplayNoiseDto;
    }

    public static MutablePair<List<NodeEntity>, Boolean> logFilterAndArrayJudge(LogEntity log) {

        List<NodeEntity> errorPath = null;
        UnmatchedPairEntity pathPair = log.getPathPair();
        if (pathPair != null && pathPair.getUnmatchedType() == UnmatchedType.UNMATCHED) {
            List<NodeEntity> leftUnmatchedPath = pathPair.getLeftUnmatchedPath();
            if (CollectionUtils.isNotEmpty(leftUnmatchedPath)) {
                errorPath = leftUnmatchedPath;
            }
        }
        if (errorPath == null) {
            return null;
        }

        int arrayIndex = 0;
        int size = errorPath.size();

        for (int i = 0; i < size; i++) {
            NodeEntity node = errorPath.get(i);
            if (node.getNodeName() == null) {
                // 跳过双重数组
                if (i != size - 1 && errorPath.get(i + 1).getNodeName() == null) {
                    arrayIndex = -1;
                    break;
                }
                arrayIndex = i;
            }
        }

        switch (arrayIndex) {
            case -1:
                return null;
            case 0:
                return new MutablePair<>(errorPath, false);
            default:
                List<NodeEntity> collect = errorPath.subList(0, arrayIndex).stream()
                    .filter(item -> item.getNodeName() != null).collect(Collectors.toList());
                return new MutablePair<>(collect, true);
        }
    }

    private void setSingleAnalysisContent(Map<List<NodeEntity>, NoiseNodeAddress> singleAggContent,
        List<NodeEntity> upperArrayNodePath, ReplayCompareResult replayCompareResult, LogEntity logEntity,
        int logIndex) {
        NoiseNodeAddress noiseNodeAddress = singleAggContent.get(upperArrayNodePath);
        String fuzzyPathStr = ListUtils.getFuzzyPathStr(logEntity.getPathPair().getLeftUnmatchedPath());
        if (noiseNodeAddress == null) {
            noiseNodeAddress = new NoiseNodeAddress();

            noiseNodeAddress.setCompareResult(replayCompareResult);
            noiseNodeAddress.setLogIndexes(Stream.of(logIndex).collect(Collectors.toList()));
            noiseNodeAddress.setSubPaths(MapUtils.createMap(fuzzyPathStr, 1));
            noiseNodeAddress.setCaseCount(1);

            singleAggContent.put(upperArrayNodePath, noiseNodeAddress);
        } else {
            noiseNodeAddress.getLogIndexes().add(logIndex);
            Map<String, Integer> subPaths = noiseNodeAddress.getSubPaths();
            Integer subPathErrorCount = subPaths.computeIfAbsent(fuzzyPathStr, k -> 0);
            subPaths.put(fuzzyPathStr, subPathErrorCount + 1);
        }
    }

    private void aggSingleAnalysis(Map<List<NodeEntity>, NoiseNodeAddress> aggContent,
        Map<List<NodeEntity>, NoiseNodeAddress> ageContentOfSingle) {
        for (Map.Entry<List<NodeEntity>, NoiseNodeAddress> aggContentOfSingleItem : ageContentOfSingle.entrySet()) {
            List<NodeEntity> nodePath = aggContentOfSingleItem.getKey();
            NoiseNodeAddress valueInSingle = aggContentOfSingleItem.getValue();

            NoiseNodeAddress valueInAgg = aggContent.get(nodePath);

            if (valueInAgg == null) {
                aggContent.put(nodePath, aggContentOfSingleItem.getValue());
            } else {
                Map<String, Integer> subPathsInSingle = valueInSingle.getSubPaths();
                Map<String, Integer> subPathsInAgg = valueInAgg.getSubPaths();
                subPathsInSingle.forEach((k, v) -> {
                    subPathsInAgg.merge(k, v, Integer::sum);
                });
                valueInAgg.setCaseCount(valueInAgg.getCaseCount() + valueInSingle.getCaseCount());
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PlanItemIdAndContextName {
        private String planItemId;
        private String contextName;
        private List<ReplayActionCaseItem> cases;
    }

    @Data
    private static class InferredReplayNoiseDto {
        Map<List<NodeEntity>, NoiseNodeAddress> mayIgnoreNodes;
        Map<List<NodeEntity>, NoiseNodeAddress> mayDisorderArray;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class NoiseNodeAddress {

        private List<Integer> logIndexes;
        private ReplayCompareResult compareResult;

        // 从属于该path的错误, 以及错误数
        private Map<String, Integer> subPaths;

        // 错误的mocker数
        private int caseCount;

        public int getPathCount() {
            return subPaths.size();
        }
    }

    public static void main(String[] args) {
        LogEntity logEntity = new LogEntity();
        UnmatchedPairEntity unmatchedPairEntity = new UnmatchedPairEntity();

        unmatchedPairEntity.setUnmatchedType(UnmatchedType.UNMATCHED);
        unmatchedPairEntity.setLeftUnmatchedPath(Arrays.asList(new NodeEntity("a", 0), new NodeEntity(null, 0),
            new NodeEntity("b", 0), new NodeEntity(null, 2), new NodeEntity("c", 0)));
        unmatchedPairEntity.setRightUnmatchedPath(Arrays.asList(new NodeEntity("a", 0), new NodeEntity(null, 0),
            new NodeEntity("b", 0), new NodeEntity(null, 2), new NodeEntity("c", 0)));
        logEntity.setPathPair(unmatchedPairEntity);
        MutablePair<List<NodeEntity>, Boolean> listBooleanMutablePair = logFilterAndArrayJudge(logEntity);
        System.out.println();
    }

}
