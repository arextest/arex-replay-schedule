package com.arextest.schedule.dao.mongodb;

import com.arextest.config.model.dao.config.AppCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * @author wildeslam.
 * @create 2023/10/11 14:06
 */
@Slf4j
@Repository
public class ApplicationRepository implements RepositoryField {
    @Autowired
    MongoTemplate mongoTemplate;

    public AppCollection query(String appId) {
        Query query = Query.query(Criteria.where(AppCollection.Fields.appId).is(appId));
        return mongoTemplate.findOne(query, AppCollection.class, AppCollection.DOCUMENT_NAME);
    }
}
