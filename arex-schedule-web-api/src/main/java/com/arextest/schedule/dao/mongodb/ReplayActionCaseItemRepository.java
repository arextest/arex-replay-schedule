package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.RepositoryWriter;
import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionCaseItem.Fields;
import com.arextest.schedule.model.converter.ReplayRunDetailsConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayRunDetailsCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Repository
@Slf4j
public class ReplayActionCaseItemRepository implements RepositoryWriter<ReplayActionCaseItem>,
    RepositoryField {

  private static final String PLAN_ITEM_ID = "planItemId";
  private static final String SEND_STATUS = "sendStatus";
  private static final String SOURCE_RESULT_ID = "sourceResultId";
  private static final String TARGET_RESULT_ID = "targetResultId";
  private static final String COMPARE_STATUS = "compareStatus";
  private static final String CASE_TYPE_FIELD = "caseType";
  private static final String COUNT_FIELD = "count";
  private static final String RECORD_TIME = "recordTime";
  private static final String LAST_RECORD_TIME_FIELD = "lastRecordTime";
  private static final String ID_FIELD = "_id";

  @Autowired
  MongoTemplate mongoTemplate;
  @Resource
  private ReplayRunDetailsConverter converter;

  @Override
  public boolean save(ReplayActionCaseItem replayActionCaseItem) {
    ReplayRunDetailsCollection replayRunDetailsCollection = converter.daoFromDto(
        replayActionCaseItem);
    ReplayRunDetailsCollection insert = mongoTemplate.insert(replayRunDetailsCollection);
    if (insert.getId() != null) {
      replayActionCaseItem.setId(insert.getId());
    }
    return insert.getId() != null;
  }

  @Override
  public boolean save(List<ReplayActionCaseItem> caseItems) {
    List<ReplayRunDetailsCollection> replayPlanItemCollections = caseItems.stream()
        .map(converter::daoFromDto).collect(Collectors.toList());

    List<ReplayRunDetailsCollection> inserted = new ArrayList<>(mongoTemplate
        .insert(replayPlanItemCollections, ReplayRunDetailsCollection.class));

    if (CollectionUtils.isEmpty(inserted) || inserted.size() != caseItems.size()) {
      LOGGER.error("Error saving case items, save size does not match source.");
      return false;
    }

    for (int i = 0; i < inserted.size(); i++) {
      caseItems.get(i).setId(inserted.get(i).getId());
    }
    return true;
  }

  /**
   * @return case list that need to be sent Case order is hard-coded to be ascending by record time.
   *
   * The case order is to control the order of extra mockers yields during the replay phase, please
   * make sure you discuss with agent team before changing the order.
   * todo make order configurable
   */
  public List<ReplayActionCaseItem> waitingSendList(String planId, int pageSize,
      List<Criteria> baseCriteria, Long minRecordTime) {
    Query query = new Query();

    Optional.ofNullable(baseCriteria).ifPresent(criteria -> criteria.forEach(query::addCriteria));

    query.addCriteria(Criteria.where(ReplayActionCaseItem.Fields.PLAN_ID).is(planId));
    query.addCriteria(new Criteria().orOperator(
        Criteria.where(ReplayActionCaseItem.Fields.SEND_STATUS)
            .is(CaseSendStatusType.WAIT_HANDLING.getValue()),
        Criteria.where(ReplayActionCaseItem.Fields.COMPARE_STATUS)
            .is(CompareProcessStatusType.WAIT_HANDLING.getValue())
    ));
    if (minRecordTime != null) {
      query.addCriteria(
          Criteria.where(ReplayRunDetailsCollection.Fields.RECORD_TIME).gte(minRecordTime));
    }
    query.limit(pageSize);
    query.with(Sort.by(Sort.Order.asc(ReplayRunDetailsCollection.Fields.RECORD_TIME)));

    List<ReplayRunDetailsCollection> replayRunDetailsCollections = mongoTemplate.find(query,
        ReplayRunDetailsCollection.class);
    return replayRunDetailsCollections.stream().map(converter::dtoFromDao)
        .collect(Collectors.toList());
  }

  /**
   * Get the case list that failed to send or compare.
   */
  public List<ReplayActionCaseItem> failedCaseList(String planId, String planItemId) {
    Query query = new Query();
    query.addCriteria(Criteria.where(ReplayActionCaseItem.Fields.PLAN_ID).is(planId));
    if (StringUtils.hasText(planItemId)) {
      query.addCriteria(Criteria.where(ReplayActionCaseItem.Fields.PLAN_ITEM_ID).is(planItemId));
    }
    query.addCriteria(new Criteria().orOperator(
        Criteria.where(ReplayActionCaseItem.Fields.SEND_STATUS)
            .ne(CaseSendStatusType.SUCCESS.getValue()),
        Criteria.where(ReplayActionCaseItem.Fields.COMPARE_STATUS)
            .ne(CompareProcessStatusType.PASS.getValue())
    ));

    List<ReplayRunDetailsCollection> replayRunDetailsCollections = mongoTemplate.find(query,
        ReplayRunDetailsCollection.class);
    return replayRunDetailsCollections.stream().map(converter::dtoFromDao)
        .collect(Collectors.toList());
  }

  public Map<String, Long> countWaitHandlingByAction(String planId, List<Criteria> baseCriteria) {
    // combine baseCriteria into one
    Criteria criteria = new Criteria();
    Optional.ofNullable(baseCriteria)
        .ifPresent(criterias -> criterias.forEach(criteria::andOperator));

    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(criteria),
        Aggregation.match(Criteria.where(ReplayActionCaseItem.Fields.PLAN_ID).is(planId)),
        Aggregation.match(Criteria.where(ReplayActionCaseItem.Fields.SEND_STATUS)
            .is(CaseSendStatusType.WAIT_HANDLING.getValue())),
        Aggregation.group(ReplayActionCaseItem.Fields.PLAN_ITEM_ID).count().as(COUNT_FIELD)
    );
    List<GroupCountRes> aggRes = mongoTemplate.aggregate(aggregation,
        ReplayRunDetailsCollection.class, GroupCountRes.class).getMappedResults();
    Map<String, Long> res = new HashMap<>();
    for (GroupCountRes aggResult : aggRes) {
      res.put(aggResult.getPlanItemId(), aggResult.getCount());
    }
    return res;
  }

  public boolean updateSendResult(ReplayActionCaseItem replayActionCaseItem) {
    Query query = Query.query(Criteria.where(DASH_ID).is(replayActionCaseItem.getId()));
    Update update = MongoHelper.getUpdate();
    MongoHelper.assertNull("update parameter is null", replayActionCaseItem.getSourceResultId(),
        replayActionCaseItem.getTargetResultId());
    update.set(SEND_STATUS, replayActionCaseItem.getSendStatus());
    update.set(SOURCE_RESULT_ID, replayActionCaseItem.getSourceResultId());
    update.set(TARGET_RESULT_ID, replayActionCaseItem.getTargetResultId());
    UpdateResult updateResult = mongoTemplate.updateMulti(query, update,
        ReplayRunDetailsCollection.class);
    return updateResult.getModifiedCount() > 0;
  }

  public boolean updateCompareStatus(String id, int comparedStatus) {
    Query query = Query.query(Criteria.where(DASH_ID).is(id));
    Update update = MongoHelper.getUpdate();
    update.set(COMPARE_STATUS, comparedStatus);
    UpdateResult updateResult = mongoTemplate.updateMulti(query, update,
        ReplayRunDetailsCollection.class);
    return updateResult.getModifiedCount() > 0;
  }

  public void batchUpdateStatus(List<ReplayActionCaseItem> replayActionCaseItemList) {
    if (CollectionUtils.isEmpty(replayActionCaseItemList)) {
      return;
    }
    BulkOperations bulkOperations =
        mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ReplayRunDetailsCollection.class);
    List<Pair<Query, Update>> updates = new ArrayList<>();
    for (ReplayActionCaseItem replayActionCaseItem : replayActionCaseItemList) {
      Update update = MongoHelper.getUpdate();
      update.set(SEND_STATUS, replayActionCaseItem.getSendStatus());
      update.set(COMPARE_STATUS, replayActionCaseItem.getCompareStatus());
      Query query = Query.query(Criteria.where(DASH_ID).is(replayActionCaseItem.getId()));
      updates.add(Pair.of(query, update));
    }
    bulkOperations.updateMulti(updates).execute();
  }

  /**
   * Get the number of recorded cases and the earliest time
   * @param planItemId
   * @return
   */
  public List<GroupCountRes> getLastRecord(String planItemId) {
    Criteria criteria = Criteria.where(PLAN_ITEM_ID).is(planItemId);
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.match(criteria),
        Aggregation.group(CASE_TYPE_FIELD)
            .min(RECORD_TIME).as(LAST_RECORD_TIME_FIELD)
            .count().as(COUNT_FIELD),
        Aggregation.project(LAST_RECORD_TIME_FIELD, COUNT_FIELD, CASE_TYPE_FIELD)
            .and(ID_FIELD).as(CASE_TYPE_FIELD)
            .andExclude(ID_FIELD)
    );

    return mongoTemplate.aggregate(aggregation,
        ReplayRunDetailsCollection.class, GroupCountRes.class).getMappedResults();
  }

  // region <context>
  public Set<String> getAllContextIdentifiers(String planId) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.Fields.PLAN_ID).is(planId));
    return new HashSet<>(
        mongoTemplate.findDistinct(query, ReplayActionCaseItem.Fields.CONTEXT_IDENTIFIER,
            ReplayRunDetailsCollection.class, String.class));
  }

  public boolean hasNullIdentifier(String planId) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.Fields.PLAN_ID).is(planId));
    query.addCriteria(Criteria.where(ReplayActionCaseItem.Fields.CONTEXT_IDENTIFIER).isNull());
    return mongoTemplate.exists(query, ReplayRunDetailsCollection.class);
  }

  // get one mocker of the given context
  public ReplayActionCaseItem getOneOfContext(String planId, String contextIdentifier) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.Fields.PLAN_ID).is(planId));
    query.addCriteria(
        Criteria.where(ReplayActionCaseItem.Fields.CONTEXT_IDENTIFIER).is(contextIdentifier));
    ReplayRunDetailsCollection replayRunDetailsCollection = mongoTemplate.findOne(query,
        ReplayRunDetailsCollection.class);
    return converter.dtoFromDao(replayRunDetailsCollection);
  }

  public ReplayActionCaseItem queryById(String caseId) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.Fields.ID).is(caseId));
    ReplayRunDetailsCollection replayRunDetailsCollection = mongoTemplate.findOne(query,
        ReplayRunDetailsCollection.class);
    return converter.dtoFromDao(replayRunDetailsCollection);
  }

  public List<ReplayActionCaseItem> batchQueryById(List<String> caseIdList) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.Fields.ID).in(caseIdList));
    List<ReplayRunDetailsCollection> replayRunDetailsCollections = mongoTemplate.find(query,
        ReplayRunDetailsCollection.class);
    return replayRunDetailsCollections.stream()
        .map(replayRunDetailsCollection -> converter.dtoFromDao(replayRunDetailsCollection))
        .collect(Collectors.toList());
  }

  public boolean deleteExcludedCases(String planId, List<String> planItemIds) {
    if (CollectionUtils.isEmpty(planItemIds)) {
      return false;
    }
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.Fields.PLAN_ID).is(planId));
    query.addCriteria(Criteria.where(Fields.PLAN_ITEM_ID).in(planItemIds));
    DeleteResult deleteResult = mongoTemplate.remove(query, ReplayRunDetailsCollection.class);
    return deleteResult.getDeletedCount() > 0;
  }

  @Data
  public static class GroupCountRes {
    @Id
    private String planItemId;
    private Long count;
    private Long lastRecordTime;
    private String caseType;
  }

  // endregion <context>
}