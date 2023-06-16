package com.arextest.schedule.dao.mongodb;

import com.arextest.web.model.dao.mongodb.ReportPlanStatisticCollection;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * created by xinyuan_wang on 2023/6/16
 */
@Repository
public class ReportPlanStatisticRepository implements RepositoryField {

    @Autowired
    MongoTemplate mongoTemplate;

    private static final String STATUS = "status";
    private static final String APP_ID = "appId";
    private static final String TARGET_ENV = "targetEnv";
    private static final String SOURCE_ENV = "sourceEnv";

    public long countReportPlanByEnv(String appId, String targetEnv, String sourceEnv, int status) {
        Query query = Query.query(Criteria.where(APP_ID).is(appId).and(STATUS).is(status));

        if (StringUtils.isNotEmpty(sourceEnv)) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where(SOURCE_ENV).in(targetEnv, sourceEnv), Criteria.where(TARGET_ENV).in(targetEnv, sourceEnv)));
        } else {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where(TARGET_ENV).is(targetEnv), Criteria.where(SOURCE_ENV).is(targetEnv)));
        }

        return mongoTemplate.count(query, ReportPlanStatisticCollection.class);
    }
}