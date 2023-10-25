package com.arextest.schedule.service;

import com.arextest.schedule.client.HttpWepServiceApiClient;
import com.arextest.schedule.dao.mongodb.ReplayCompareResultRepositoryImpl;
import com.arextest.schedule.model.storage.CompareResultDbAggStruct;
import com.arextest.schedule.model.storage.PostProcessResultRequestType;
import com.arextest.schedule.model.storage.ResultCodeGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReplayStorageService {

  @Resource
  private ReplayCompareResultRepositoryImpl replayCompareResultRepository;
  @Resource
  private HttpWepServiceApiClient client;
  @Value("${arex.storage.postProcess.url}")
  private String postProcessUrl;

  public void postProcessCompareResult(String planId, int planResultCode) {
    try {
      List<CompareResultDbAggStruct> dbAgg = replayCompareResultRepository.calculateResultCodeGroup(
          planId);
      PostProcessResultRequestType storageReq = new PostProcessResultRequestType();
      storageReq.setReplayStatusCode(planResultCode);
      storageReq.setReplayPlanId(planId);
      List<ResultCodeGroup> resGroups = new ArrayList<>();
      storageReq.setResults(resGroups);

      // convert db agg results to request type
      Map<Integer, List<CompareResultDbAggStruct>> groupByCodes = dbAgg.stream()
          .collect(Collectors.groupingBy(CompareResultDbAggStruct::getDiffResultCode));
      for (Map.Entry<Integer, List<CompareResultDbAggStruct>> entry : groupByCodes.entrySet()) {
        ResultCodeGroup resGroup = new ResultCodeGroup();
        resGroup.setResultCode(entry.getKey());
        List<ResultCodeGroup.CategoryGroup> categoryGroups = new ArrayList<>();
        Map<String, CompareResultDbAggStruct> groupByCategory = entry.getValue().stream()
            .collect(Collectors.toMap(CompareResultDbAggStruct::getCategoryName, i -> i));

        for (Map.Entry<String, CompareResultDbAggStruct> categoryEntry : groupByCategory.entrySet()) {
          ResultCodeGroup.CategoryGroup categoryGroup = new ResultCodeGroup.CategoryGroup();
          categoryGroup.setCategoryName(categoryEntry.getKey());
          categoryGroup.setResultIds(categoryEntry.getValue().getRelatedIds());
          categoryGroups.add(categoryGroup);
        }
        resGroup.setCategoryGroups(categoryGroups);
        resGroups.add(resGroup);
      }

      String out = client.jsonPost(postProcessUrl, storageReq, String.class);
      LOGGER.info("postProcessCompareResult result: {}", out);
    } catch (Exception e) {
      LOGGER.error("postProcessCompareResult error", e);
    }
  }
}
