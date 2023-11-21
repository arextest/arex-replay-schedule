package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.converter.ReplayNoiseConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayNoiseCollection;
import com.arextest.schedule.model.dao.mongodb.ReplayNoiseCollection.ReplayNoiseItemDao;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseDto;
import com.arextest.schedule.model.noiseidentify.UpdateNoiseItem;
import com.arextest.schedule.utils.MapUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class ReplayNoiseRepository implements RepositoryField {

  @Resource
  private MongoTemplate mongoTemplate;

  public boolean saveList(List<ReplayNoiseDto> replayNoiseDtoList) {
    if (CollectionUtils.isEmpty(replayNoiseDtoList)) {
      return true;
    }

    List<ReplayNoiseCollection> collect =
        replayNoiseDtoList.stream().map(ReplayNoiseConverter.INSTANCE::daoFromDto)
            .collect(Collectors.toList());

    try {
      BulkOperations bulkOperations =
          mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ReplayNoiseCollection.class);
      for (ReplayNoiseCollection replayNoiseCollection : collect) {
        Query query = Query
            .query(Criteria.where(ReplayNoiseCollection.FIELD_PLAN_ID)
                .is(replayNoiseCollection.getPlanId())
                .and(ReplayNoiseCollection.FIELD_PLAN_ITEM_ID)
                .is(replayNoiseCollection.getPlanItemId())
                .and(ReplayNoiseCollection.FIELD_CATEGORY_NAME)
                .is(replayNoiseCollection.getCategoryName())
                .and(ReplayNoiseCollection.FIELD_OPERATION_NAME)
                .is(replayNoiseCollection.getOperationName()));

        Update update = MongoHelper.getUpdate();
        update.setOnInsert(ReplayNoiseCollection.FIELD_DATA_CHANGE_CREATE_DATE, new Date());
        update.setOnInsert(ReplayNoiseCollection.FIELD_PLAN_ID, replayNoiseCollection.getPlanId());
        update.setOnInsert(ReplayNoiseCollection.FIELD_PLAN_ITEM_ID,
            replayNoiseCollection.getPlanItemId());
        update.setOnInsert(ReplayNoiseCollection.FIELD_CATEGORY_NAME,
            replayNoiseCollection.getCategoryName());
        update.setOnInsert(ReplayNoiseCollection.FIELD_OPERATION_ID,
            replayNoiseCollection.getOperationId());
        update.setOnInsert(ReplayNoiseCollection.FIELD_OPERATION_NAME,
            replayNoiseCollection.getOperationName());

        Map<String, ReplayNoiseCollection.ReplayNoiseItemDao> mayIgnoreItems =
            replayNoiseCollection.getMayIgnoreItems();
        Map<String, ReplayNoiseCollection.ReplayNoiseItemDao> mayDisorderItems =
            replayNoiseCollection.getMayDisorderItems();
        this.appendUpdate(update, mayIgnoreItems, ReplayNoiseCollection.FIELD_MAY_IGNORE_ITEMS);
        this.appendUpdate(update, mayDisorderItems, ReplayNoiseCollection.FIELD_MAY_DISORDER_ITEMS);
        bulkOperations.upsert(query, update);
      }
      bulkOperations.execute();
    } catch (RuntimeException e) {
      LOGGER.error("ReplayNoiseRepository.saveList error", e);
      return false;
    }
    return true;
  }

  public boolean removeReplayNoise(List<String> planItemIds) {
    if (CollectionUtils.isEmpty(planItemIds)) {
      return true;
    }
    Query query = Query.query(
        Criteria.where(ReplayNoiseCollection.FIELD_PLAN_ITEM_ID).in(planItemIds));
    mongoTemplate.remove(query, ReplayNoiseCollection.class);
    return true;
  }

  public List<ReplayNoiseDto> queryReplayNoise(@NotBlank String planId, String planItemId) {
    Query query = Query.query(Criteria.where(ReplayNoiseCollection.FIELD_PLAN_ID).is(planId));
    if (StringUtils.isNotEmpty(planItemId)) {
      query.addCriteria(Criteria.where(ReplayNoiseCollection.FIELD_PLAN_ITEM_ID).is(planItemId));
    }
    List<ReplayNoiseCollection> replayNoiseCollections = mongoTemplate.find(query,
        ReplayNoiseCollection.class);
    return replayNoiseCollections.stream().map(ReplayNoiseConverter.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  public boolean updateReplayNoiseStatus(List<UpdateNoiseItem> updateNoiseItems) {

    try {
      BulkOperations bulkOperations =
          mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ReplayNoiseCollection.class);
      for (UpdateNoiseItem updateNoiseItem : updateNoiseItems) {
        Map<String, Object> queryFields = updateNoiseItem.getQueryFields();
        Map<String, Object> updateFields = updateNoiseItem.getUpdateFields();
        if (MapUtils.isEmpty(queryFields) || MapUtils.isEmpty(updateFields)) {
          continue;
        }

        List<Criteria> criteriaList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : queryFields.entrySet()) {
          if (entry.getKey() == null) {
            continue;
          }
          criteriaList.add(Criteria.where(entry.getKey()).is(entry.getValue()));
        }
        if (CollectionUtils.isEmpty(criteriaList)) {
          continue;
        }
        Query query = new Query().addCriteria(new Criteria().andOperator(criteriaList));

        Update update = MongoHelper.getUpdate();
        for (Map.Entry<String, Object> entry : updateFields.entrySet()) {
          update.set(entry.getKey(), entry.getValue());
        }
        bulkOperations.updateMulti(query, update);
      }
      bulkOperations.execute();
    } catch (RuntimeException e) {
      LOGGER.error("ReplayNoiseRepository.updateReplayNoiseStatus error", e);
      return false;
    }
    return true;
  }

  private void appendUpdate(Update update,
      Map<String, ReplayNoiseCollection.ReplayNoiseItemDao> needUpdateContent,
      String updateKey) {
    if (MapUtils.isNotEmpty(needUpdateContent)) {
      for (Map.Entry<String, ReplayNoiseCollection.ReplayNoiseItemDao> entry : needUpdateContent.entrySet()) {
        String path = entry.getKey();
        ReplayNoiseCollection.ReplayNoiseItemDao replayNoiseItemDao = entry.getValue();
        update.set(
            MongoHelper.appendDot(updateKey, path,
                ReplayNoiseCollection.ReplayNoiseItemDao.FIELD_NODE_PATH),
            replayNoiseItemDao.getNodePath());
        update.set(
            MongoHelper.appendDot(updateKey, path,
                ReplayNoiseCollection.ReplayNoiseItemDao.FIELD_COMPARE_RESULT_ID),
            replayNoiseItemDao.getCompareResultId());
        update.set(
            MongoHelper.appendDot(updateKey, path,
                ReplayNoiseCollection.ReplayNoiseItemDao.FIELD_LOG_INDEXES),
            replayNoiseItemDao.getLogIndexes());
        if (replayNoiseItemDao.getStatus() != null) {
          update.set(
              MongoHelper.appendDot(updateKey, path, ReplayNoiseItemDao.FIELD_STATUS),
              replayNoiseItemDao.getStatus());
        }
        update.inc(
            MongoHelper.appendDot(updateKey, path,
                ReplayNoiseCollection.ReplayNoiseItemDao.FIELD_PATH_COUNT),
            replayNoiseItemDao.getPathCount());
        update.inc(
            MongoHelper.appendDot(updateKey, path,
                ReplayNoiseCollection.ReplayNoiseItemDao.FIELD_CASE_COUNT),
            replayNoiseItemDao.getCaseCount());
        Optional.ofNullable(replayNoiseItemDao.getSubPaths()).ifPresent(map -> {
          for (Map.Entry<String, Integer> subEntry : map.entrySet()) {
            String subPath = subEntry.getKey();
            Integer subCount = subEntry.getValue();
            update.inc(MongoHelper.appendDot(updateKey, path,
                ReplayNoiseCollection.ReplayNoiseItemDao.FIELD_SUB_PATHS, subPath), subCount);
          }
        });
      }
    }
  }

}
