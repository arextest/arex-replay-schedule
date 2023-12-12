package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.RepositoryWriter;
import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.CaseSendStatusType;
import com.arextest.schedule.model.CompareProcessStatusType;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.converter.ReplayRunDetailsConverter;
import com.arextest.schedule.model.dao.mongodb.ModelBase;
import com.arextest.schedule.model.dao.mongodb.ReplayRunDetailsCollection;
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
import org.bson.types.ObjectId;
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
  private static final String COUNT_FIELD = "count";
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

  public List<ReplayActionCaseItem> waitingSendList(String planId, int pageSize,
      List<Criteria> baseCriteria, String minId) {
    Query query = new Query();

    Optional.ofNullable(baseCriteria).ifPresent(criteria -> criteria.forEach(query::addCriteria));

    query.addCriteria(Criteria.where(ReplayActionCaseItem.FIELD_PLAN_ID).is(planId));
    query.addCriteria(new Criteria().orOperator(
        Criteria.where(ReplayActionCaseItem.FIELD_SEND_STATUS)
            .is(CaseSendStatusType.WAIT_HANDLING.getValue()),
        Criteria.where(ReplayActionCaseItem.FIELD_COMPARE_STATUS)
            .is(CompareProcessStatusType.WAIT_HANDLING.getValue())
    ));
    if (StringUtils.hasText(minId)) {
      query.addCriteria(Criteria.where(DASH_ID).gt(new ObjectId(minId)));
    }
    query.limit(pageSize);
    query.with(Sort.by(Sort.Order.asc(DASH_ID)));
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
    query.addCriteria(Criteria.where(ReplayActionCaseItem.FIELD_PLAN_ID).is(planId));
    if (StringUtils.hasText(planItemId)) {
      query.addCriteria(Criteria.where(ReplayActionCaseItem.FIELD_PLAN_ITEM_ID).is(planItemId));
    }
    query.addCriteria(new Criteria().orOperator(
        Criteria.where(ReplayActionCaseItem.FIELD_SEND_STATUS)
            .ne(CaseSendStatusType.SUCCESS.getValue()),
        Criteria.where(ReplayActionCaseItem.FIELD_COMPARE_STATUS)
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
        Aggregation.match(Criteria.where(ReplayActionCaseItem.FIELD_PLAN_ID).is(planId)),
        Aggregation.match(Criteria.where(ReplayActionCaseItem.FIELD_SEND_STATUS)
            .is(CaseSendStatusType.WAIT_HANDLING.getValue())),
        Aggregation.group(ReplayActionCaseItem.FIELD_PLAN_ITEM_ID).count().as(COUNT_FIELD)
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

  public ReplayActionCaseItem lastOne(String planItemId) {
    Query query = Query.query(Criteria.where(PLAN_ITEM_ID).is(planItemId));

    query.addCriteria(
        new Criteria().orOperator(
            Criteria.where(SEND_STATUS).is(CaseSendStatusType.WAIT_HANDLING.getValue()),
            new Criteria().andOperator(
                Criteria.where(SEND_STATUS).is(CaseSendStatusType.SUCCESS.getValue()),
                Criteria.where(COMPARE_STATUS).is(CompareProcessStatusType.WAIT_HANDLING.getValue())
            )
        )
    );
    query.limit(1);
    query.with(Sort.by(
        Sort.Order.desc(DASH_ID)
    ));
    ReplayRunDetailsCollection replayRunDetailsCollections = mongoTemplate.findOne(query,
        ReplayRunDetailsCollection.class);
    return converter.dtoFromDao(replayRunDetailsCollections);
  }

  // region <context>
  public Set<String> getAllContextIdentifiers(String planId) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.FIELD_PLAN_ID).is(planId));
    return new HashSet<>(
        mongoTemplate.findDistinct(query, ReplayActionCaseItem.FIELD_CONTEXT_IDENTIFIER,
            ReplayRunDetailsCollection.class, String.class));
  }

  public boolean hasNullIdentifier(String planId) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.FIELD_PLAN_ID).is(planId));
    query.addCriteria(Criteria.where(ReplayActionCaseItem.FIELD_CONTEXT_IDENTIFIER).isNull());
    return mongoTemplate.exists(query, ReplayRunDetailsCollection.class);
  }

  // get one mocker of the given context
  public ReplayActionCaseItem getOneOfContext(String planId, String contextIdentifier) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.FIELD_PLAN_ID).is(planId));
    query.addCriteria(
        Criteria.where(ReplayActionCaseItem.FIELD_CONTEXT_IDENTIFIER).is(contextIdentifier));
    ReplayRunDetailsCollection replayRunDetailsCollection = mongoTemplate.findOne(query,
        ReplayRunDetailsCollection.class);
    return converter.dtoFromDao(replayRunDetailsCollection);
  }

  public ReplayActionCaseItem queryById(String caseId) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.FIELD_ID).is(caseId));
    ReplayRunDetailsCollection replayRunDetailsCollection = mongoTemplate.findOne(query,
        ReplayRunDetailsCollection.class);
    return converter.dtoFromDao(replayRunDetailsCollection);
  }

  public List<ReplayActionCaseItem> batchQueryById(List<String> caseIdList) {
    Query query = Query.query(Criteria.where(ReplayActionCaseItem.FIELD_ID).in(caseIdList));
    List<ReplayRunDetailsCollection> replayRunDetailsCollections = mongoTemplate.find(query,
        ReplayRunDetailsCollection.class);
    return replayRunDetailsCollections.stream()
        .map(replayRunDetailsCollection -> converter.dtoFromDao(replayRunDetailsCollection))
        .collect(Collectors.toList());
  }


  @Data
  private static class GroupCountRes {

    @Id
    private String planItemId;
    private Long count;
  }

  // endregion <context>
}