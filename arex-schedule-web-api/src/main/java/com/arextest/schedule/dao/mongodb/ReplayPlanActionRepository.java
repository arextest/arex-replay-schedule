package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.RepositoryWriter;
import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.converter.ReplayPlanItemConverter;
import com.arextest.schedule.model.dao.mongodb.ModelBase.Fields;
import com.arextest.schedule.model.dao.mongodb.ReplayPlanItemCollection;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Repository
@Slf4j
public class ReplayPlanActionRepository implements RepositoryWriter<ReplayActionItem>,
    RepositoryField {

  private static final String REPLAY_STATUS = "replayStatus";
  private static final String REPLAY_BEGIN_TIME = "replayBeginTime";
  private static final String REPLAY_FINISH_TIME = "replayFinishTime";
  private static final String REPLAY_CASE_COUNT = "replayCaseCount";
  private static final String PLAN_ID = "planId";
  @Autowired
  MongoTemplate mongoTemplate;

  @Override
  public boolean save(ReplayActionItem actionItem) {
    ReplayPlanItemCollection replayPlanItemCollection = ReplayPlanItemConverter.INSTANCE.daoFromDto(
        actionItem);
    ReplayPlanItemCollection insert = mongoTemplate.insert(replayPlanItemCollection);
    if (insert.getId() != null) {
      actionItem.setId(insert.getId());
    }
    return insert.getId() != null;
  }

  @Override
  public boolean save(List<ReplayActionItem> actionItems) {
    List<ReplayPlanItemCollection> replayPlanItemCollections =
        actionItems.stream().map(ReplayPlanItemConverter.INSTANCE::daoFromDto)
            .collect(Collectors.toList());

    List<ReplayPlanItemCollection> inserted =
        new ArrayList<>(
            mongoTemplate.insert(replayPlanItemCollections, ReplayPlanItemCollection.class));

    if (CollectionUtils.isEmpty(inserted) || inserted.size() != actionItems.size()) {
      LOGGER.error("Error saving action items, save size does not match source.");
      return false;
    }

    for (int i = 0; i < inserted.size(); i++) {
      actionItems.get(i).setId(inserted.get(i).getId());
    }
    return true;
  }

  public boolean update(ReplayActionItem actionItem) {
    Query query = Query.query(Criteria.where(DASH_ID).is(actionItem.getId()));
    Update update = MongoHelper.getUpdate();
    update.set(REPLAY_STATUS, actionItem.getReplayStatus());
    update.set(REPLAY_BEGIN_TIME, actionItem.getReplayBeginTime());
    update.set(REPLAY_FINISH_TIME, actionItem.getReplayFinishTime());
    update.set(REPLAY_CASE_COUNT, actionItem.getReplayCaseCount());
    UpdateResult updateResult = mongoTemplate.updateMulti(query, update,
        ReplayPlanItemCollection.class);
    return updateResult.getModifiedCount() > 0;
  }

  public List<ReplayActionItem> queryPlanActionList(String planId) {
    Query query = Query.query(Criteria.where(PLAN_ID).is(planId));
    List<ReplayPlanItemCollection> replayPlanItemCollections =
        mongoTemplate.find(query, ReplayPlanItemCollection.class);
    return replayPlanItemCollections.stream().map(ReplayPlanItemConverter.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  public boolean updateNoiseOfContextFinished(String id, String contextName, int count) {
    Query query = Query.query(Criteria.where(DASH_ID).is(id));
    Update update = new Update()
        .inc(MongoHelper.appendDot(ReplayPlanItemCollection.Fields.NOISE_FINISHED_CONTEXTS,
            contextName), count);
    UpdateResult updateResult = mongoTemplate.updateMulti(query, update,
        ReplayPlanItemCollection.class);
    return updateResult.getModifiedCount() > 0;
  }

  public boolean bulkUpdateNoiseFinishedContexts(List<ReplayActionItem> actionItems) {
    if (CollectionUtils.isEmpty(actionItems)) {
      return true;
    }

    try {
      BulkOperations bulkOperations =
          mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ReplayPlanItemCollection.class);
      for (ReplayActionItem actionItem : actionItems) {
        Query query = Query.query(
            Criteria.where(Fields.ID).is(actionItem.getId()));
        Update update = MongoHelper.getUpdate();
        update.set(ReplayPlanItemCollection.Fields.NOISE_FINISHED_CONTEXTS,
            actionItem.getNoiseFinishedContexts());
        bulkOperations.updateMulti(query, update);
      }
      bulkOperations.execute();
    } catch (RuntimeException exception) {
      LOGGER.error("ReplayPlanActionRepository.bulkUpdateNoiseFinishedContexts error", exception);
      return false;
    }
    return true;
  }

}