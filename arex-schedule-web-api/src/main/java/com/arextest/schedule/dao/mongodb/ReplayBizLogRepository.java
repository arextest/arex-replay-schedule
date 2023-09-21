package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.model.bizlog.ReplayBizLogQueryCondition;
import com.arextest.schedule.model.converter.ReplayBizLogConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayBizLogCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by qzmo on 2023/5/31.
 */
@Repository
public class ReplayBizLogRepository implements RepositoryField {

    @Autowired
    MongoTemplate mongoTemplate;

    private static final String PLAN_ID_KEY = "planId";
    private static final String LOG_LEVEL_KEY = "level";
    private static final String LOG_TYPE_KEY = "logType";
    private static final String LOG_ACTION_ID_KEY = "actionItemId";
    private static final String LOG_RESUME_KEY = "resumedExecution";
    private static final String[] EXCLUSIONS = {"dataChangeCreateTime",
            "dataChangeUpdateTime",
            "dataChangeCreateDate",
            "_id"};

    public void saveAll(Collection<BizLog> logs) {
        List<ReplayBizLogCollection> logDocs = logs
                .stream()
                .map(ReplayBizLogConverter.INSTANCE::daoFromDto)
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(logDocs)) {
            this.mongoTemplate.insertAll(logDocs);
        }
    }

    public List<ReplayBizLogCollection> queryByPlanId(String planId) {
        return this.mongoTemplate.find(commonQuery(planId, null), ReplayBizLogCollection.class);
    }

    public List<ReplayBizLogCollection> queryByPlanId(String planId, ReplayBizLogQueryCondition condition) {
        Query query = commonQuery(planId, condition);
        query.skip((long) (condition.getPageNum() - 1) * condition.getPageSize());
        query.limit(condition.getPageSize());

        return this.mongoTemplate.find(query, ReplayBizLogCollection.class);
    }

    public long countByPlanId(String planId, ReplayBizLogQueryCondition condition) {
        Query query = commonQuery(planId, condition);

        return this.mongoTemplate.count(query, ReplayBizLogCollection.class);
    }


    private Query commonQuery(String planId, ReplayBizLogQueryCondition condition) {
        Query query = new Query().addCriteria(Criteria.where(PLAN_ID_KEY).is(planId));
        query.fields().exclude(EXCLUSIONS);
        query.with(Sort.by(DASH_ID));

        if (condition == null) {
            return query;
        }

        if (!CollectionUtils.isEmpty(condition.getLevels())) {
            query.addCriteria(Criteria.where(LOG_LEVEL_KEY).in(condition.getLevels()));
        }

        if (!CollectionUtils.isEmpty(condition.getTypes())) {
            query.addCriteria(Criteria.where(LOG_TYPE_KEY).in(condition.getTypes()));
        }

        if (!CollectionUtils.isEmpty(condition.getActionItems())) {
            query.addCriteria(Criteria.where(LOG_ACTION_ID_KEY).in(condition.getActionItems()));
        }

        if (condition.getResumedExecution() != null) {
            query.addCriteria(Criteria.where(LOG_RESUME_KEY).is(condition.getResumedExecution()));
        }
        return query;
    }

}