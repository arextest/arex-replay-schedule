package com.arextest.schedule.service.report;

import com.arextest.common.context.ArexContext;
import com.arextest.common.utils.JsonTraverseUtils;
import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.enumeration.DiffResultCode;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.diff.model.log.NodeEntity;
import com.arextest.diff.model.log.UnmatchedPairEntity;
import com.arextest.diff.sdk.CompareSDK;
import com.arextest.schedule.comparer.CompareConfigService;
import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.comparer.impl.DefaultReplayResultComparer;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.config.ComparisonGlobalConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.arextest.schedule.model.converter.ReplayCompareResultConverter;
import com.arextest.schedule.model.report.QueryDiffMsgByIdResponseType;
import com.arextest.schedule.model.report.QueryLogEntityRequestTye;
import com.arextest.schedule.model.report.QueryLogEntityResponseType;
import com.arextest.schedule.utils.ListUtils;
import com.arextest.schedule.model.report.CompareResultDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
public class QueryReplayMsgService {
    @Resource
    private ReplayCompareResultRepositoryImpl replayCompareResultRepository;
    @Resource
    private CompareConfigService compareConfigService;
    @Resource
    private CustomComparisonConfigurationHandler configHandler;
    @Resource
    private ReplayCompareResultConverter replayCompareResultConverter;
    private static final CompareSDK COMPARE_INSTANCE = DefaultReplayResultComparer.getCompareSDKInstance();


    public QueryDiffMsgByIdResponseType queryDiffMsgById(String id) {
        QueryDiffMsgByIdResponseType response = new QueryDiffMsgByIdResponseType();

        ReplayCompareResult compareResultBo = replayCompareResultRepository.queryCompareResultsById(id);

        ComparisonInterfaceConfig operationConfig = compareConfigService.loadInterfaceConfig(compareResultBo.getPlanItemId());
        ComparisonGlobalConfig globalConfig = compareConfigService.loadGlobalConfig(compareResultBo.getPlanId());


        ReplayComparisonConfig itemConfig = configHandler.pickConfig(globalConfig, operationConfig,
                compareResultBo.getCategoryName(), compareResultBo.getOperationName());

        CompareOptions compareOptions = configHandler.buildSkdOption(compareResultBo.getCategoryName(), itemConfig);

        // bo may contain only quick compare result, need to fill log entities into BO
        if (DiffResultCode.COMPARED_WITH_DIFFERENCE == compareResultBo.getDiffResultCode()
                && CollectionUtils.isEmpty(compareResultBo.getLogs())) {
            List<LogEntity> logs = COMPARE_INSTANCE.compare(compareResultBo.getBaseMsg(),
                    compareResultBo.getTestMsg(), compareOptions).getLogs();

            compareResultBo.setLogs(logs);
            replayCompareResultRepository.save(compareResultBo);
        }

        CompareResultDetail compareResultDetail = replayCompareResultConverter.voFromBo(compareResultBo);

        fillCompareResultDetail(compareResultBo, compareResultDetail);

        if (!Boolean.TRUE.equals(ArexContext.getContext().getPassAuth())) {
            try {
                compareResultDetail.setBaseMsg(JsonTraverseUtils.trimAllLeaves(compareResultBo.getBaseMsg()));
                compareResultDetail.setTestMsg(JsonTraverseUtils.trimAllLeaves(compareResultBo.getTestMsg()));
            } catch (Exception e) {
                LOGGER.error("trimAllLeaves error", e);
            }
        }

        response.setCompareResultDetail(compareResultDetail);
        return response;
    }

    private void fillCompareResultDetail(ReplayCompareResult compareResultBo, CompareResultDetail compareResultDetail) {
        List<LogEntity> logEntities = compareResultBo.getLogs();
        if (CollectionUtils.isEmpty(logEntities)) {
            return;
        }

        if (compareResultBo.getDiffResultCode() == DiffResultCode.COMPARED_INTERNAL_EXCEPTION) {
            LogEntity logEntity = logEntities.get(0);
            CompareResultDetail.LogInfo logInfo = new CompareResultDetail.LogInfo();
            logInfo.setUnmatchedType(logEntity.getPathPair().getUnmatchedType());
            logInfo.setNodePath(Collections.emptyList());
            compareResultDetail.setLogInfos(Collections.singletonList(logInfo));
            compareResultDetail.setExceptionMsg(logEntity.getLogInfo());
        } else {
            HashMap<MutablePair<String, Integer>, CompareResultDetail.LogInfo> logInfoMap = new HashMap<>();
            int size = logEntities.size();
            for (int i = 0; i < size; i++) {
                LogEntity logEntity = logEntities.get(i);
                UnmatchedPairEntity pathPair = logEntity.getPathPair();
                int unmatchedType = pathPair.getUnmatchedType();
                List<NodeEntity> leftUnmatchedPath = pathPair.getLeftUnmatchedPath();
                List<NodeEntity> rightUnmatchedPath = pathPair.getRightUnmatchedPath();
                int leftUnmatchedPathSize = leftUnmatchedPath == null ? 0 : leftUnmatchedPath.size();
                int rightUnmatchedPathSize = rightUnmatchedPath == null ? 0 : rightUnmatchedPath.size();
                List<NodeEntity> nodePath =
                        leftUnmatchedPathSize >= rightUnmatchedPathSize ? leftUnmatchedPath : rightUnmatchedPath;
                MutablePair<String, Integer> tempPair =
                        new MutablePair<>(ListUtils.getFuzzyPathStr(nodePath), unmatchedType);
                CompareResultDetail.LogInfo logInfo;
                if (!logInfoMap.containsKey(tempPair)) {
                    logInfo = new CompareResultDetail.LogInfo();
                    logInfo.setUnmatchedType(unmatchedType);
                    logInfo.setNodePath(nodePath);
                    logInfo.setLogIndex(i);
                    logInfoMap.put(tempPair, logInfo);
                } else {
                    logInfo = logInfoMap.get(tempPair);
                }
                logInfo.setCount(logInfo.getCount() + 1);
            }
            compareResultDetail.setLogInfos(new ArrayList<>(logInfoMap.values()));
        }
    }

    public QueryLogEntityResponseType queryLogEntity(QueryLogEntityRequestTye request) {
        QueryLogEntityResponseType response = new QueryLogEntityResponseType();
        ReplayCompareResult dto = replayCompareResultRepository.queryCompareResultsById(request.getCompareResultId());
        List<LogEntity> logs = dto.getLogs();
        response.setLogEntity(logs.get(request.getLogIndex()));
        response.setDiffResultCode(dto.getDiffResultCode());
        return response;
    }
}