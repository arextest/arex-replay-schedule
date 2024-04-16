package com.arextest.schedule.service.noise;

import com.arextest.common.runnable.AbstractContextWithTraceRunnable;
import com.arextest.diff.model.CompareResult;
import com.arextest.diff.model.enumeration.DiffResultCode;
import com.arextest.diff.model.enumeration.UnmatchedType;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.diff.model.log.NodeEntity;
import com.arextest.diff.model.log.UnmatchedPairEntity;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.schedule.common.CommonConstant;
import com.arextest.schedule.comparer.CategoryComparisonHolder;
import com.arextest.schedule.comparer.ReplayResultComparer;
import com.arextest.schedule.comparer.impl.PrepareCompareSourceRemoteLoader;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.dao.mongodb.ReplayNoiseRepository;
import com.arextest.schedule.dao.mongodb.ReplayPlanActionRepository;
import com.arextest.schedule.mdc.MDCTracer;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.noiseidentify.ActionItemForNoiseIdentify;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseDto;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseItemDto;
import com.arextest.schedule.utils.ListUtils;
import com.arextest.schedule.utils.MapUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 * Created by coryhh on 2023/10/17.
 */
@Data
@Slf4j
public class AsyncNoiseCaseAnalysisTaskRunnable extends AbstractContextWithTraceRunnable {

  private ActionItemForNoiseIdentify actionItemForNoiseIdentify;

  private PrepareCompareSourceRemoteLoader sourceRemoteLoader;

  private ReplayResultComparer replayResultComparer;

  private ReplayCompareResultRepositoryImpl replayCompareResultRepository;

  private ReplayNoiseRepository replayNoiseRepository;

  private ReplayPlanActionRepository replayPlanActionRepository;

