package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.RepositoryWriter;
import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.converter.ReplayPlanItemConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayPlanItemCollection;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Repository
public class ReplayPlanActionRepository implements RepositoryWriter<ReplayActionItem>, RepositoryField {

    @Autowired
    MongoTemplate mongoTemplate;

    private static final String REPLAY_STATUS = "replayStatus";
    private static final String REPLAY_BEGIN_TIME = "replayBeginTime";
    private static final String REPLAY_FINISH_TIME = "replayFinishTime";
    private static final String REPLAY_CASE_COUNT = "replayCaseCount";
    private static final String PLAN_ID = "planId";
    private static final String APP_ID ="appId";

    @Override
    public boolean save(ReplayActionItem actionItem) {
        ReplayPlanItemCollection replayPlanItemCollection = ReplayPlanItemConverter.INSTANCE.daoFromDto(actionItem);
        ReplayPlanItemCollection insert = mongoTemplate.insert(replayPlanItemCollection);
        if (insert.getId() != null) {
            actionItem.setId(insert.getId());
        }
        return insert.getId() != null;
    }

    public boolean update(ReplayActionItem actionItem) {
        Query query = Query.query(Criteria.where(DASH_ID).is(actionItem.getId()));
        Update update = MongoHelper.getUpdate();
        update.set(REPLAY_STATUS, actionItem.getReplayStatus());
        update.set(REPLAY_BEGIN_TIME, actionItem.getReplayBeginTime());
        update.set(REPLAY_FINISH_TIME, actionItem.getReplayFinishTime());
        update.set(REPLAY_CASE_COUNT, actionItem.getReplayCaseCount());
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ReplayPlanItemCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    public List<ReplayActionItem> queryPlanActionList(String planId) {
        Query query = Query.query(Criteria.where(PLAN_ID).is(planId));
        List<ReplayPlanItemCollection> replayPlanItemCollections = mongoTemplate.find(query, ReplayPlanItemCollection.class);
        return replayPlanItemCollections.stream().map(ReplayPlanItemConverter.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

    public long queryRunningItemCount(String appId) {
        Query query = Query.query(Criteria.where(APP_ID).is(appId));
        query.addCriteria(Criteria.where(REPLAY_STATUS).in(Arrays.asList(0, 1)));
        return mongoTemplate.count(query, ReplayPlanItemCollection.class);
    }


}