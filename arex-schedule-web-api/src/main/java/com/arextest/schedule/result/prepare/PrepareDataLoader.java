package com.arextest.schedule.result.prepare;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.schedule.model.replay.ReplayCaseResult;
import com.arextest.schedule.model.replay.ReplayCaseItem;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.serialization.ZstdJacksonSerializer;
import com.arextest.schedule.service.external.storage.ReplayStorageService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class PrepareDataLoader {
    private static final int INDEX_NOT_FOUND = -1;
    @Resource
    private ReplayStorageService replayStorageService;
    @Resource
    private ZstdJacksonSerializer zstdJacksonSerializer;

    public List<ReplayCaseResult> load(ReplayActionCaseItem caseItem) {
        if (!caseItem.isMultiEnvCompare()) {
            return getReplayResult(caseItem.getRecordId(), caseItem.getTargetResultId());
        }
        List<ReplayCaseResult> sourceReplayResultList = getReplayResult(caseItem.getRecordId(),
            caseItem.getSourceResultId());
        List<ReplayCaseResult> targetReplayResultList = getReplayResult(caseItem.getRecordId(),
            caseItem.getTargetResultId());
        if (CollectionUtils.isEmpty(sourceReplayResultList) || CollectionUtils.isEmpty(targetReplayResultList)) {
            LOGGER.warn(
                "replay recordId:{} invalid response, source replayId:{} size:{}, target replayId:{} size:{}",
                caseItem.getRecordId(), caseItem.getSourceResultId(), sourceReplayResultList.size(),
                caseItem.getTargetResultId(),
                targetReplayResultList.size());
            return Collections.emptyList();
        }
        multiEnvExchangeData(sourceReplayResultList, targetReplayResultList);
        return sourceReplayResultList;
    }

    // TODO: In the scenario where the operation is empty, there is a problem of redundant returns in the record.
    public List<ReplayCaseResult> getReplayResult(String replayId, String resultId) {
        List<ListResultHolder> resultHolderList = replayStorageService.queryRelayResult(replayId, resultId);
        return convertToReplayResultList(resultHolderList);
    }

    private List<ReplayCaseResult> convertToReplayResultList(List<ListResultHolder> resultHolderList) {
        List<ReplayCaseResult> decodedListResult = new ArrayList<>(resultHolderList.size());
        for (ListResultHolder resultHolder : resultHolderList) {
            if (resultHolder.getCategoryType() == null || resultHolder.getCategoryType().isSkipComparison()) {
                continue;
            }

            ReplayCaseResult replayResult = new ReplayCaseResult();
            replayResult.setCategoryName(resultHolder.getCategoryType().getName());
            decodedListResult.add(replayResult);

            List<ReplayCaseItem> recordCaseList = deserializeCase(resultHolder.getRecord());
            List<ReplayCaseItem> replayCaseList = deserializeCase(resultHolder.getReplayResult());
            if (resultHolder.getCategoryType().isEntryPoint()) {
                /*
                 * call missing means record case is not null, replay case is null
                 * new call means record case is null, replay case is not null
                 */
                if (CollectionUtils.isEmpty(recordCaseList) || CollectionUtils.isEmpty(replayCaseList)) {
                    return Collections.emptyList();
                }
            }
            replayResult.setRecordCaseList(recordCaseList);
            replayResult.setReplayCaseList(replayCaseList);
        }
        return decodedListResult;
    }

    private List<ReplayCaseItem> deserializeCase(List<String> caseList) {
        if (CollectionUtils.isEmpty(caseList)) {
            return Collections.emptyList();
        }
        List<ReplayCaseItem> caseItemList = new ArrayList<>(caseList.size());
        for (String base64 : caseList) {
            AREXMocker source = zstdJacksonSerializer.deserialize(base64, AREXMocker.class);
            if (source == null) {
                continue;
            }
            caseItemList.add(toReplayCaseItem(source));
        }
        return caseItemList;
    }

    private ReplayCaseItem toReplayCaseItem(AREXMocker mocker) {
        ReplayCaseItem item = new ReplayCaseItem();
        item.setId(mocker.getId());
        item.setEntryPoint(mocker.getCategoryType().isEntryPoint());
        item.setOperationName(mocker.getOperationName());
        item.setCreateTime(mocker.getCreationTime());
        if (mocker.getTargetRequest() != null) {
            item.setRequest(mocker.getTargetRequest().getBody());
        }
        if (mocker.getTargetResponse() != null) {
            item.setResponse(mocker.getTargetResponse().getBody());
        }

        return item;
    }

    private void multiEnvExchangeData(List<ReplayCaseResult> sourceResultList,
        List<ReplayCaseResult> targetResultList) {
        for (ReplayCaseResult sourceResult : sourceResultList) {
            // exchange source replay case list with source record case list
            sourceResult.setRecordCaseList(sourceResult.getReplayCaseList());
            int targetIndex = findResultByCategory(targetResultList, sourceResult.getCategoryName());
            if (targetIndex == INDEX_NOT_FOUND) {
                continue;
            }

            ReplayCaseResult targetResult = targetResultList.get(targetIndex);
            // exchange source replay case list with target replay case list
            sourceResult.setReplayCaseList(targetResult.getReplayCaseList());
            targetResultList.remove(targetIndex);
        }
        if (CollectionUtils.isNotEmpty(targetResultList)) {
            for (ReplayCaseResult targetResult : targetResultList) {
                targetResult.setRecordCaseList(Collections.emptyList());
                sourceResultList.add(targetResult);
            }
        }
    }

    private int findResultByCategory(List<ReplayCaseResult> replayResultList, String category) {
        for (int i = 0; i < replayResultList.size(); i++) {
            ReplayCaseResult replayResult = replayResultList.get(i);
            if (StringUtils.equals(replayResult.getCategoryName(), category)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }
}