  @Override
  protected void doWithContextRunning() {
    String planId = actionItemForNoiseIdentify.getPlanId();
    String planItemId = actionItemForNoiseIdentify.getPlanItemId();
    String contextName = actionItemForNoiseIdentify.getContextName();
    MDCTracer.addNoiseActionId(planItemId);
    LOGGER.info("start to analysis noise case, planItemId: {}, contextName: {}", planItemId,
        contextName);
    try {
      List<ReplayActionCaseItem> cases = actionItemForNoiseIdentify.getCases();

      // do compare
      List<ReplayCompareResult> compareResults = new ArrayList<>();
      for (ReplayActionCaseItem caseItem : cases) {
        // do not analysis the case which has no sourceId and targetId
        if (caseItem.getSourceResultId() == null && caseItem.getTargetResultId() == null) {
          CompareResult compareResult =
              CompareSDK.fromException(caseItem.requestMessage(), null,
                  caseItem.getSendErrorMessage());
          ReplayCompareResult exceptionReplayCompareResult =
              ReplayCompareResult.createFrom(caseItem, compareResult);
          compareResults.add(exceptionReplayCompareResult);
          continue;
        }

        List<CategoryComparisonHolder> categoryComparisonHolders =
            sourceRemoteLoader.buildWaitCompareList(caseItem, true);
        List<ReplayCompareResult> replayCompareResults =
            replayResultComparer.doContentCompare(caseItem, categoryComparisonHolders);
        Optional.ofNullable(replayCompareResults).orElse(Collections.emptyList()).forEach(item -> {
          // correct the problem of recordId value in dual environment
          item.setRecordId(caseItem.getSourceResultId());
          compareResults.add(item);
        });
      }

      List<ReplayNoiseDto> result = new ArrayList<>();

      // do analysis
      if (CollectionUtils.isEmpty(compareResults)) {
        return;
      }

      Map<CompareResultGroupIdentify, List<ReplayCompareResult>> compareResultOfCategory = compareResults.stream()
          .filter(compareResult -> compareResult.getDiffResultCode()
              == DiffResultCode.COMPARED_WITH_DIFFERENCE)
          .collect(
              Collectors.groupingBy(item -> new CompareResultGroupIdentify(item.getCategoryName(),
                  item.getOperationId(), item.getOperationName())));
      for (Map.Entry<CompareResultGroupIdentify, List<ReplayCompareResult>> entry : compareResultOfCategory
          .entrySet()) {

        CompareResultGroupIdentify categoryAndOperation = entry.getKey();
        List<ReplayCompareResult> replayCompareResultList = entry.getValue();

        InferredReplayNoiseDto inferredReplayNoiseDto =
            this.analysisNoiseFromCompareResult(replayCompareResultList);
        if (inferredReplayNoiseDto == null) {
          continue;
        }

        ReplayNoiseDto replayNoiseDto = new ReplayNoiseDto();
        replayNoiseDto.setPlanId(planId);
        replayNoiseDto.setPlanItemId(planItemId);
        replayNoiseDto.setCategoryName(categoryAndOperation.getCategoryName());
        replayNoiseDto.setOperationId(categoryAndOperation.getOperationId());
        replayNoiseDto.setOperationName(categoryAndOperation.getOperationName());

        Map<List<NodeEntity>, ReplayNoiseItemDto> mayIgnoreNodes = inferredReplayNoiseDto.getMayIgnoreNodes();
        if (MapUtils.isNotEmpty(mayIgnoreNodes)) {
          Map<String, ReplayNoiseItemDto> mayIgnoreItems = new HashMap<>();
          for (Map.Entry<List<NodeEntity>, ReplayNoiseItemDto> nodeEntry : mayIgnoreNodes.entrySet()) {
            List<NodeEntity> nodePath = nodeEntry.getKey();
            ReplayNoiseItemDto replayNoiseItemDto = nodeEntry.getValue();
            replayNoiseItemDto.setNodePath(nodePath);
            mayIgnoreItems.put(ListUtils.getFuzzyPathStrWithBase64(nodePath), replayNoiseItemDto);
          }
          replayNoiseDto.setMayIgnoreItems(mayIgnoreItems);
        }

        Map<List<NodeEntity>, ReplayNoiseItemDto> mayDisorderArray =
            inferredReplayNoiseDto.getMayDisorderArray();
        if (MapUtils.isNotEmpty(mayDisorderArray)) {
          Map<String, ReplayNoiseItemDto> mayDisorderItems = new HashMap<>();
          for (Map.Entry<List<NodeEntity>, ReplayNoiseItemDto> nodeEntry : mayDisorderArray.entrySet()) {
            List<NodeEntity> nodePath = nodeEntry.getKey();
            ReplayNoiseItemDto replayNoiseItemDto = nodeEntry.getValue();
            replayNoiseItemDto.setNodePath(nodePath);
            mayDisorderItems.put(ListUtils.getFuzzyPathStrWithBase64(nodePath), replayNoiseItemDto);
          }
          replayNoiseDto.setMayDisorderItems(mayDisorderItems);
        }
        result.add(replayNoiseDto);
      }

      // dropped into the database
      this.toNoiseCompareResult(compareResults);
      replayCompareResultRepository.save(compareResults);
      replayNoiseRepository.saveList(result);
      replayPlanActionRepository.updateNoiseOfContextFinished(planItemId, contextName,
          cases.size());
    } catch (RuntimeException exception) {
      LOGGER.error("analysis noise case failed, planItemId: {}, contextName: {}", planItemId,
          contextName,
          exception);
    }
    LOGGER.info("finish to analysis noise case, planItemId: {}, contextName: {}", planItemId,
        contextName);
    MDCTracer.removeNoiseActionId();
  }

  /**
   * Analysis results of a single mocker type
   *
   * @param compareResults
   * @return
   */
  private InferredReplayNoiseDto analysisNoiseFromCompareResult(
      List<ReplayCompareResult> compareResults) {
    if (CollectionUtils.isEmpty(compareResults)) {
      return null;
    }
    // leaf node
    Map<List<NodeEntity>, ReplayNoiseItemDto> mayIgnoreNodes = new HashMap<>();
    // array node
    Map<List<NodeEntity>, ReplayNoiseItemDto> mayDisorderArray = new HashMap<>();

    for (ReplayCompareResult compareResult : compareResults) {

      // leaf node path -> ReplayNoiseItemDto
      Map<List<NodeEntity>, ReplayNoiseItemDto> mayIgnoreNodesOfSingle = new HashMap<>();
      // array node path -> ReplayNoiseItemDto
      Map<List<NodeEntity>, ReplayNoiseItemDto> mayDisorderArrayOfSingle = new HashMap<>();

      List<LogEntity> logs = compareResult.getLogs();
      if (CollectionUtils.isEmpty(logs)) {
        continue;
      }

      int size = logs.size();
      for (int i = 0; i < size; i++) {
        LogEntity log = logs.get(i);
        MutablePair<List<NodeEntity>, Boolean> judgeResult = this.logFilterAndArrayJudge(log);
        if (judgeResult == null) {
          continue;
        }

        List<NodeEntity> upperArrayNodePath = judgeResult.getLeft();
        boolean ifFindArray = judgeResult.getRight();
        if (!ifFindArray) {
          this.getAnalysisOfSingleCompareResult(mayIgnoreNodesOfSingle, upperArrayNodePath,
              compareResult,
              log, i);
        } else {
          this.getAnalysisOfSingleCompareResult(mayDisorderArrayOfSingle, upperArrayNodePath,
              compareResult,
              log, i);
        }
      }

      this.aggSingleAnalysis(mayIgnoreNodes, mayIgnoreNodesOfSingle);
      this.aggSingleAnalysis(mayDisorderArray, mayDisorderArrayOfSingle);
    }

    if (MapUtils.isEmpty(mayIgnoreNodes) && MapUtils.isEmpty(mayDisorderArray)) {
      return null;
    }
    return new InferredReplayNoiseDto(mayIgnoreNodes, mayDisorderArray);
  }

