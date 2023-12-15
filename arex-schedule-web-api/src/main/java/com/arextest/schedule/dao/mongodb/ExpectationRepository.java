package com.arextest.schedule.dao.mongodb;

import com.arextest.schedule.model.dao.mongodb.ExpectationResultCollection;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ExpectationRepository {
    @Resource
    private MongoTemplate mongoTemplate;

    public void save(ExpectationResultCollection collection) {
        mongoTemplate.insert(collection);
    }

    public List<ExpectationResultCollection> query(String caseId) {
        Query query = Query.query(Criteria.where(ExpectationResultCollection.FIELD_CASE_ID).is(caseId));
        return mongoTemplate.find(query, ExpectationResultCollection.class);
    }
}
