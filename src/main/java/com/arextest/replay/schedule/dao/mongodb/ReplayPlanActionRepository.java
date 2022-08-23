package com.arextest.replay.schedule.dao.mongodb;

import com.arextest.replay.schedule.dao.RepositoryWriter;
import com.arextest.replay.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.converter.ReplayPlanItemConverter;
import com.arextest.replay.schedule.model.dao.mongodb.ReplayPlanItemCollection;
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
        update.set("replayStatus", actionItem.getReplayStatus());
        update.set("replayBeginTime", actionItem.getReplayBeginTime());
        update.set("replayFinishTime", actionItem.getReplayFinishTime());
        update.set("replayCaseCount", actionItem.getReplayCaseCount());
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ReplayPlanItemCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    public List<ReplayActionItem> queryPlanActionList(String planId) {
        Query query = Query.query(Criteria.where("planId").is(planId));
        List<ReplayPlanItemCollection> replayPlanItemCollections = mongoTemplate.find(query, ReplayPlanItemCollection.class);
        return replayPlanItemCollections.stream().map(ReplayPlanItemConverter.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

    public long queryRunningItemCount(String appId) {
        Query query = Query.query(Criteria.where("appId").is(appId));
        query.addCriteria(Criteria.where("replayStatus").in(Arrays.asList(0, 1)));
        return mongoTemplate.count(query, ReplayPlanItemCollection.class);
    }


}