  private MutablePair<List<NodeEntity>, Boolean> logFilterAndArrayJudge(LogEntity log) {

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
        // skip double array
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

  private void getAnalysisOfSingleCompareResult(
      Map<List<NodeEntity>, ReplayNoiseItemDto> singleAggContent,
      List<NodeEntity> upperArrayNodePath, ReplayCompareResult replayCompareResult,
      LogEntity logEntity,
      int logIndex) {
    ReplayNoiseItemDto replayNoiseItemDto = singleAggContent.get(upperArrayNodePath);
    String fuzzyPathStr = ListUtils.getFuzzyPathStrWithBase64(
        logEntity.getPathPair().getLeftUnmatchedPath());
    if (replayNoiseItemDto == null) {
      replayNoiseItemDto = new ReplayNoiseItemDto();
      replayNoiseItemDto.setCompareResult(replayCompareResult);
      replayNoiseItemDto.setLogIndexes(Stream.of(logIndex).collect(Collectors.toList()));
      replayNoiseItemDto.setSubPaths(MapUtils.createMap(fuzzyPathStr, 1));
      replayNoiseItemDto.setCaseCount(1);

      singleAggContent.put(upperArrayNodePath, replayNoiseItemDto);
    } else {
      replayNoiseItemDto.getLogIndexes().add(logIndex);
      Map<String, Integer> subPaths = replayNoiseItemDto.getSubPaths();
      Integer subPathErrorCount = subPaths.computeIfAbsent(fuzzyPathStr, k -> 0);
      subPaths.put(fuzzyPathStr, subPathErrorCount + 1);
    }
  }

  private void aggSingleAnalysis(Map<List<NodeEntity>, ReplayNoiseItemDto> aggContent,
      Map<List<NodeEntity>, ReplayNoiseItemDto> ageContentOfSingle) {
    for (Map.Entry<List<NodeEntity>, ReplayNoiseItemDto> aggContentOfSingleItem : ageContentOfSingle.entrySet()) {
      List<NodeEntity> nodePath = aggContentOfSingleItem.getKey();
      ReplayNoiseItemDto valueInSingle = aggContentOfSingleItem.getValue();

      ReplayNoiseItemDto valueInAgg = aggContent.get(nodePath);

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

  private void toNoiseCompareResult(List<ReplayCompareResult> compareResults) {
    for (ReplayCompareResult replayCompareResult : compareResults) {
      replayCompareResult.setPlanId(replayCompareResult.getPlanId() + CommonConstant.NOISE_HANDLER);
      replayCompareResult.setPlanItemId(
          replayCompareResult.getPlanItemId() + CommonConstant.NOISE_HANDLER);
      replayCompareResult.setRecordId(
          replayCompareResult.getRecordId() + CommonConstant.NOISE_HANDLER);
      replayCompareResult.setReplayId(
          replayCompareResult.getReplayId() + CommonConstant.NOISE_HANDLER);
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class InferredReplayNoiseDto {

    Map<List<NodeEntity>, ReplayNoiseItemDto> mayIgnoreNodes;
    Map<List<NodeEntity>, ReplayNoiseItemDto> mayDisorderArray;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(of = {"categoryName", "operationName"})
  private static class CompareResultGroupIdentify {

    private String categoryName;
    private String operationId;
    private String operationName;
  }

}
