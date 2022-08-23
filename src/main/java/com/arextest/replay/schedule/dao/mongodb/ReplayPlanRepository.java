package com.arextest.replay.schedule.dao.mongodb;

import com.arextest.replay.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.replay.schedule.model.ReplayPlan;
import com.arextest.replay.schedule.model.converter.ReplayPlanConverter;
import com.arextest.replay.schedule.model.dao.mongodb.ReplayPlanCollection;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Repository
public class ReplayPlanRepository implements RepositoryField {

    @Autowired
    MongoTemplate mongoTemplate;


    public boolean save(ReplayPlan replayPlan) {
        ReplayPlanCollection replayPlanCollection = ReplayPlanConverter.INSTANCE.daoFromDto(replayPlan);
        ReplayPlanCollection insert = mongoTemplate.insert(replayPlanCollection);
        if (insert.getId() != null) {
            replayPlan.setId(insert.getId());
        }
        return insert.getId() != null;
    }

    public boolean updateCaseTotal(String planId, int caseTotal) {
        Query query = Query.query(Criteria.where(DASH_ID).is(planId));
        Update update = MongoHelper.getUpdate();
        update.set("caseTotalCount", caseTotal);
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ReplayPlanCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    public boolean finish(String planId) {
        Query query = Query.query(Criteria.where(DASH_ID).is(planId));
        Update update = MongoHelper.getUpdate();
        update.set("planFinishTime", new Date());
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ReplayPlanCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    public List<ReplayPlan> timeoutPlanList(Duration offsetDuration, Duration maxDuration) {
        Query query = Query.query(Criteria.where("planFinishTime").is(null));
        query.addCriteria(Criteria.where("planCreateTime").gte(offsetDuration).lte(maxDuration));
        List<ReplayPlanCollection> replayPlanCollections = mongoTemplate.find(query, ReplayPlanCollection.class);
        return replayPlanCollections.stream().map(ReplayPlanConverter.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

}
