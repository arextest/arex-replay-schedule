package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.converter.ReplayPlanConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayPlanCollection;
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

    private static final String CASE_TOTAL_COUNT = "caseTotalCount";
    private static final String PLAN_FINISH_TIME = "planFinishTime";
    private static final String PLAN_CREATE_TIME = "planCreateTime";
    private static final String REPLAY_PLAN_STAGE_LIST = "replayPlanStageList";


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
        update.set(CASE_TOTAL_COUNT, caseTotal);
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ReplayPlanCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    public boolean finish(String planId) {
        Query query = Query.query(Criteria.where(DASH_ID).is(planId));
        Update update = MongoHelper.getUpdate();
        update.set(PLAN_FINISH_TIME, new Date());
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ReplayPlanCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    public List<ReplayPlan> timeoutPlanList(Duration offsetDuration, Duration maxDuration) {
        Query query = Query.query(Criteria.where(PLAN_FINISH_TIME).is(null));
        long now = System.currentTimeMillis();
        long from = now - offsetDuration.toMillis();
        long to = from - maxDuration.toMillis();
        query.addCriteria(Criteria.where(PLAN_CREATE_TIME).gte(new Date(to)).lte(new Date(from)));
        List<ReplayPlanCollection> replayPlanCollections = mongoTemplate.find(query, ReplayPlanCollection.class);
        return replayPlanCollections.stream().map(ReplayPlanConverter.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

    public void updateStage(ReplayPlan replayPlan) {
        Query query = Query.query(Criteria.where(DASH_ID).is(replayPlan.getId()));
        Update update = MongoHelper.getUpdate();
        update.set(REPLAY_PLAN_STAGE_LIST, replayPlan.getReplayPlanStageList());
        mongoTemplate.findAndModify(query, update, ReplayPlanCollection.class);
    }

    public ReplayPlan query(String planId) {
        Query query = Query.query(Criteria.where(DASH_ID).is(planId));
        ReplayPlanCollection replayPlanCollection = mongoTemplate.findOne(query, ReplayPlanCollection.class);
        return ReplayPlanConverter.INSTANCE.dtoFromDao(replayPlanCollection);
    }
}