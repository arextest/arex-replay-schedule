package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.dao.mongodb.util.MongoHelper;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.model.converter.ReplayBizLogConverter;
import com.arextest.schedule.model.converter.ReplayPlanConverter;
import com.arextest.schedule.model.dao.mongodb.ReplayBizLogCollection;
import com.arextest.schedule.model.dao.mongodb.ReplayPlanCollection;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
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
        Query query = new Query().addCriteria(Criteria.where(PLAN_ID_KEY).is(planId));
        query.fields().exclude(EXCLUSIONS);
        return this.mongoTemplate
                .find(query, ReplayBizLogCollection.class);
    }
}